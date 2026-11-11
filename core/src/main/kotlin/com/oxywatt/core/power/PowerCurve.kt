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
    val version: String = "power-range-means-v2",
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
    workBudget: Long = 100_000_000,
    cancelled: () -> Boolean = { false },
) {

    private val work = PowerWork(workBudget, cancelled)
    private val index = PowerRangeIndex(work)
    private var root: PowerRangeIndex.Node? = null
    val workStats: PowerWorkStats
        get() = work.stats()

    var workLimited = false
        private set

    val retainedPeak: Int
        get() = index.sizePeak

    // Conservative bound includes both roots, transient AVL paths and rotations, including
    // staging before eviction or an exhausted append. No caller can retain tree snapshots.
    val stagedNodeBound: Int
        get() = if (index.sizePeak == 0) 0 else 2 * index.sizePeak + 2 * index.heightPeak + 2

    var candidatePeak = 0
        private set

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
        if (workLimited) return
        val previous = lastAccepted
        if (previous != null && previous.epochId == interval.epochId) {
            require(interval.startNanos >= previous.endNanos) {
                "Overlapping or regressing interval"
            }
            require(interval.segmentIndex >= previous.segmentIndex) { "Regressing segment" }
        }
        val contiguous =
            root != null &&
                previous != null &&
                previous.epochId == interval.epochId &&
                previous.segmentIndex == interval.segmentIndex &&
                previous.endNanos == interval.startNanos
        val merge = contiguous && previous.watts == interval.watts
        val saturated = contiguous && !merge && root!!.size == policy.maxIntervals
        val reset = !contiguous || saturated
        val history = if (reset) null else root
        val spanStart = if (reset) interval.startNanos else coverageStart
        val proposed = winners.toMutableMap()
        var windows = 0L
        var fragments = 0L
        try {
            for (seconds in DURATIONS) {
                val duration = seconds * NANOS_PER_SECOND
                if (interval.endNanos - spanStart < duration) continue
                val minimumEnd = maxOf(interval.startNanos, spanStart + duration)
                val ends = java.util.TreeSet<Long>()
                fun candidate(end: Long) {
                    work.charge(2)
                    ends.add(end)
                    candidatePeak = maxOf(candidatePeak, ends.size)
                }
                candidate(minimumEnd)
                candidate(interval.endNanos)
                // Arrival boundaries shifted by duration are expiration-side slope changes.
                val lower = maxOf(0, minimumEnd - duration)
                val upper = interval.endNanos - duration
                index.boundaries(history, lower, upper) { candidate(it + duration) }
                if (interval.startNanos <= upper) candidate(interval.startNanos + duration)
                for (end in ends) {
                    val start = end - duration
                    work.charge(2)
                    val weighted = WeightedMean(duration, work)
                    index.integrate(history, start, end, weighted)
                    val overlap = minOf(end, interval.endNanos) - maxOf(start, interval.startNanos)
                    if (overlap > 0) weighted.add(interval.watts, overlap)
                    val sum = weighted.value()
                    fragments += weighted.terms
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
            // Build a persistent staged index before publishing, so rejection is retryable.
            val admitted =
                if (!reset && merge) interval.copy(startNanos = tailStart(history!!)) else interval
            var staged = index.put(history, admitted)
            val cutoff = maxOf(0, interval.endNanos - MAX_DURATION_NANOS)
            while (true) {
                val first = index.first(staged)
                if (first.endNanos > cutoff) {
                    if (first.startNanos < cutoff) {
                        val rest = index.removeFirst(staged)
                        staged = index.put(rest, first.copy(startNanos = cutoff))
                    }
                    break
                }
                staged = index.removeFirst(staged) ?: break
            }
            root = staged
            coverageStart = spanStart
            lastAccepted = interval
            winners = proposed
            resourceLimited = resourceLimited || saturated
            evaluatedWindows = saturatingAdd(evaluatedWindows, windows)
            integratedFragments = saturatingAdd(integratedFragments, fragments)
        } catch (_: PowerWorkLimit) {
            // Preserve only previously committed candidates; this append is incomplete.
            workLimited = true
            resourceLimited = true
            root = null
        }
    }

    private fun tailStart(n: PowerRangeIndex.Node): Long {
        work.charge(0)
        return n.right?.let(::tailStart) ?: n.part.startNanos
    }

    fun breakCoverage() {
        root = null
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
            root?.size ?: 0,
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
