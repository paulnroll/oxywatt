package com.oxywatt.diagnostic

import android.content.Context
import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
import java.io.File
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import org.json.JSONObject

/** Private host-qualified archive; no FIT data is bundled in an APK or relabelled as live input. */
internal class FitArchive(private val directory: File) {
    private val manifestFile = File(directory, "manifest.json")
    private val projection = File(directory, "observations.tsv")
    val manifest: JSONObject
    val epoch: UUID
    val id: UUID
    val origin: Instant
    val end: Long

    init {
        require(manifestFile.length() in 1..65536 && projection.length() in 1..16777216)
        manifest = JSONObject(manifestFile.readText())
        require(manifest.getString("schema") == "fit-history-archive-v1")
        require(
            manifest.getString("decoder") == "fitdecode" &&
                manifest.getString("decoder_version") == "0.11.0"
        )
        require(manifest.getString("normalization_version") == "fit-normalization-v1")
        require(manifest.getString("provenance") == "private-owner-input")
        require(manifest.getString("timestamp_policy") == "ordered-original-file-time-v1")
        require(manifest.getString("scheduler_policy") == "timers-before-input-v1")
        require(manifest.getString("completion") == "file-session-activity-stop-v1")
        require(manifest.getString("input_sha256").matches(Regex("[0-9a-f]{64}")))
        require(manifest.getLong("projected_rows") in 1..131072)
        origin = instant(manifest.getString("file_origin"))
        end = manifest.getLong("end_offset_nanos")
        require(end in 1..86_400_000_000_000L)
        require(instant(manifest.getString("session_start")) <= origin)
        require(instant(manifest.getString("session_end")) >= origin.plusNanos(end))
        require(
            Duration.between(
                instant(manifest.getString("session_start")),
                instant(manifest.getString("session_end")),
            ) in Duration.ofSeconds(1)..Duration.ofDays(1)
        )
        epoch =
            UUID.nameUUIDFromBytes(
                ("fit-epoch-v1:" + manifest.getString("input_sha256")).toByteArray()
            )
        id =
            UUID.nameUUIDFromBytes(
                ("fit-session-v1:" + manifest.getString("input_sha256")).toByteArray()
            )
        verifyHash()
        // Full shape/order/lifecycle validation before opening a database or allocating a result.
        observations().forEach {}
    }

    fun verifyHash() {
        val digest = MessageDigest.getInstance("SHA-256")
        projection.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        require(
            digest.digest().joinToString("") { "%02x".format(it) } ==
                manifest.getString("projection_sha256")
        )
    }

    fun metadata(): String =
        JSONObject()
            .put("version", 1)
            .put("origin", FitSource.ORIGIN)
            .put("epoch", epoch.toString())
            .put("sourceHash", manifest.getString("input_sha256"))
            .put("projectionHash", manifest.getString("projection_sha256"))
            .put("decoder", "fitdecode:0.11.0")
            .put("clock", "reconstructed-file-time-v1")
            .put("recordSupport", "hold-to-next-record-max-1s-v1")
            .put("completion", "file-session-activity-stop-v1")
            .put("fileDate", instant(manifest.getString("session_start")).toString())
            .put("rows", manifest.getLong("projected_rows"))
            .put("importedAt", Instant.now().toString())
            .put("storage", LiveDiagnosticStorage.POLICY)
            .put("maximumBytes", LiveDiagnosticStorage.MAXIMUM_BYTES)
            .toString()

