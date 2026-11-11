package com.oxywatt.core.power

import java.util.UUID
import kotlin.test.*
import org.junit.Test

class LongPowerCurveTest {
    private val id = UUID(0, 501)
    private val epoch = UUID(0, 502)

    private fun part(a: Long, b: Long, w: Double) = PowerInterval(id, epoch, 0, a, b, w)

    @Test
    fun `four hour variable session is fully searched with exact earliest maxima`() {
        val e = BudgetedPowerCurve(id)
        repeat(14400) { t ->
            e.accept(
                part(t * 1_000_000_000L, (t + 1) * 1_000_000_000L, if (t % 2 == 0) 100.0 else 200.0)
            )
        }
        assertFalse(e.limited, "complete representative scan required")
        assertEquals(14400, e.intervalCount)
        for (d in e.snapshot().durations) {
            val r = assertNotNull(d.record)
            assertEquals(if (d.durationSeconds == 5) 160.0 else 150.0, r.meanWatts, 1e-8)
            assertEquals(if (d.durationSeconds == 5) 1_000_000_000L else 0L, r.startNanos)
        }
    }

    private fun verify(parts: List<PowerInterval>, e: BudgetedPowerCurve) {
        val expected = ExactPowerOracle.records(listOf(parts))
        e.snapshot().durations.forEachIndexed { i, d ->
            val ref = expected[i]
            if (ref == null) assertNull(d.record)
            else {
                val actual = assertNotNull(d.record)
                assertEquals(ref.meanWatts, actual.meanWatts, maxOf(1e-8, 1e-12 * ref.meanWatts))
                assertEquals(ref.copy(meanWatts = actual.meanWatts), actual)
            }
        }
    }

    private fun run(parts: List<PowerInterval>): BudgetedPowerCurve {
        val began = System.nanoTime()
        val e = BudgetedPowerCurve(id)
        parts.forEach(e::accept)
        assertFalse(e.limited)
        assertFalse(e.snapshot().resourceLimited)
        assertEquals(parts.size.toLong(), e.intervalCount)
        assertTrue(e.reserved <= 100_000_000)
        assertTrue(e.retainedPeak >= e.snapshot().retainedIntervals)
        assertTrue(e.stagedNodeBound >= e.retainedPeak)
        assertTrue(e.retainedPeak <= 16384)
        assertTrue(e.stagedNodeBound <= 4 * 16384 + 2)
        assertTrue(e.candidatePeak <= 16384 + 3)
        println(
            "T04b2 n=${parts.size} stats=${e.workStats} windows=${e.snapshot().evaluatedWindows} terms=${e.snapshot().integratedFragments} retainedPeak=${e.retainedPeak} stagedBound=${e.stagedNodeBound} candidates=${e.candidatePeak} ms=${(System.nanoTime()-began)/1e6}"
        )
        return e
    }

    @Test
    fun `four hour irregular nanoseconds and seeded stages match exact energy oracle`() {
        for (seed in 0..1) {
            var t = 0L
            val random = kotlin.random.Random(67)
            val parts =
                (0 until 14400).map { i ->
                    val dt = longArrayOf(250000001, 749999999, 1500000000, 1500000000)[i % 4]
                    part(
                            t,
                            t + dt,
                            if (seed == 0) {
                                if (i % 2 == 0) 100.0 else 200.0
                            } else random.nextInt(0, 1001).toDouble(),
                        )
                        .also { t += dt }
                }
            verify(parts, run(parts))
        }
    }

    @Test
    fun `dense history followed by long interval scans expiry candidates subquadratically`() {
        val parts =
            (0 until 16000).map {
                part(it * 1_000_000L, (it + 1) * 1_000_000L, if (it % 2 == 0) 100.0 else 200.0)
            } + part(16_000_000_000, 3616_000_000_000, 50.0)
        val e = run(parts)
        verify(parts, e)
        assertTrue(e.snapshot().integratedFragments < 10_000_000)
    }

    @Test
    fun `local means preserve small windows after huge power and exact large origins`() {
        val offset = Long.MAX_VALUE - 5000_000_000_000
        val values = listOf(1e308, 0.0, 0.125, 300.0, 300.000000005, 300.00000003)
        val parts = values.mapIndexed { i, w ->
            part(offset + i * 600_000_000_000L, offset + (i + 1) * 600_000_000_000L, w)
        }
        verify(parts, run(parts))
        // A huge historical value cannot corrupt a new low-power coverage span.
        val e = run(parts)
        e.accept(null)
        e.accept(part(offset + 3600_000_000_000, offset + 3605_000_000_000, 0.125))
        assertEquals(1e308, e.snapshot().durations.first().record!!.meanWatts)
        for (delta in listOf(5e-9, 3e-8)) {
            val small =
                listOf(
                    part(0, 5_000_000_000, 300.0),
                    part(5_000_000_000, 10_000_000_000, 300.0 + delta),
                )
            verify(small, run(small))
        }
    }

