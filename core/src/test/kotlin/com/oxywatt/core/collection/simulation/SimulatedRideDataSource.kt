package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID

/** Lazily supplied events must already be in acquisition order; ties retain iterator order. */
data class ScheduledObservation(
    val offsetNanos: Long,
    val payload: InputPayload,
    val utcShiftNanos: Long = 0,
)

/** Test-only source. Harness calls pump once per playback deadline; no thread or upstream queue. */
class SimulatedRideDataSource(
    private val events: Sequence<ScheduledObservation>,
    private val clock: VirtualTimeline,
    private val epoch: UUID,
    private val origin: Instant,
    private val endNanos: Long,
    private val normalizationVersion: String = "canonical-v1",
    private val speed: Int? = null,
    private val awaitPlaybackDeadline: (Long) -> Unit = {},
) : RideDataSource {
    private var started = false
    @Volatile private var cancelled = false
    private var delivering = false
    private var stopped = false
    private var iterator: Iterator<ScheduledObservation>? = null
    private var accept: ((SourceObservation) -> Unit)? = null
    private var onStopped: ((Throwable?) -> Unit)? = null
    private var index = 0L
    private var previous = 0L

    init {
        require(endNanos >= 0)
        require(speed == null || speed > 0)
    }

    @Synchronized
    override fun start(
        accept: (SourceObservation) -> Unit,
        onStopped: (Throwable?) -> Unit,
    ): AutoCloseable {
        check(!started) { "Source instances are single-use" }
        started = true
        // Iterator acquisition can fail before start returns; no callbacks/resources acquired yet.
        iterator = events.iterator()
        this.accept = accept
        this.onStopped = onStopped
        return AutoCloseable {
            cancelled = true
            synchronized(this) { if (!delivering) finish(null) }
        }
    }

    /** Returns false after EOF/failure/cancellation. Callback exceptions terminate the source. */
    @Synchronized
    fun pump(): Boolean {
        check(started)
        check(!delivering) { "Reentrant pumping is not supported" }
        if (stopped) return false
        delivering = true
        var failure: Throwable? = null
        var terminal = false
        try {
            val input = checkNotNull(iterator)
            if (cancelled) terminal = true
            else if (!input.hasNext()) {
                awaitPlaybackDeadline(playbackDeadline(endNanos, speed))
                if (!cancelled) clock.advanceTo(endNanos)
                terminal = true
            } else {
                val next = input.next()
                require(next.offsetNanos in previous..endNanos) {
                    "Regressing or out-of-range input"
                }
                awaitPlaybackDeadline(playbackDeadline(next.offsetNanos, speed))
                if (!cancelled) clock.advanceTo(next.offsetNanos)
                if (!cancelled) {
                    accept!!(
                        SourceObservation(
                            ReceiptStamp(
                                origin.plusNanos(
                                    Math.addExact(next.offsetNanos, next.utcShiftNanos)
                                ),
                                epoch,
                                next.offsetNanos,
                                index,
                            ),
                            SourceRef("simulated", "1", index),
                            normalizationVersion,
                            next.payload,
                        )
                    )
                    index = Math.incrementExact(index)
                    previous = next.offsetNanos
                }
            }
        } catch (error: Throwable) {
            failure = error
            terminal = true
        } finally {
            delivering = false
        }
        if (terminal || cancelled) finish(failure)
        return !stopped
    }

    private fun finish(error: Throwable?) {
        if (stopped) return
        stopped = true
        iterator = null
        accept = null
        val callback = onStopped
        onStopped = null
        callback?.invoke(error)
    }
}

/** Absolute deadlines only; a driver may wait for them, tests execute without sleeping. */
fun playbackDeadline(offsetNanos: Long, speed: Int?): Long {
    require(offsetNanos >= 0)
    require(speed == null || speed > 0)
    return if (speed == null) 0 else offsetNanos / speed
}
