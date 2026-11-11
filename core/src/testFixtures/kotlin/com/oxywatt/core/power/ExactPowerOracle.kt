package com.oxywatt.core.power

import java.math.BigDecimal
import java.math.MathContext

/** Test-only exact energy prefixes. No production traversal, integrator or comparator. */
object ExactPowerOracle {
    fun records(spans: List<List<PowerInterval>>): List<PowerRecord?> =
        PowerCurveEngine.DURATIONS.map { seconds ->
            val width = seconds * 1_000_000_000L
            var best: BigDecimal? = null
            var record: PowerRecord? = null
            for (parts in spans.filter { it.isNotEmpty() }) {
                val lower = parts.first().startNanos
                val upper = parts.last().endNanos - width
                if (upper < lower) continue
                val prefix = arrayListOf(BigDecimal.ZERO)
                for (p in parts) prefix.add(
                    prefix.last() +
                        p.watts.toBigDecimal() * (p.endNanos - p.startNanos).toBigDecimal()
                )
                fun energy(t: Long): BigDecimal {
                    var lo = 0
                    var hi = parts.size
                    while (lo < hi) {
                        val mid = (lo + hi) / 2
                        if (parts[mid].startNanos < t) lo = mid + 1 else hi = mid
                    }
                    if (lo == 0) return BigDecimal.ZERO
                    val i = lo - 1
                    return prefix[i] +
                        parts[i].watts.toBigDecimal() * (t - parts[i].startNanos).toBigDecimal()
                }
                val starts = java.util.TreeSet<Long>()
                starts.add(lower)
                starts.add(upper)
                for (p in parts) for (edge in listOf(p.startNanos, p.endNanos)) {
                    if (edge in lower..upper) starts.add(edge)
                    if (edge >= width && edge - width in lower..upper) starts.add(edge - width)
                }
                for (t in starts) {
                    val mean =
                        (energy(t + width) - energy(t)).divide(
                            width.toBigDecimal(),
                            MathContext(80),
                        )
                    val old = best
                    val tolerance =
                        maxOf(
                            "1e-8".toBigDecimal(),
                            maxOf(mean, old ?: BigDecimal.ZERO) * "1e-12".toBigDecimal(),
                        )
                    val earlier =
                        record?.let { it.epochId == parts.first().epochId && t < it.startNanos }
                            ?: false
                    if (
                        old == null ||
                            mean - old > tolerance ||
                            ((mean - old).abs() <= tolerance && earlier)
                    ) {
                        best = mean
                        val p = parts.first()
                        record =
                            PowerRecord(
                                seconds,
                                mean.toDouble(),
                                t,
                                t + width,
                                p.sessionId,
                                p.epochId,
                                p.segmentIndex,
                            )
                    }
                }
            }
            record
        }
}
