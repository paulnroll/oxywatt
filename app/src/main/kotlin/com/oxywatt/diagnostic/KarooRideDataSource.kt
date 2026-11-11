package com.oxywatt.diagnostic

import android.os.SystemClock
import com.oxywatt.core.collection.*
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** SDK receipt adapter. No native session identity, measurement time or backfill is inferred. */
internal class KarooRideDataSource(
    private val system: KarooSystemService,
    private val stamp: () -> ReceiptStamp = {
        ReceiptStamp(
            Instant.now(),
            processEpoch,
            SystemClock.elapsedRealtimeNanos(),
            sequence.getAndIncrement(),
        )
    },
    private val onRide: (RideState) -> Unit = {},
    private val onStream: (String, StreamState) -> Unit = { _, _ -> },
    private val onConnection: (Boolean) -> Unit = {},
) : RideDataSource {
    private val lock = Any()
    private var settingUp = true
    private var bindingAttempted = false
    private var started = false
    private var stopping = false
    private var stopped = false
    private var delivering = false
    private var failure: Throwable? = null
    private var accept: (SourceObservation) -> Unit = {}
    private var onStopped: (Throwable?) -> Unit = {}
    private val consumers = mutableListOf<String>()

    override fun start(
        accept: (SourceObservation) -> Unit,
        onStopped: (Throwable?) -> Unit,
    ): AutoCloseable {
        synchronized(lock) {
            check(!started) { "Source is single-use" }
            started = true
            this.accept = accept
            this.onStopped = onStopped
        }
        try {
            consumers +=
                system.addConsumer<RideState>(onError = { fail() }, onComplete = { fail() }) {
                    deliver {
                        onRide(it)
                        KarooMapping.recording(it)
                    }
                }
            consumers +=
                system.addConsumer<UserProfile>(onError = { fail() }, onComplete = { fail() }) {
                    deliver { KarooMapping.profile(it) }
                }
            observe("power", DataType.Type.POWER, DataType.Field.POWER, Metric.POWER_W)
            observe(
                "heart-rate",
                DataType.Type.HEART_RATE,
                DataType.Field.HEART_RATE,
                Metric.HEART_RATE_BPM,
            )
            observe("cadence", DataType.Type.CADENCE, DataType.Field.CADENCE, Metric.CADENCE_RPM)
            bindingAttempted = true
            system.connect { connected ->
                synchronized(lock) { if (!stopping) onConnection(connected) }
            }
        } catch (error: Throwable) {
            synchronized(lock) {
                failure = error
                stopping = true
            }
        } finally {
            synchronized(lock) { settingUp = false }
            finish()
        }
        return AutoCloseable {
            synchronized(lock) { stopping = true }
            finish()
        }
    }

    private fun observe(label: String, type: String, field: String, metric: Metric) {
        consumers +=
            system.addConsumer<OnStreamState>(
                OnStreamState.StartStreaming(type),
                onError = { fail() },
                onComplete = { fail() },
            ) {
                deliver {
                    onStream(label, it.state)
                    KarooMapping.metric(metric, field, it.state)
                }
            }
    }

    private fun fail() {
        synchronized(lock) {
            if (!stopped) {
                failure = IllegalStateException("Karoo subscription ended")
                stopping = true
            }
        }
        finish()
    }

    private fun deliver(payload: () -> InputPayload) {
        try {
            synchronized(lock) {
                if (stopping) return
                if (delivering) {
                    failure = IllegalStateException("Reentrant Karoo delivery")
                    stopping = true
                    return
                }
                delivering = true
                try {
                    accept(
                        SourceObservation(
                            stamp(),
                            SourceRef("karoo", "1.1.9"),
                            "karoo-normalization-v1",
                            payload(),
                        )
                    )
                } catch (error: Throwable) {
                    failure = error
                    stopping = true
                } finally {
                    delivering = false
                }
            }
        } finally {
            finish()
        }
    }

    private fun finish() {
        // close from accept is reentrant: let the outer delivery release its lock first.
        if (Thread.holdsLock(lock)) return
        val ids =
            synchronized(lock) {
                if (!stopping || stopped || settingUp || delivering) return
                stopped = true
                consumers.toList().also { consumers.clear() }
            }
        // Binder calls must not hold the delivery monitor: a concurrent SDK callback may need it.
        for (id in ids) try {
            system.removeConsumer(id)
        } catch (error: Throwable) {
            recordFailure(error)
        }
        try {
            if (bindingAttempted) system.disconnect()
        } catch (error: Throwable) {
            recordFailure(error)
        }
        onStopped(failure)
    }

    private fun recordFailure(error: Throwable) {
        if (failure == null) failure = error
        else if (failure !== error) failure!!.addSuppressed(error)
    }

    private companion object {
        val processEpoch: UUID = UUID.randomUUID()
        val sequence = AtomicLong()
    }
}