    fun observations(): Sequence<SourceObservation> = sequence {
        var sequence = 0L
        var previous = -1L
        var previousMessage = -1L
        var rows = 0L
        var recording = false
        var timers = 0
        var channel = 0
        var recordAt = -1L
        var due: Long? = null
        fun input(t: Long, payload: InputPayload) =
            SourceObservation(
                ReceiptStamp(origin.plusNanos(t), epoch, t, sequence++),
                SourceRef("fit-file", "fitdecode-0.11.0"),
                FitSource.NORMALIZATION,
                payload,
            )
        projection.bufferedReader().use { reader ->
            require(
                reader.readLine() ==
                    "message_index\tfile_timestamp\toffset_nanos\tkind\tvalue\treason\ttimer_type"
            )
            for (line in reader.lineSequence()) {
                require(line.length <= 1024)
                val c = line.split('\t')
                require(c.size == 7)
                val message = c[0].toLong()
                require(message >= previousMessage)
                previousMessage = message
                val at = c[2].toLong()
                require(at >= previous && at in 0..end)
                val stamp = instant(c[1])
                require(Duration.between(origin, stamp).toNanos() == at)
                if (due != null && at > due!!) {
                    // Derived absence boundary, not a fabricated sensor/file measurement.
                    yield(
                        input(
                            due!!,
                            InputPayload.MetricReading(Metric.POWER_W, null, MissingReason.DROPOUT),
                        )
                    )
                    due = null
                }
                if (c[3] == "STATE") {
                    require(channel == 0 && c[5] == "-")
                    val start = c[6] == "start"
                    require(start || c[6] in setOf("stop", "stop_all"))
                    require(c[4] == if (start) "RECORDING" else "PAUSED")
                    require(start != recording)
                    if (rows == 0L) require(start && at == 0L)
                    recording = start
                    timers++
                    yield(
                        input(
                            at,
                            InputPayload.Recording(
                                if (start) RecordingState.RECORDING else RecordingState.PAUSED
                            ),
                        )
                    )
                } else {
                    require(timers > 0 && c[6] == "-")
                    val metric =
                        listOf(Metric.POWER_W, Metric.HEART_RATE_BPM, Metric.CADENCE_RPM)[channel]
                    require(c[3] == metric.name)
                    if (channel == 0) {
                        recordAt = at
                        due = at + 1_000_000_000L
                    } else require(at == recordAt)
                    val value = c[4].takeUnless { it == "-" }?.toDouble()
                    val reason = c[5].takeUnless { it == "-" }?.let(MissingReason::valueOf)
                    require((value == null) == (reason != null))
                    require(
                        value == null ||
                            (value.isFinite() &&
                                value >= 0 &&
                                (metric != Metric.HEART_RATE_BPM || value > 0))
                    )
                    yield(
                        input(
                            at,
                            InputPayload.MetricReading(
                                metric,
                                value,
                                reason,
                                SourceTime(stamp, SourceTimeMeaning.FILE_RECORD),
                            ),
                        )
                    )
                    channel = (channel + 1) % 3
                }
                previous = at
                rows++
            }
        }
        require(rows == manifest.getLong("projected_rows") && previous == end)
        require(channel == 0 && timers > 0 && !recording)
    }

    private fun instant(text: String): Instant = OffsetDateTime.parse(text).toInstant()
}

internal class FitHistoryImporter(
    private val context: Context,
    private val name: String = "oxywatt-live-diagnostic.db",
    private val configure: (InputRepository) -> Unit = {},
) {
    fun importArchive(directory: File): UUID {
        val archive = FitArchive(directory)
        val metadata = archive.metadata()
        InputRepository(context, name, LiveDiagnosticStorage.MAXIMUM_BYTES, metadata).use { inputs
            ->
            if (inputs.session(archive.id) != null) {
                AnalysisRepository(context, name, LiveDiagnosticStorage.MAXIMUM_BYTES).use { results
                    ->
                    val existing =
                        readRealSavedCurve(results, inputs.session(archive.id)!!, fit = true)
                    require(existing?.status == AnalysisStatus.AVAILABLE) {
                        "FIT_ALREADY_PRESENT_WITHOUT_SAVED_RESULT"
                    }
                    require(
                        JSONObject(existing.terminal!!.curve!!.acquisition!!)
                            .getString("projectionHash") ==
                            archive.manifest.getString("projection_sha256")
                    )
                    return archive.id
                }
            }
            require(
                inputs.checkpoint()?.latestSession?.phase !in
                    setOf(SessionPhase.RECORDING, SessionPhase.PAUSED)
            ) {
                "ACTIVE_CANONICAL_SESSION"
            }
            configure(inputs)
            inputs.atomicImport {
                val lifecycle = CollectorLifecycle(archive.epoch, FitSource.policy, { archive.id })
                val queue = BoundedAdmission()
                fun flush() {
                    val batch = queue.takeBatch() ?: return
                    inputs.commit(inputs.prepare(batch, lifecycle, inputs.state().revision))
                    queue.committed(batch)
                }
                for (input in archive.observations()) {
                    check(queue.offer(input))
                    if (queue.pendingCount >= 64) flush()
                }
                flush()
                val ended = lifecycle.interrupt(InterruptionReason.SOURCE_STOPPED)
                FitSource.requireSession(ended.checkpoint.latestSession!!)
                inputs.commit(
                    inputs.prepareEffect(ended, AdmissionPolicy(), inputs.state().revision)
                )
                archive.verifyHash()
            }
        }
        InputRepository(context, name, LiveDiagnosticStorage.MAXIMUM_BYTES).use { inputs ->
            AnalysisRepository(context, name, LiveDiagnosticStorage.MAXIMUM_BYTES).use { results ->
                val completion =
                    PowerCurveCalculation(inputs, results)
                        .prepareFit(archive.id, BuildConfig.CODE_REVISION)
                results.complete(completion.generation, completion.terminal)
                require(completion.terminal.status == AnalysisStatus.AVAILABLE) {
                    "FIT_ANALYSIS_UNAVAILABLE"
                }
            }
        }
        return archive.id
    }
}
