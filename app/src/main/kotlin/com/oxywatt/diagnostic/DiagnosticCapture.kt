package com.oxywatt.diagnostic

import android.os.SystemClock
import com.oxywatt.core.collection.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.time.Instant
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

internal data class DiagnosticMetric(
    val value: Double? = null,
    val reason: MissingReason? = null,
    val callbacks: Long = 0,
    val ageNanos: Long? = null,
    val stamp: ReceiptStamp? = null,
    val intervalNanos: Long? = null,
    val minimumIntervalNanos: Long? = null,
    val maximumIntervalNanos: Long? = null,
)

internal enum class DiagnosticReportStatus {
    WAITING,
    SAVED,
    LIMIT_REACHED,
    STORAGE_ERROR,
    INCOMPLETE,
}

internal data class DiagnosticSnapshot(
    val metrics: Map<Metric, DiagnosticMetric> = emptyMap(),
    val reportStatus: DiagnosticReportStatus = DiagnosticReportStatus.WAITING,
    val savedBytes: Long = 0,
    val droppedEvents: Long = 0,
    val pendingEvents: Int = 0,
    val connected: Boolean? = null,
    val recording: RecordingState = RecordingState.UNKNOWN,
    val sourceActive: Boolean = false,
)

/**
 * Constant-size received state; private report contains timing/status only, never metric values.
 */
