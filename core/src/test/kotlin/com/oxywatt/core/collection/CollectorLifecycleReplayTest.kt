package com.oxywatt.core.collection

import com.oxywatt.core.collection.simulation.*
import java.time.Instant
import java.util.UUID
import kotlin.test.*
import org.junit.Test

class CollectorLifecycleReplayTest {
    @Test
    fun `simulator speeds preserve exact lifecycle transitions with timers before equal-time input`() {
        val events =
            listOf(
                ScheduledObservation(0, InputPayload.Recording(RecordingState.IDLE)),
                ScheduledObservation(1, InputPayload.Recording(RecordingState.RECORDING)),
                ScheduledObservation(2, InputPayload.MetricReading(Metric.POWER_W, 0.0)),
                ScheduledObservation(3, InputPayload.MetricReading(Metric.HEART_RATE_BPM, 140.0)),
                ScheduledObservation(4, InputPayload.MetricReading(Metric.POWER_W, 100.0)),
                ScheduledObservation(
                    5,
                    InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.DROPOUT),
                ),
                ScheduledObservation(6, InputPayload.Recording(RecordingState.PAUSED)),
                ScheduledObservation(7, InputPayload.MetricReading(Metric.POWER_W, 130.0)),
                ScheduledObservation(8, InputPayload.Recording(RecordingState.RECORDING)),
                ScheduledObservation(9, InputPayload.MetricReading(Metric.POWER_W, 200.0)),
                ScheduledObservation(14, InputPayload.MetricReading(Metric.POWER_W, 300.0)),
                ScheduledObservation(15, InputPayload.Recording(RecordingState.IDLE)),
                ScheduledObservation(16, InputPayload.Recording(RecordingState.RECORDING)),
                ScheduledObservation(17, InputPayload.MetricReading(Metric.POWER_W, 200.0)),
            )
        fun run(speed: Int?): List<LifecycleUpdate> {
            var id = 100L
            val epoch = UUID(0, 1)
            val lifecycle =
                CollectorLifecycle(epoch, LifecyclePolicy("test-v1", 5), { UUID(0, id++) })
            val updates = mutableListOf<LifecycleUpdate>()
            val clock = VirtualTimeline()
            for (time in 0L..20L) clock.at(time) { updates += lifecycle.advanceTime(clock.now) }
            val deadlines = mutableListOf<Long>()
            val source =
                SimulatedRideDataSource(
                    events.asSequence(),
                    clock,
                    epoch,
                    Instant.EPOCH,
                    20,
                    speed = speed,
                    awaitPlaybackDeadline = deadlines::add,
                )
            source
                .start({ updates += lifecycle.accept(it) }) {
                    assertNull(it)
                    updates += lifecycle.sourceStopped()
                }
                .use { while (source.pump()) {} }
            assertEquals(
                (events.map { it.offsetNanos } + 20L).map { if (speed == null) 0L else it / speed },
                deadlines,
            )
            return updates
        }
        val baseline = run(1)
        assertEquals(baseline, run(10))
        assertEquals(baseline, run(null))
        val breaks = baseline.flatMap { it.coverageBreaks }
        assertEquals(
            listOf(
                Triple(Metric.HEART_RATE_BPM, CoverageBreakReason.MISSING_VALUE, 5L),
                Triple(Metric.POWER_W, CoverageBreakReason.PAUSE, 6L),
                Triple(Metric.POWER_W, CoverageBreakReason.SILENCE, 14L),
                Triple(Metric.POWER_W, CoverageBreakReason.SESSION_ENDED, 15L),
                Triple(Metric.POWER_W, CoverageBreakReason.INTERRUPTED, 20L),
            ),
            breaks.map { Triple(it.metric, it.reason, it.detectedAtNanos) },
        )
        val timer = baseline.indexOfFirst {
            it.coverageBreaks.any { b -> b.detectedAtNanos == 14L }
        }
        val input = baseline.indexOfFirst { it.envelope?.stamp?.elapsedNanos == 14L }
        assertTrue(timer < input)
        val ended =
            baseline.flatMap { it.sessionChanges }.filter { it.phase == SessionPhase.ENDED_LOCALLY }
        assertEquals(listOf(UUID(0, 100)), ended.map { it.id })
        assertTrue(ended.single().trendEligible)
        assertEquals(SessionPhase.INTERRUPTED, baseline.last().checkpoint.latestSession?.phase)
        assertEquals(UUID(0, 101), baseline.last().checkpoint.latestSession?.id)
        assertNull(baseline.last().envelope)
    }

    @Test
    fun `generated FIT replay retains FILE_RECORD provenance and EOF stays interrupted`() {
        fun run(speed: Int?): List<LifecycleUpdate> {
            val lifecycle =
                CollectorLifecycle(
                    UUID(0, 101),
                    LifecyclePolicy("fit-test-v1", 5_000_000_000L),
                    { UUID(0, 100) },
                )
            val updates = mutableListOf<LifecycleUpdate>()
            replayFitScenario(java.nio.file.Path.of("src/test/resources/fit-scenario"), speed) {
                updates += lifecycle.accept(it)
            }
            updates += lifecycle.sourceStopped()
            return updates
        }
        val baseline = run(1)
        assertEquals(baseline, run(10))
        assertEquals(baseline, run(null))
        val envelopes = baseline.mapNotNull { it.envelope }
        assertEquals(15, envelopes.size)
        assertTrue(envelopes.all { it.sessionId == UUID(0, 100) })
        assertTrue(
            envelopes
                .map { it.payload }
                .filterIsInstance<InputPayload.MetricReading>()
                .all { it.sourceTime?.meaning == SourceTimeMeaning.FILE_RECORD }
        )
        assertEquals(listOf(0L, 1L), baseline.mapNotNull { it.recordingSegment }.distinct())
        val final = assertNotNull(baseline.last().checkpoint.latestSession)
        assertEquals(SessionOrigin.COLD_ATTACH, final.origin)
        assertEquals(SessionPhase.INTERRUPTED, final.phase)
        assertFalse(final.trendEligible)
        assertTrue(envelopes.none { it.payload == InputPayload.Recording(RecordingState.IDLE) })
    }
}