    @Test
    fun `work exhaustion inside append preserves winners and bounded counters`() {
        val parts =
            (0 until 1000).map {
                part(it * 1_000_000L, (it + 1) * 1_000_000L, if (it % 2 == 0) 100.0 else 200.0)
            }
        val prefix = run(parts)
        val cap = prefix.reserved + 100
        val limited = BudgetedPowerCurve(id, budget = cap)
        parts.forEach(limited::accept)
        assertFalse(limited.limited)
        limited.accept(part(1_000_000_000, 3601_000_000_000, 100.0))
        assertTrue(limited.limited)
        assertEquals(cap, limited.reserved)
        assertEquals(0, limited.snapshot().retainedIntervals)
        assertTrue(limited.snapshot().resourceLimited)
        assertTrue(limited.snapshot().durations.all { it.record == null })
        val before = limited.snapshot()
        limited.accept(part(3601_000_000_000, 7201_000_000_000, 999.0))
        assertEquals(before, limited.snapshot())
        var checks = 0
        val cancelled = BudgetedPowerCurve(id, cancelled = { ++checks > 1 })
        assertFailsWith<java.util.concurrent.CancellationException> {
            parts.forEach(cancelled::accept)
        }
        assertTrue(cancelled.reserved <= 1024)
    }

    @Test
    fun `doubling length keeps cost linear at fixed density`() {
        fun scan(n: Int) =
            run(
                (0 until n).map {
                    part(
                        it * 1_000_000_000L,
                        (it + 1) * 1_000_000_000L,
                        if (it % 2 == 0) 100.0 else 200.0,
                    )
                }
            )
        val a = scan(14400)
        val b = scan(28800)
        assertTrue(b.reserved <= a.reserved * 2.5)
    }

    @Test
    fun `integer cell oracle independently checks seeded four hour spikes and stages`() {
        val cells = IntArray(14400) { i -> if (i % 997 == 0) 1500 else (i / 37 * 71 % 501) }
        val e =
            run(
                cells.mapIndexed { i, w ->
                    part(i * 1_000_000_000L, (i + 1) * 1_000_000_000L, w.toDouble())
                }
            )
        for (d in e.snapshot().durations) {
            val width = d.durationSeconds
            var energy = (0 until width).sumOf { cells[it].toLong() }
            var best = energy
            var at = 0
            for (end in width until cells.size) {
                energy += cells[end] - cells[end - width]
                if (energy > best) {
                    best = energy
                    at = end - width + 1
                }
            }
            val r = assertNotNull(d.record)
            assertEquals(best.toDouble() / width, r.meanWatts, 1e-8)
            assertEquals(at * 1_000_000_000L, r.startNanos)
        }
    }

    @Test
    fun `small complete window after huge short discarded span loses no precision`() {
        val e = BudgetedPowerCurve(id)
        e.accept(part(0, 1, 1e308))
        e.accept(null)
        e.accept(part(1, 5_000_000_001, 0.125))
        val r = assertNotNull(e.snapshot().durations.first().record)
        assertEquals(0.125, r.meanWatts)
        assertEquals(1, r.startNanos)
        val irregular =
            listOf(
                part(0, 1, 1e308),
                part(1, 5_000_000_000, 0.125),
                part(5_000_000_000, 10_000_000_000, 0.25),
            )
        verify(irregular, run(irregular))
    }

    @Test
    fun `repeated four hour broken spans preserve independently attributed winners`() {
        for (mode in 0..3) {
            val e = BudgetedPowerCurve(id)
            val spans = mutableListOf<List<PowerInterval>>()
            for (block in 0 until 4) {
                val origin = if (mode == 3) 0L else block * 3601_000_000_000L
                val span =
                    (0 until 3600).map { i ->
                        part(
                                origin + i * 1_000_000_000L,
                                origin + (i + 1) * 1_000_000_000L,
                                if (i % 2 == 0) 100.0 else 200.0,
                            )
                            .copy(
                                segmentIndex = if (mode == 2) block.toLong() else 0,
                                epochId = if (mode == 3) UUID(0, 502 + block.toLong()) else epoch,
                            )
                    }
                if (mode == 1) e.accept(null)
                span.forEach(e::accept)
                spans.add(span)
            }
            assertFalse(e.snapshot().resourceLimited)
            val expected = ExactPowerOracle.records(spans)
            e.snapshot().durations.forEachIndexed { i, d ->
                val actual = assertNotNull(d.record)
                val ref = assertNotNull(expected[i])
                assertEquals(ref.meanWatts, actual.meanWatts, 1e-8)
                assertEquals(ref.copy(meanWatts = actual.meanWatts), actual)
            }
            assertTrue(e.retainedPeak <= 3601)
        }
    }

    @Test
    fun `inner work exhaustion retains established winner and exact retry rejects bad input`() {
        val prefix =
            listOf(part(0, 5_000_000_000, 250.0)) +
                (0 until 1000).map { i ->
                    part(
                        5_000_000_000 + i * 1_000_000L,
                        5_000_000_000 + (i + 1) * 1_000_000L,
                        if (i % 2 == 0) 0.0 else 100.0,
                    )
                }
        val full = run(prefix)
        val e = BudgetedPowerCurve(id, budget = full.reserved + 100)
        prefix.forEach(e::accept)
        val winner = e.snapshot().durations.first().record
        val before = e.snapshot()
        assertFailsWith<IllegalArgumentException> { e.accept(part(1, 10, 0.0)) }
        assertEquals(before, e.snapshot())
        e.accept(part(6_000_000_000, 3606_000_000_000, 999.0))
        assertTrue(e.limited)
        assertEquals(winner, e.snapshot().durations.first().record)
        assertTrue(e.snapshot().resourceLimited)
    }
}