internal class DiagnosticCapture(
    private val file: File,
    private val nowNanos: () -> Long = SystemClock::elapsedRealtimeNanos,
    private val maxBytes: Long = 8L * 1024 * 1024,
    private val maxEvents: Int = 128,
) {
    private val lock = Any()
    private val writerLock = Any()
    private val processRun = UUID.randomUUID()
    private val readings = mutableMapOf<Metric, DiagnosticMetric>()
    private val events = ArrayDeque<JSONObject>()
    private var dirty = true
    private var dropped = 0L
    private var inFlightEvents = 0
    private var savedBytes = 0L
    private var report = DiagnosticReportStatus.WAITING
    private var connected: Boolean? = null
    private var recording = RecordingState.UNKNOWN
    private var sourceActive = false
    private var accepting = true
    internal var beforeAppendForTest: () -> Unit = {}

    init {
        require(maxBytes > 0 && maxEvents > 0)
        event("process_run_started", "prior continuity unknown")
    }

    fun sourceStarted() =
        synchronized(lock) {
            accepting = true
            sourceActive = true
            event("source_started")
        }

    fun sourceStopped(failed: Boolean) =
        synchronized(lock) {
            accepting = false
            sourceActive = false
            connected = false
            recording = RecordingState.UNKNOWN
            clearReadings()
            event(if (failed) "source_failed" else "source_stopped")
        }

    fun connection(value: Boolean) =
        synchronized(lock) {
            connected = value
            if (!value) clearReadings()
            event(if (value) "host_connected" else "host_disconnected")
        }

    fun fitBinding(started: Boolean) =
        synchronized(lock) {
            event(if (started) "fit_binding_started" else "fit_binding_stopped")
        }

    fun accept(input: SourceObservation) =
        synchronized(lock) {
            if (!accepting) return@synchronized
            when (val payload = input.payload) {
                is InputPayload.MetricReading -> {
                    val old = readings[payload.metric]
                    val interval =
                        old?.stamp
                            ?.takeIf {
                                it.epochId == input.stamp.epochId &&
                                    input.stamp.elapsedNanos >= it.elapsedNanos
                            }
                            ?.let { input.stamp.elapsedNanos - it.elapsedNanos }
                    readings[payload.metric] =
                        DiagnosticMetric(
                            value = payload.value,
                            reason = payload.missingReason,
                            callbacks = increment(old?.callbacks ?: 0),
                            stamp = input.stamp,
                            intervalNanos = interval,
                            minimumIntervalNanos =
                                interval?.let { minOf(it, old.minimumIntervalNanos ?: it) },
                            maximumIntervalNanos =
                                interval?.let { maxOf(it, old.maximumIntervalNanos ?: it) },
                        )
                    dirty = true
                }
                is InputPayload.Recording -> {
                    recording = payload.state
                    event("recording_state", payload.state.name)
                }
                is InputPayload.Profile -> Unit
            }
        }

    fun snapshot(): DiagnosticSnapshot =
        synchronized(lock) {
            val now = nowNanos()
            DiagnosticSnapshot(
                readings.mapValues { (_, reading) ->
                    reading.copy(
                        ageNanos =
                            reading.stamp?.let { (now - it.elapsedNanos).takeIf { n -> n >= 0 } }
                    )
                },
                report,
                savedBytes,
                dropped,
                events.size + inFlightEvents,
                connected,
                recording,
                sourceActive,
            )
        }

    /** Only the single background writer calls this; fsync precedes the saved-state update. */
    fun flush() =
        synchronized(writerLock) {
            val bytes =
                synchronized(lock) {
                    if (
                        !dirty ||
                            report !in
                                setOf(DiagnosticReportStatus.WAITING, DiagnosticReportStatus.SAVED)
                    )
                        return@synchronized null
                    val metadata =
                        JSONObject()
                            .put(
                                "channels",
                                JSONArray(
                                    readings.map { (metric, r) ->
                                        JSONObject()
                                            .put("metric", metric.name)
                                            .put("has_value", r.value != null)
                                            .put("reason", r.reason?.name ?: JSONObject.NULL)
                                            .put("callbacks", r.callbacks)
                                            .put(
                                                "receipt_elapsed_nanos",
                                                r.stamp?.elapsedNanos ?: JSONObject.NULL,
                                            )
                                            .put(
                                                "receipt_utc",
                                                r.stamp?.utc?.toString() ?: JSONObject.NULL,
                                            )
                                            .put(
                                                "receipt_epoch",
                                                r.stamp?.epochId?.toString() ?: JSONObject.NULL,
                                            )
                                            .put(
                                                "preceding_interval_nanos",
                                                r.intervalNanos ?: JSONObject.NULL,
                                            )
                                            .put(
                                                "minimum_interval_nanos",
                                                r.minimumIntervalNanos ?: JSONObject.NULL,
                                            )
                                            .put(
                                                "maximum_interval_nanos",
                                                r.maximumIntervalNanos ?: JSONObject.NULL,
                                            )
                                    }
                                ),
                            )
                            .put("diagnostic_events_dropped", dropped)
                            .put("source_active", sourceActive)
                            .put("connected", connected ?: JSONObject.NULL)
                            .put("recording", recording.name)
                    val lines =
                        events.toList() + document("status_snapshot").put("status", metadata)
                    inFlightEvents = events.size
                    events.clear()
                    dirty = false
                    lines.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)
                } ?: return@synchronized
            try {
                val length = file.length()
                if (length + bytes.size > maxBytes) {
                    synchronized(lock) { report = DiagnosticReportStatus.LIMIT_REACHED }
                    return@synchronized
                }
                if (length > 0) {
                    RandomAccessFile(file, "r").use { stream ->
                        stream.seek(length - 1)
                        if (stream.read() != '\n'.code) {
                            synchronized(lock) { report = DiagnosticReportStatus.INCOMPLETE }
                            return@synchronized
                        }
                    }
                }
                file.parentFile?.mkdirs()
                beforeAppendForTest()
                FileOutputStream(file, true).use { stream ->
                    stream.write(bytes)
                    stream.flush()
                    stream.fd.sync()
                }
                synchronized(lock) {
                    savedBytes = length + bytes.size
                    report = DiagnosticReportStatus.SAVED
                }
            } catch (_: IOException) {
                synchronized(lock) { report = DiagnosticReportStatus.STORAGE_ERROR }
            } finally {
                synchronized(lock) { inFlightEvents = 0 }
            }
        }

    private fun document(kind: String) =
        JSONObject()
            .put("format", "oxywatt-diagnostic-report-v1")
            .put("kind", kind)
            .put("process_run", processRun.toString())
            .put("code_revision", BuildConfig.CODE_REVISION)
            .put("utc", Instant.now().toString())
            .put("elapsed_nanos", nowNanos())
            .put("time_meaning", "local_receipt_not_sensor_measurement")

    private fun event(kind: String, detail: String? = null) {
        if (events.size + inFlightEvents >= maxEvents) dropped = increment(dropped)
        else events.addLast(document(kind).put("detail", detail ?: JSONObject.NULL))
        dirty = true
    }

    private fun clearReadings() {
        readings.replaceAll { _, reading ->
            reading.copy(value = null, reason = MissingReason.UNAVAILABLE)
        }
        dirty = true
    }

    private fun increment(value: Long) = if (value == Long.MAX_VALUE) value else value + 1
}
