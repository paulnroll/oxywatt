package com.oxywatt.core.power

import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID
import kotlin.test.*
import org.junit.Test

class CanonicalPowerReplayTest {
    private val id = UUID(0, 41)
    private val epoch = UUID(0, 42)

    private fun input(t: Long, seq: Long, payload: InputPayload) =
        SourceObservation(
            ReceiptStamp(Instant.EPOCH, epoch, t, seq),
            SourceRef("simulated", "1"),
            "canonical-v1",
            payload,
        )

    private fun recording(s: RecordingState) = InputPayload.Recording(s)

    private fun power(w: Double?) =
        InputPayload.MetricReading(
            Metric.POWER_W,
            w,
            if (w == null) MissingReason.DROPOUT else null,
        )

    private fun lifecycle(ttl: Long = 2_000_000_000) =
        CollectorLifecycle(epoch, LifecyclePolicy("preview-silence-v1", ttl), { id })

    @Test
    fun `live irregular integral has independent seventy watt five second winner`() {
        val l =
            CollectorLifecycle(
                epoch,
                LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
                { id },
            )
        val engine =
            BudgetedPowerCurve(id, coverageOrigin = "committed-real-karoo-diagnostic-effects-v1")
        val replay = CanonicalPowerReplay(id, engine::accept)
        listOf(
                input(0, 0, recording(RecordingState.RECORDING)),
                input(0, 1, power(0.0)),
                input(1_500_000_000, 2, power(100.0)),
                input(3_500_000_000, 3, power(50.0)),
                input(7_500_000_000, 4, recording(RecordingState.IDLE)),
            )
            .forEach { replay.accept(l.accept(it)) }
        // [1.5,6.5): 2*100 + 3*50 = 350 joules over five seconds.
        val record = assertNotNull(engine.snapshot().durations.first().record)
        assertEquals(70.0, record.meanWatts)
        assertEquals(1_500_000_000, record.startNanos)
    }

    @Test
    fun `unknown live timeout and policy cannot silently become preview holds`() {
        for (policy in
            listOf(
                LifecyclePolicy("karoo-diagnostic-silence-v1", 4_000_000_000),
                LifecyclePolicy("karoo-diagnostic-silence-v2", 5_000_000_000),
            )) {
            val l = CollectorLifecycle(epoch, policy, { id })
            assertFailsWith<IllegalArgumentException> {
                CanonicalPowerReplay(id) {}
                    .accept(l.accept(input(0, 0, recording(RecordingState.RECORDING))))
            }
        }
    }

    @Test
    fun `live five second policy expires before equal receipt without extending the last value`() {
        val l =
            CollectorLifecycle(
                epoch,
                LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
                { id },
            )
        val parts = mutableListOf<PowerInterval?>()
        val replay = CanonicalPowerReplay(id) { parts.add(it) }
        replay.accept(l.accept(input(0, 0, recording(RecordingState.IDLE))))
        replay.accept(l.accept(input(0, 1, recording(RecordingState.RECORDING))))
        replay.accept(l.accept(input(0, 2, power(0.0))))
        assertTrue(parts.filterNotNull().isEmpty())
        replay.accept(l.accept(input(5_000_000_000, 3, power(200.0))))
        assertEquals(
            listOf(0L to 5_000_000_000L),
            parts.filterNotNull().map { it.startNanos to it.endNanos },
        )
        assertEquals(0.0, parts.filterNotNull().single().watts)
        assertTrue(parts.any { it == null })
        replay.accept(l.accept(input(6_000_000_000, 4, recording(RecordingState.PAUSED))))
        replay.accept(l.accept(input(7_000_000_000, 5, power(999.0))))
        assertEquals(
            listOf(0L to 5_000_000_000L, 5_000_000_000L to 6_000_000_000L),
            parts.filterNotNull().map { it.startNanos to it.endNanos },
        )
    }

    @Test
    fun `last value needs committed evidence and repeated readings confirm five seconds`() {
        val l = lifecycle()
        val intervals = mutableListOf<PowerInterval>()
        val replay = CanonicalPowerReplay(id) { it?.let(intervals::add) }
        replay.accept(l.accept(input(0, 0, recording(RecordingState.IDLE))))
        replay.accept(l.accept(input(0, 1, recording(RecordingState.RECORDING))))
        replay.accept(l.accept(input(0, 2, power(200.0))))
        assertTrue(intervals.isEmpty())
        for (s in 1L..5L) replay.accept(l.accept(input(s * 1_000_000_000, s + 2, power(200.0))))
        assertEquals(5_000_000_000, intervals.sumOf { it.endNanos - it.startNanos })
        val engine = PowerCurveEngine(id, PowerProvenance("canonical-power-hold-v1", "synthetic"))
        intervals.forEach(engine::accept)
        assertEquals(200.0, engine.snapshot().durations.first().record?.meanWatts)
    }

