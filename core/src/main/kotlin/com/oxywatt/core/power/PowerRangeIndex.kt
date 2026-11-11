package com.oxywatt.core.power

import java.util.concurrent.CancellationException

/** Work units are structural operations, not CPU time or v1 reservations. */
data class PowerWorkStats(
    val visits: Long = 0,
    val contributions: Long = 0,
    val candidates: Long = 0,
    val mutations: Long = 0,
) {
    val total: Long
        get() = visits + contributions + candidates + mutations
}

internal class PowerWorkLimit : RuntimeException()

internal class PowerWork(private val limit: Long, private val cancelled: () -> Boolean) {
    private val counts = LongArray(4)
    var total = 0L
        private set

    init {
        require(limit > 0)
    }

    // Check BEFORE each visit, contribution, candidate insertion/evaluation or node construction.
    fun charge(category: Int) {
        if (total % 1024L == 0L && cancelled()) throw CancellationException()
        if (total == limit) throw PowerWorkLimit()
        total++
        counts[category]++
    }

    fun stats() = PowerWorkStats(counts[0], counts[1], counts[2], counts[3])
}

/** Persistent AVL: staging shares untouched branches; no session-energy prefix exists. */
internal class PowerRangeIndex(private val work: PowerWork) {
    var sizePeak = 0
        private set

    var heightPeak = 0
        private set

    class Node(
        val part: PowerInterval,
        val left: Node?,
        val right: Node?,
        val mean: Double,
    ) {
        val height: Int = 1 + maxOf(left?.height ?: 0, right?.height ?: 0)
        val size: Int = 1 + (left?.size ?: 0) + (right?.size ?: 0)
        val start: Long = left?.start ?: part.startNanos
        val end: Long = right?.end ?: part.endNanos
    }

    private fun node(p: PowerInterval, l: Node?, r: Node?): Node {
        work.charge(3)
        val duration = (r?.end ?: p.endNanos) - (l?.start ?: p.startNanos)
        val sum = WeightedMean(duration, work)
        if (l != null) sum.add(l.mean, l.end - l.start)
        sum.add(p.watts, p.endNanos - p.startNanos)
        if (r != null) sum.add(r.mean, r.end - r.start)
        return Node(p, l, r, sum.value()).also {
            sizePeak = maxOf(sizePeak, it.size)
            heightPeak = maxOf(heightPeak, it.height)
        }
    }

    private fun balance(p: PowerInterval, l: Node?, r: Node?): Node {
        val delta = (l?.height ?: 0) - (r?.height ?: 0)
        if (delta > 1) {
            val a = l!!
            if ((a.left?.height ?: 0) >= (a.right?.height ?: 0))
                return node(a.part, a.left, node(p, a.right, r))
            val b = a.right!!
            return node(b.part, node(a.part, a.left, b.left), node(p, b.right, r))
        }
        if (delta < -1) {
            val a = r!!
            if ((a.right?.height ?: 0) >= (a.left?.height ?: 0))
                return node(a.part, node(p, l, a.left), a.right)
            val b = a.left!!
            return node(b.part, node(p, l, b.left), node(a.part, b.right, a.right))
        }
        return node(p, l, r)
    }

    fun put(n: Node?, p: PowerInterval): Node {
        if (n == null) return node(p, null, null)
        work.charge(0)
        return when {
            p.startNanos < n.part.startNanos -> balance(n.part, put(n.left, p), n.right)
            p.startNanos > n.part.startNanos -> balance(n.part, n.left, put(n.right, p))
            else -> node(p, n.left, n.right)
        }
    }

    fun first(n: Node): PowerInterval {
        work.charge(0)
        return if (n.left == null) n.part else first(n.left)
    }

    fun removeFirst(n: Node): Node? {
        work.charge(0)
        return if (n.left == null) {
            work.charge(3)
            n.right
        } else balance(n.part, removeFirst(n.left), n.right)
    }

    fun boundaries(n: Node?, lower: Long, upper: Long, consume: (Long) -> Unit) {
        if (n == null) return
        work.charge(0)
        if (n.start > upper || n.end <= lower) return
        if (n.part.startNanos >= lower) boundaries(n.left, lower, upper, consume)
        if (n.part.startNanos in lower..upper) consume(n.part.startNanos)
        if (n.part.startNanos <= upper) boundaries(n.right, lower, upper, consume)
    }

    fun integrate(n: Node?, start: Long, end: Long, sum: WeightedMean) {
        if (n == null) return
        work.charge(0)
        if (n.end <= start || n.start >= end) return
        if (n.start >= start && n.end <= end) {
            sum.add(n.mean, n.end - n.start)
            return
        }
        integrate(n.left, start, end, sum)
        val overlap = minOf(end, n.part.endNanos) - maxOf(start, n.part.startNanos)
        if (overlap > 0) sum.add(n.part.watts, overlap)
        integrate(n.right, start, end, sum)
    }
}

/** Local normalized terms avoid overflow in W*ns and cancellation between large totals. */
internal class WeightedMean(private val duration: Long, private val work: PowerWork) {
    private var sum = 0.0
    private var correction = 0.0
    var terms = 0L
        private set

    fun add(watts: Double, nanos: Long) {
        work.charge(1)
        val adjusted = watts * (nanos.toDouble() / duration.toDouble()) - correction
        val next = sum + adjusted
        correction = (next - sum) - adjusted
        sum = next
        terms++
    }

    fun value(): Double {
        require(sum.isFinite()) { "Nonfinite power integral" }
        return sum
    }
}
