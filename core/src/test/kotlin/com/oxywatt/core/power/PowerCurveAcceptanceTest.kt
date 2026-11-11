package com.oxywatt.core.power

import com.oxywatt.core.collection.simulation.VirtualTimeline
import com.oxywatt.core.collection.simulation.playbackDeadline
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*
import org.junit.Test

class PowerCurveAcceptanceTest {
    private val session = UUID(0, 11)
    private val epoch = UUID(0, 12)
    private val provenance = PowerProvenance("confirmed-v1", "synthetic-interval-table-v1")

    private fun engine(cap: Int = 16384) =
        PowerCurveEngine(session, provenance, PowerCurvePolicy(maxIntervals = cap))

    private fun part(a: Long, b: Long, w: Double) = PowerInterval(session, epoch, 0, a, b, w)

    private fun PowerCurveEngine.best(d: Int = 5) =
        snapshot().durations.single { it.durationSeconds == d }.record

    @Test
    fun `gaps explicit breaks segments and epochs never concatenate short coverage`() {
        for (mode in 0..3) {
            val e = engine()
            e.accept(part(0, 3_000_000_000, 100.0))
            val second =
                when (mode) {
                    0 -> part(4_000_000_000, 7_000_000_000, 200.0)
                    1 -> {
                        e.breakCoverage()
                        part(3_000_000_000, 6_000_000_000, 200.0)
                    }
                    2 -> part(3_000_000_000, 6_000_000_000, 200.0).copy(segmentIndex = 1)
                    else -> part(0, 3_000_000_000, 200.0).copy(epochId = UUID(0, 99))
                }
            e.accept(second)
            assertNull(e.best(), "mode=$mode")
        }
    }

    @Test
    fun `break preserves winner and sessions remain independently attributed`() {
        val e = engine()
        e.accept(part(0, 5_000_000_000, 210.0))
        val winner = e.best()
        e.breakCoverage()
        e.accept(part(5_000_000_000, 8_000_000_000, 999.0))
        assertEquals(winner, e.best())
        assertEquals(session, assertNotNull(winner).sessionId)
        val other = PowerCurveEngine(UUID(0, 15), provenance)
        assertNull(other.best())
    }

    @Test
    fun `invalid intervals leave snapshot unchanged and corrected input can retry`() {
        val e = engine()
        e.accept(part(0, 5_000_000_000, 100.0))
        val before = e.snapshot()
        val valid = part(5_000_000_000, 10_000_000_000, 200.0)
        for (bad in
            listOf(
                valid.copy(sessionId = UUID(0, 100)),
                valid.copy(segmentIndex = -1),
                valid.copy(startNanos = -1),
                valid.copy(endNanos = valid.startNanos),
                valid.copy(startNanos = 4_000_000_000),
                valid.copy(watts = -1.0),
                valid.copy(watts = Double.NaN),
                valid.copy(watts = Double.POSITIVE_INFINITY),
            )) {
            assertFailsWith<IllegalArgumentException> { e.accept(bad) }
            assertEquals(before, e.snapshot())
        }
        e.breakCoverage()
        assertFailsWith<IllegalArgumentException> { e.accept(valid.copy(startNanos = 1)) }
        e.accept(valid)
        assertEquals(200.0, assertNotNull(e.best()).meanWatts, 1e-8)
        e.accept(
            valid.copy(startNanos = 10_000_000_000, endNanos = 11_000_000_000, segmentIndex = 2)
        )
        val next = e.snapshot()
        assertFailsWith<IllegalArgumentException> {
            e.accept(
                valid.copy(startNanos = 11_000_000_000, endNanos = 12_000_000_000, segmentIndex = 1)
            )
        }
        assertEquals(next, e.snapshot())
    }

    @Test
    fun `exact endpoint earliest tie and large monotonic origins remain exact`() {
        val e = engine()
        val start = Long.MAX_VALUE - 10_000_000_000
        e.accept(part(start, start + 5_000_000_000, 200.0))
        e.accept(part(start + 5_000_000_000, Long.MAX_VALUE, 200.0))
        assertEquals(start, assertNotNull(e.best()).startNanos)
        assertEquals(start + 5_000_000_000, assertNotNull(e.best()).endNanos)
        assertEquals(1, e.snapshot().retainedIntervals)
        e.breakCoverage()
        e.accept(part(0, 5_000_000_000, 200.0).copy(epochId = UUID(0, 50)))
        assertEquals(epoch, assertNotNull(e.best()).epochId)
    }

    @Test
    fun `resource saturation preserves numeric winner and reports incomplete scan`() {
        val e = engine(2)
        e.accept(part(0, 5_000_000_000, 200.0))
        e.accept(part(5_000_000_000, 6_000_000_000, 0.0))
        val winner = e.best()
        e.accept(part(6_000_000_000, 7_000_000_000, 400.0))
        assertTrue(e.snapshot().resourceLimited)
        assertEquals(1, e.snapshot().retainedIntervals)
        assertEquals(winner, e.best())
        assertEquals(
            PowerUnavailableReason.RESOURCE_LIMIT,
            e.snapshot().durations.single { it.durationSeconds == 30 }.unavailableReason,
        )
        repeat(2000) { n ->
            e.accept(
                part(
                    (7L + n) * 1_000_000_000,
                    (8L + n) * 1_000_000_000,
                    if (n % 2 == 0) 0.0 else 400.0,
                )
            )
            assertTrue(e.snapshot().retainedIntervals <= 2)
            assertTrue(e.retainedPeak <= 2)
            assertTrue(e.stagedNodeBound <= 10)
            assertEquals(winner, e.best())
        }
    }

