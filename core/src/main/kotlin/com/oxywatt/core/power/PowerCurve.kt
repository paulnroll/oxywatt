package com.oxywatt.core.power

import java.util.UUID

data class PowerInterval(
    val sessionId: UUID,
    val epochId: UUID,
    val segmentIndex: Long,
    val startNanos: Long,
    val endNanos: Long,
    val watts: Double,
)

data class PowerCurvePolicy(
    val version: String = "power-intervals-v1",
    val maxIntervals: Int = 16384,
) {
    init {
        require(version.isNotBlank() && maxIntervals > 0)
    }
}

data class PowerProvenance(val preprocessingVersion: String, val coverageOrigin: String) {
    init {
        require(preprocessingVersion.isNotBlank() && coverageOrigin.isNotBlank())
    }
}

data class PowerRecord(
    val durationSeconds: Int,
    val meanWatts: Double,
    val startNanos: Long,
    val endNanos: Long,
    val sessionId: UUID,
    val epochId: UUID,
    val segmentIndex: Long,
)

enum class PowerUnavailableReason {
    INSUFFICIENT_CONTIGUOUS_COVERAGE,
    RESOURCE_LIMIT,
}

data class PowerDurationResult(
    val durationSeconds: Int,
    val record: PowerRecord?,
    val unavailableReason: PowerUnavailableReason?,
)

data class PowerCurveSnapshot(
    val sessionId: UUID,
    val provenance: PowerProvenance,
    val policy: PowerCurvePolicy,
    val durations: List<PowerDurationResult>,
    val resourceLimited: Boolean,
    val retainedIntervals: Int,
    val evaluatedWindows: Long,
    val integratedFragments: Long,
)