    @Test
    fun `expiry before equal time observation breaks adjacent coverage and zero is present`() {
        val l = lifecycle(3_000_000_000)
        val parts = mutableListOf<PowerInterval?>()
        val engine = BudgetedPowerCurve(id)
        val replay =
            CanonicalPowerReplay(id) {
                parts.add(it)
                engine.accept(it)
            }
        replay.accept(l.accept(input(0, 0, recording(RecordingState.RECORDING))))
        replay.accept(l.accept(input(0, 1, power(0.0))))
        replay.accept(l.accept(input(3_000_000_000, 2, power(0.0))))
        replay.accept(l.accept(input(6_000_000_000, 3, power(0.0))))
        assertEquals(
            listOf(0L to 3_000_000_000L, 3_000_000_000L to 6_000_000_000L),
            parts.filterNotNull().map { it.startNanos to it.endNanos },
        )
        assertNull(engine.snapshot().durations.first().record)
        assertTrue(parts.any { it == null })
    }

    @Test
    fun `HR horizon confirms power without refreshing TTL and missing expiry fails`() {
        val l = lifecycle()
        val parts = mutableListOf<PowerInterval?>()
        val replay = CanonicalPowerReplay(id) { parts.add(it) }
        replay.accept(l.accept(input(0, 0, recording(RecordingState.RECORDING))))
        replay.accept(l.accept(input(0, 1, power(100.0))))
        replay.accept(
            l.accept(
                input(
                    1_000_000_000,
                    2,
                    InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.DROPOUT),
                )
            )
        )
        val expired = l.advanceTime(4_000_000_000)
        assertFailsWith<IllegalArgumentException> {
            replay.accept(
                expired.copy(
                    coverageBreaks = emptyList(),
                    checkpoint =
                        expired.checkpoint.copy(
                            lastReceipt =
                                expired.checkpoint.lastReceipt!!.copy(elapsedNanos = 4_000_000_000)
                        ),
                )
            )
        }
        replay.accept(expired)
        assertEquals(2_000_000_000, parts.filterNotNull().sumOf { it.endNanos - it.startNanos })
    }

    @Test
    fun `pause missing stop and recovery never join unknown time`() {
        for (mode in 0..3) {
            val l = lifecycle(10_000_000_000)
            val parts = mutableListOf<PowerInterval?>()
            val e = BudgetedPowerCurve(id)
            val r =
                CanonicalPowerReplay(id) {
                    parts.add(it)
                    e.accept(it)
                }
            r.accept(l.accept(input(0, 0, recording(RecordingState.RECORDING))))
            r.accept(l.accept(input(0, 1, power(300.0))))
            r.accept(
                l.accept(
                    input(3_000_000_000, 2, InputPayload.MetricReading(Metric.CADENCE_RPM, 90.0))
                )
            )
            when (mode) {
                0 -> {
                    r.accept(l.accept(input(3_000_000_000, 3, recording(RecordingState.PAUSED))))
                    r.accept(l.accept(input(8_000_000_000, 4, power(999.0))))
                    r.accept(l.accept(input(9_000_000_000, 5, recording(RecordingState.RECORDING))))
                    r.accept(l.accept(input(9_000_000_000, 6, power(300.0))))
                    r.accept(l.accept(input(12_000_000_000, 7, recording(RecordingState.IDLE))))
                }
                1 -> {
                    r.accept(l.accept(input(3_000_000_000, 3, power(null))))
                    r.accept(l.accept(input(9_000_000_000, 4, power(300.0))))
                    r.accept(l.accept(input(12_000_000_000, 5, recording(RecordingState.IDLE))))
                }
                2 -> r.accept(l.sourceStopped())
                3 -> {
                    val newer =
                        CollectorLifecycle(
                            UUID(0, 99),
                            LifecyclePolicy("preview-silence-v1", 10_000_000_000),
                            recovered = l.checkpoint(),
                        )
                    r.accept(newer.advanceTime(100_000_000_000))
                    assertEquals(InterruptionReason.PROCESS_RESTART, r.session?.interruptionReason)
                }
            }
            assertNull(e.snapshot().durations.first().record, "mode=$mode")
            assertEquals(
                if (mode < 2) 6_000_000_000 else 3_000_000_000,
                parts.filterNotNull().sumOf { it.endNanos - it.startNanos },
            )
        }
    }

    @Test
    fun `fractional independent integral and every constant duration survive canonical preprocessing`() {
        val l = lifecycle(10_000_000_000)
        val e = BudgetedPowerCurve(id)
        val r = CanonicalPowerReplay(id, e::accept)
        listOf(
                input(0, 0, recording(RecordingState.RECORDING)),
                input(0, 1, power(0.0)),
                input(1_500_000_000, 2, power(100.0)),
                input(3_500_000_000, 3, power(50.0)),
                input(7_500_000_000, 4, recording(RecordingState.IDLE)),
            )
            .forEach { r.accept(l.accept(it)) }
        assertEquals(70.0, e.snapshot().durations.first().record?.meanWatts)
        assertEquals(1_500_000_000, e.snapshot().durations.first().record?.startNanos)
        val constant = lifecycle()
        val all = BudgetedPowerCurve(id)
        val replay = CanonicalPowerReplay(id, all::accept)
        replay.accept(constant.accept(input(0, 0, recording(RecordingState.RECORDING))))
        for (t in 0L..3600) replay.accept(
            constant.accept(input(t * 1_000_000_000, t + 1, power(200.0)))
        )
        all.snapshot().durations.forEach {
            assertEquals(200.0, assertNotNull(it.record).meanWatts, 1e-8)
        }
        assertTrue(all.snapshot().integratedFragments <= all.reserved)
    }

    @Test
    fun `loss boundary closes held coverage and never produces a crossing winner`() {
        val l = lifecycle(10_000_000_000)
        val parts = mutableListOf<PowerInterval?>()
        val r = CanonicalPowerReplay(id) { parts.add(it) }
        r.accept(l.accept(input(0, 0, recording(RecordingState.RECORDING))))
        r.accept(l.accept(input(0, 1, power(500.0))))
        val stamp = input(2_000_000_000, 2, power(100.0)).stamp
        val loss = AdmissionLoss(stamp, stamp, 1, setOf(AdmissionLossReason.CAPACITY))
        r.accept(l.accept(input(6_000_000_000, 3, power(500.0))), loss)
        assertEquals(2_000_000_000, parts.filterNotNull().sumOf { it.endNanos - it.startNanos })
    }

    @Test
    fun `budget stops dense long append and four hour state stays bounded`() {
        val e = BudgetedPowerCurve(id, budget = 100)
        for (t in 0L..300) {
            e.accept(
                PowerInterval(
                    id,
                    epoch,
                    0,
                    t * 1_000_000,
                    (t + 1) * 1_000_000,
                    if (t % 2 == 0L) 100.0 else 200.0,
                )
            )
            assertTrue(e.snapshot().integratedFragments <= e.reserved)
        }
        assertTrue(e.limited)
        val before = e.snapshot()
        e.accept(PowerInterval(id, epoch, 0, 301_000_000, 3600_000_000_000, 200.0))
        assertEquals(before, e.snapshot())
        for (alternating in listOf(false, true)) {
            val long = BudgetedPowerCurve(id)
            val began = System.nanoTime()
            repeat(14400) { t ->
                long.accept(
                    PowerInterval(
                        id,
                        epoch,
                        0,
                        t * 1_000_000_000L,
                        (t + 1) * 1_000_000_000L,
                        if (alternating && t % 2 == 0) 100.0 else 200.0,
                    )
                )
            }
            assertFalse(long.limited)
            assertTrue(long.snapshot().integratedFragments <= long.reserved)
            assertTrue(long.snapshot().retainedIntervals <= 16384)
            println(
                "T04b four-hour alternating=$alternating reserved=${long.reserved} actual=${long.snapshot().integratedFragments} retained=${long.snapshot().retainedIntervals} elapsedMs=${(System.nanoTime()-began)/1e6}"
            )
        }
    }

    @Test
    fun `irregular canonical stream matches independent integer cell oracle despite UTC jumps`() {
        val random = kotlin.random.Random(19)
        val cells = IntArray(160)
        val events = mutableListOf<Pair<Long, Double>>()
        var at = 0
        while (at < cells.size) {
            val end = minOf(cells.size, at + random.nextInt(1, 12))
            val watts = random.nextInt(0, 501)
            for (i in at until end) cells[i] = watts
            events.add(at * 250_000_000L to watts.toDouble())
            at = end
        }
        for (jump in listOf(false, true)) {
            val l = lifecycle(10_000_000_000)
            val e = BudgetedPowerCurve(id)
            val replay = CanonicalPowerReplay(id, e::accept)
            replay.accept(l.accept(input(0, 0, recording(RecordingState.RECORDING))))
            events.forEachIndexed { index, (t, w) ->
                val original = input(t, index + 1L, power(w))
                replay.accept(
                    l.accept(
                        if (jump)
                            original.copy(
                                stamp =
                                    original.stamp.copy(
                                        utc = Instant.EPOCH.minusSeconds(index.toLong())
                                    )
                            )
                        else original
                    )
                )
            }
            replay.accept(
                l.accept(input(40_000_000_000, events.size + 1L, recording(RecordingState.IDLE)))
            )
            for (d in e.snapshot().durations) {
                val width = d.durationSeconds * 4
                if (width > cells.size) {
                    assertNull(d.record)
                    continue
                }
                // Sum integer cell energies directly; no production candidate-boundary search.
                val sums =
                    (0..cells.size - width).map { start ->
                        (start until start + width).sumOf { cells[it].toLong() }
                    }
                val maximum = sums.max()
                assertEquals(maximum.toDouble() / width, assertNotNull(d.record).meanWatts, 1e-8)
                assertEquals(sums.indexOf(maximum) * 250_000_000L, d.record!!.startNanos)
            }
        }
    }
}
