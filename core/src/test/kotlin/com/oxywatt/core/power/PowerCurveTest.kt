package com.oxywatt.core.power

import java.util.UUID
import kotlin.test.*
import org.junit.Test

class PowerCurveTest {
    private val session = UUID(0, 1)
    private val epoch = UUID(0, 2)
    private val provenance = PowerProvenance("confirmed-intervals-v1", "synthetic")

    private fun engine(cap: Int = 16384) =
        PowerCurveEngine(session, provenance, PowerCurvePolicy(maxIntervals = cap))

    private fun interval(start: Long, end: Long, watts: Double) =
        PowerInterval(session, epoch, 0, start, end, watts)

    private fun PowerCurveEngine.result(seconds: Int) =
        snapshot().durations.single { it.durationSeconds == seconds }

    @Test
    fun `constant covered power yields only supported durations`() {
        val engine = engine()
        assertNull(engine.result(5).record)
        engine.accept(interval(0, 300_000_000_000, 200.0))
        for (d in PowerCurveEngine.DURATIONS) {
            val result = engine.result(d)
            if (d <= 300) {
                val record = assertNotNull(result.record)
                assertEquals(200.0, record.meanWatts, 1e-8)
                assertEquals(0, record.startNanos)
                assertEquals(d * 1_000_000_000L, record.endNanos)
                assertNull(result.unavailableReason)
            } else
                assertEquals(
                    PowerUnavailableReason.INSUFFICIENT_CONTIGUOUS_COVERAGE,
                    result.unavailableReason,
                )
        }
    }

    @Test
    fun `time weighted ramp and valid zero are not missing`() {
        for (zero in listOf(false, true)) {
            val e = engine()
            repeat(5) { n ->
                e.accept(
                    interval(
                        n * 1_000_000_000L,
                        (n + 1) * 1_000_000_000L,
                        if (zero) 0.0 else n * 100.0,
                    )
                )
            }
            assertEquals(
                if (zero) 0.0 else 200.0,
                assertNotNull(e.result(5).record).meanWatts,
                1e-8,
            )
        }
    }

    @Test
    fun `fractional optimum occurs between arrivals at an expiration boundary`() {
        val e = engine()
        e.accept(interval(0, 1_500_000_000, 0.0))
        e.accept(interval(1_500_000_000, 3_500_000_000, 100.0))
        e.accept(interval(3_500_000_000, 7_500_000_000, 50.0))
        val best = assertNotNull(e.result(5).record)
        assertEquals(70.0, best.meanWatts, 1e-8)
        assertEquals(1_500_000_000, best.startNanos)
        assertEquals(6_500_000_000, best.endNanos)
    }
}