/** Single-session, caller-serialized calculation over confirmed coverage only. */
class PowerCurveEngine(
    private val sessionId: UUID,
    private val provenance: PowerProvenance,
    private val policy: PowerCurvePolicy = PowerCurvePolicy(),
) {

    private val intervals = java.util.TreeMap<Long, PowerInterval>()
    private var winners = mutableMapOf<Int, PowerRecord>()
    private var lastAccepted: PowerInterval? = null
    private var coverageStart = 0L
    private var resourceLimited = false
    private var evaluatedWindows = 0L
    private var integratedFragments = 0L

    fun accept(interval: PowerInterval) {
        require(interval.sessionId == sessionId)
        require(
            interval.segmentIndex >= 0 &&
                interval.startNanos >= 0 &&
                interval.endNanos > interval.startNanos
        )
        require(interval.watts.isFinite() && interval.watts >= 0)
        val previous = lastAccepted
        if (previous != null && previous.epochId == interval.epochId) {
            require(interval.startNanos >= previous.endNanos) {
                "Overlapping or regressing interval"
            }
            require(interval.segmentIndex >= previous.segmentIndex) { "Regressing segment" }
        }
        val contiguous =
            intervals.isNotEmpty() &&
                previous != null &&
                previous.epochId == interval.epochId &&
                previous.segmentIndex == interval.segmentIndex &&
                previous.endNanos == interval.startNanos
        val merge = contiguous && intervals.lastEntry().value.watts == interval.watts
        val saturated = contiguous && !merge && intervals.size == policy.maxIntervals
        val reset = !contiguous || saturated
        val history = if (reset) java.util.TreeMap<Long, PowerInterval>() else intervals
        val spanStart = if (reset) interval.startNanos else coverageStart
        val proposed = winners.toMutableMap()
        var windows = 0L
        var fragments = 0L
        for (seconds in DURATIONS) {
            val duration = seconds * NANOS_PER_SECOND
            if (interval.endNanos - spanStart < duration) continue
            val minimumEnd = maxOf(interval.startNanos, spanStart + duration)
            val ends = java.util.TreeSet<Long>()
            ends.add(minimumEnd)
            ends.add(interval.endNanos)
            // Arrival boundaries shifted by duration are expiration-side slope changes.
            val lower = maxOf(0, minimumEnd - duration)
            val upper = interval.endNanos - duration
            for (boundary in history.subMap(lower, true, upper, true).keys) ends.add(
                boundary + duration
            )
            if (interval.startNanos <= upper) ends.add(interval.startNanos + duration)
            for (end in ends) {
                val start = end - duration
                var sum = 0.0
                var correction = 0.0
                fun add(part: PowerInterval) {
                    val overlap = minOf(end, part.endNanos) - maxOf(start, part.startNanos)
                    if (overlap <= 0) return
                    val weighted = part.watts * (overlap.toDouble() / duration.toDouble())
                    val adjusted = weighted - correction
                    val next = sum + adjusted
                    correction = (next - sum) - adjusted
                    sum = next
                    fragments++
                }
                val first = history.floorKey(start) ?: history.ceilingKey(start)
                if (first != null)
                    for (part in history.tailMap(first, true).values) {
                        if (part.startNanos >= end) break
                        add(part)
                    }
                add(interval)
                require(sum.isFinite()) { "Nonfinite power integral" }
                windows++
                val best = proposed[seconds]
                val tolerance = maxOf(1e-8, 1e-12 * maxOf(sum, best?.meanWatts ?: 0.0))
                if (
                    best == null ||
                        sum - best.meanWatts > tolerance ||
                        (kotlin.math.abs(sum - best.meanWatts) <= tolerance &&
                            best.epochId == interval.epochId &&
                            start < best.startNanos)
                ) {
                    proposed[seconds] =
                        PowerRecord(
                            seconds,
                            sum,
                            start,
                            end,
                            sessionId,
                            interval.epochId,
                            interval.segmentIndex,
                        )
                }
            }
        }
        // Publish only after all validation/calculation succeeds; rejected inputs are retryable.
        if (reset) intervals.clear()
        if (!reset && merge) {
            val tail = intervals.pollLastEntry().value
            intervals[tail.startNanos] = interval.copy(startNanos = tail.startNanos)
        } else intervals[interval.startNanos] = interval
        val cutoff = maxOf(0, interval.endNanos - MAX_DURATION_NANOS)
        while (intervals.isNotEmpty() && intervals.firstEntry().value.endNanos <= cutoff) intervals
            .pollFirstEntry()
        val first = intervals.firstEntry()?.value
        if (first != null && first.startNanos < cutoff) {
            intervals.pollFirstEntry()
            intervals[cutoff] = first.copy(startNanos = cutoff)
        }
        coverageStart = spanStart
        lastAccepted = interval
        winners = proposed
        resourceLimited = resourceLimited || saturated
        evaluatedWindows = saturatingAdd(evaluatedWindows, windows)
        integratedFragments = saturatingAdd(integratedFragments, fragments)
    }

    fun breakCoverage() {
        intervals.clear()
    }

    fun snapshot() =
        PowerCurveSnapshot(
            sessionId,
            provenance,
            policy,
            java.util.Collections.unmodifiableList(
                DURATIONS.map {
                    PowerDurationResult(
                        it,
                        winners[it],
                        if (winners[it] != null) null
                        else if (resourceLimited) PowerUnavailableReason.RESOURCE_LIMIT
                        else PowerUnavailableReason.INSUFFICIENT_CONTIGUOUS_COVERAGE,
                    )
                }
            ),
            resourceLimited,
            intervals.size,
            evaluatedWindows,
            integratedFragments,
        )

    private fun saturatingAdd(a: Long, b: Long) =
        if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b

    companion object {
        val DURATIONS: List<Int> =
            java.util.Collections.unmodifiableList(
                listOf(5, 30, 60, 180, 300, 600, 1200, 1800, 3600)
            )
        private const val NANOS_PER_SECOND = 1_000_000_000L
        private const val MAX_DURATION_NANOS = 3600 * NANOS_PER_SECOND
    }
}