    @Test
    fun `coalescing avoids unnecessary saturation and maximum horizon evicts old fragments`() {
        val equal = engine(1)
        repeat(4000) { n ->
            equal.accept(part(n * 1_000_000_000L, (n + 1) * 1_000_000_000L, 200.0))
        }
        assertFalse(equal.snapshot().resourceLimited)
        assertEquals(1, equal.snapshot().retainedIntervals)
        for (r in equal.snapshot().durations) assertEquals(
            200.0,
            assertNotNull(r.record).meanWatts,
            1e-8,
        )
        val bounded = engine()
        repeat(14400) { n ->
            bounded.accept(
                part(n * 1_000_000_000L, (n + 1) * 1_000_000_000L, if (n % 2 == 0) 100.0 else 200.0)
            )
            assertTrue(bounded.snapshot().retainedIntervals <= 3600)
        }
        assertEquals(3600, bounded.snapshot().retainedIntervals)
        assertFalse(bounded.snapshot().resourceLimited)
        for (r in bounded.snapshot().durations) assertNotNull(r.record)
        println(
            "T04a four-hour counters: windows=${bounded.snapshot().evaluatedWindows}, fragments=${bounded.snapshot().integratedFragments}, retained=${bounded.snapshot().retainedIntervals}"
        )
    }

    @Test
    fun `delivery speeds preserve acquisition interval outcomes without sleeping`() {
        val table =
            listOf(
                part(0, 1_500_000_000, 0.0),
                part(1_500_000_000, 3_500_000_000, 100.0),
                part(3_500_000_000, 7_500_000_000, 50.0),
            )
        val results =
            listOf(1, 10, null).map { speed ->
                val e = engine()
                val clock = VirtualTimeline()
                val deliveryClock = VirtualTimeline()
                for (p in table) {
                    val deadline = playbackDeadline(p.endNanos, speed)
                    deliveryClock.advanceTo(deadline)
                    clock.advanceTo(p.endNanos)
                    e.accept(p)
                }
                e.snapshot()
            }
        assertEquals(results[0], results[1])
        assertEquals(results[0], results[2])
    }

    @Test
    fun `irregular interval maxima agree with an independent exact quarter second grid oracle`() {
        // Every interval boundary lies on this exact grid; the continuous optimum does too.
        // Integer cell energy prefix sums are independent of production boundary enumeration.
        for (seed in 0..5) {
            val random = Random(seed)
            val ticks = if (seed == 0) 16000 else 400
            val cells = IntArray(ticks)
            val e = engine()
            var at = 0
            while (at < ticks) {
                val end = minOf(ticks, at + random.nextInt(1, 12))
                val watts = random.nextInt(0, 501)
                for (i in at until end) cells[i] = watts
                e.accept(part(at * 250_000_000L, end * 250_000_000L, watts.toDouble()))
                at = end
            }
            val sums = LongArray(ticks + 1)
            for (i in cells.indices) sums[i + 1] = sums[i] + cells[i]
            for (duration in PowerCurveEngine.DURATIONS) {
                val width = duration * 4
                if (width > ticks) {
                    assertNull(e.best(duration))
                    continue
                }
                var bestSum = -1L
                var firstStart = -1
                for (start in 0..ticks - width) {
                    val sum = sums[start + width] - sums[start]
                    if (sum > bestSum) {
                        bestSum = sum
                        firstStart = start
                    }
                }
                val actual = assertNotNull(e.best(duration))
                assertEquals(
                    bestSum.toDouble() / width,
                    actual.meanWatts,
                    maxOf(1e-8, 1e-12 * actual.meanWatts),
                    "seed=$seed d=$duration",
                )
                assertEquals(firstStart * 250_000_000L, actual.startNanos, "seed=$seed d=$duration")
            }
        }
    }

    @Test
    fun `extreme finite watts need no overflowing energy prefix`() {
        val e = engine()
        e.accept(part(0, 3600_000_000_000, 1e308))
        for (r in e.snapshot().durations) assertTrue(assertNotNull(r.record).meanWatts.isFinite())
        e.breakCoverage()
        e.accept(part(3600_000_000_000, 3605_000_000_000, 2.0))
        assertEquals(1e308, assertNotNull(e.best()).meanWatts)
    }

    @Test
    fun `callers cannot mutate the globally configured duration contract`() {
        val original = PowerCurveEngine.DURATIONS[0]
        try {
            assertFailsWith<UnsupportedOperationException> {
                (PowerCurveEngine.DURATIONS as MutableList<Int>)[0] = 1
            }
        } finally {
            if (PowerCurveEngine.DURATIONS[0] != original)
                (PowerCurveEngine.DURATIONS as MutableList<Int>)[0] = original
        }
    }

    @Test
    fun `published snapshots remain immutable after later ingestion`() {
        val e = engine()
        e.accept(part(0, 5_000_000_000, 100.0))
        val snapshot = e.snapshot()
        assertFailsWith<UnsupportedOperationException> {
            (snapshot.durations as MutableList<PowerDurationResult>).clear()
        }
        e.accept(part(5_000_000_000, 10_000_000_000, 200.0))
        assertEquals(100.0, assertNotNull(snapshot.durations.first().record).meanWatts)
        assertEquals(200.0, assertNotNull(e.best()).meanWatts)
    }
}
