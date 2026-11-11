package com.oxywatt.core.collection

import java.time.Instant
import java.util.UUID
import kotlin.test.*
import org.junit.Test

class CollectorLifecycleTest {
    private val epoch = UUID(0, 10)
    private val policy = LifecyclePolicy("coverage-test-v1", 5)
    private var id = 100L

    private fun collector() = CollectorLifecycle(epoch, policy, { UUID(0, id++) })

    private fun input(
        sequence: Long,
        time: Long,
        payload: InputPayload,
        acquisitionEpoch: UUID = epoch,
    ) =
        SourceObservation(
            ReceiptStamp(Instant.EPOCH.plusNanos(time), acquisitionEpoch, time, sequence),
            SourceRef("test", "1"),
            "canonical-v1",
            payload,
        )

    private fun state(sequence: Long, time: Long, state: RecordingState) =
        input(sequence, time, InputPayload.Recording(state))

    private fun power(sequence: Long, time: Long, value: Double = 150.0) =
        input(sequence, time, InputPayload.MetricReading(Metric.POWER_W, value))

    @Test
    fun `initial Idle creates no session and observed start ends locally with exact envelopes`() {
        val collector = collector()
        assertNull(collector.accept(state(0, 0, RecordingState.IDLE)).envelope)
        assertNull(collector.checkpoint().latestSession)
        val start = collector.accept(state(1, 1, RecordingState.RECORDING))
        assertEquals(UUID(0, 100), assertNotNull(start.envelope).sessionId)
        val reading = power(2, 2, 0.0)
        val accepted = collector.accept(reading)
        assertEquals(
            InputEnvelope(
                UUID(0, 100),
                reading.stamp,
                reading.source,
                reading.normalizationVersion,
                reading.payload,
            ),
            accepted.envelope,
        )
        val end = collector.accept(state(3, 3, RecordingState.IDLE))
        val session = assertNotNull(end.checkpoint.latestSession)
        assertEquals(SessionPhase.ENDED_LOCALLY, session.phase)
        assertEquals(SessionOrigin.OBSERVED_START, session.origin)
        assertTrue(session.trendEligible)
        assertEquals(UUID(0, 100), assertNotNull(end.envelope).sessionId)
        assertNull(end.recordingSegment)
        assertNull(collector.accept(state(4, 4, RecordingState.IDLE)).envelope)
    }

    @Test
    fun `two known rides keep repeated readings and receive distinct session IDs`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        for (ride in 0L..1L) {
            val n = 1 + ride * 4
            val start = c.accept(state(n, n, RecordingState.RECORDING))
            val a = c.accept(power(n + 1, n + 1))
            val b = c.accept(power(n + 2, n + 2))
            val end = c.accept(state(n + 3, n + 3, RecordingState.IDLE))
            val expected = UUID(0, 100 + ride)
            assertEquals(
                listOf(expected, expected, expected, expected),
                listOf(start, a, b, end).map { it.envelope?.sessionId },
            )
            assertNotEquals(a.envelope?.stamp, b.envelope?.stamp)
            assertTrue(assertNotNull(end.checkpoint.latestSession).trendEligible)
        }
    }

    @Test
    fun `cold recording and paused attachment remain partial through resume and end`() {
        for (initial in listOf(RecordingState.RECORDING, RecordingState.PAUSED)) {
            val c = collector()
            val first = assertNotNull(c.accept(state(0, 0, initial)).checkpoint.latestSession)
            assertEquals(SessionOrigin.COLD_ATTACH, first.origin)
            assertEquals(first.id, first.ambiguityGroupId)
            val resume =
                assertNotNull(
                    c.accept(state(1, 1, RecordingState.RECORDING)).checkpoint.latestSession
                )
            assertEquals(first.id, resume.id)
            assertEquals(SessionOrigin.COLD_ATTACH, resume.origin)
            assertFalse(
                assertNotNull(c.accept(state(2, 2, RecordingState.IDLE)).checkpoint.latestSession)
                    .trendEligible
            )
        }
    }

    @Test
    fun `pause persists inputs without a recording segment and resume starts another segment`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        assertEquals(0L, c.accept(state(1, 1, RecordingState.RECORDING)).recordingSegment)
        c.accept(power(2, 2))
        assertNull(c.accept(state(3, 3, RecordingState.PAUSED)).recordingSegment)
        val pausedReading = c.accept(power(4, 4))
        assertNotNull(pausedReading.envelope)
        assertNull(pausedReading.recordingSegment)
        c.accept(state(5, 5, RecordingState.PAUSED))
        assertEquals(1L, c.accept(state(6, 6, RecordingState.RECORDING)).recordingSegment)
        assertEquals(1L, c.accept(state(7, 7, RecordingState.RECORDING)).recordingSegment)
    }

    @Test
    fun `source termination interrupts recording or paused sessions without inventing Idle`() {
        for (phase in listOf(RecordingState.RECORDING, RecordingState.PAUSED)) {
            for (failed in listOf(false, true)) {
                val c = collector()
                c.accept(state(0, 0, RecordingState.IDLE))
                c.accept(state(1, 1, RecordingState.RECORDING))
                val prior = assertNotNull(c.accept(state(2, 2, phase)).checkpoint.latestSession)
                val stopped = c.sourceStopped(failed)
                val interrupted = assertNotNull(stopped.checkpoint.latestSession)
                assertEquals(SessionPhase.INTERRUPTED, interrupted.phase)
                assertEquals(
                    if (failed) InterruptionReason.SOURCE_FAILED
                    else InterruptionReason.SOURCE_STOPPED,
                    interrupted.interruptionReason,
                )
                assertFalse(interrupted.trendEligible)
                assertNull(stopped.envelope)
                assertEquals(prior.lastReceipt, interrupted.lastReceipt)
                assertTrue(c.sourceStopped(failed).sessionChanges.isEmpty())
                val fragment =
                    assertNotNull(
                        c.accept(state(3, 3, RecordingState.RECORDING)).checkpoint.latestSession
                    )
                assertNotEquals(prior.id, fragment.id)
                assertEquals(prior.id, fragment.ambiguityGroupId)
                assertEquals(SessionOrigin.RECOVERY_FRAGMENT, fragment.origin)
                assertFalse(
                    assertNotNull(
                            c.accept(state(4, 4, RecordingState.IDLE)).checkpoint.latestSession
                        )
                        .trendEligible
                )
                val fresh = c.accept(state(5, 5, RecordingState.RECORDING))
                assertEquals(SessionOrigin.OBSERVED_START, fresh.checkpoint.latestSession?.origin)
                assertTrue(
                    assertNotNull(
                            c.accept(state(6, 6, RecordingState.IDLE)).checkpoint.latestSession
                        )
                        .trendEligible
                )
            }
        }
    }

    @Test
    fun `unknown host state interrupts rather than closing or resuming the original session`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        val lost = c.accept(state(2, 2, RecordingState.UNKNOWN))
        assertEquals(SessionPhase.INTERRUPTED, lost.checkpoint.latestSession?.phase)
        assertEquals(
            InterruptionReason.UNKNOWN_STATE,
            lost.checkpoint.latestSession?.interruptionReason,
        )
        assertEquals(UUID(0, 100), lost.envelope?.sessionId)
        assertEquals(
            UUID(0, 101),
            c.accept(state(3, 3, RecordingState.RECORDING)).envelope?.sessionId,
        )
    }

    @Test
    fun `channel silence fires at exact deadline even while other channels keep arriving`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        c.accept(power(2, 2, 0.0))
        c.accept(input(3, 5, InputPayload.MetricReading(Metric.HEART_RATE_BPM, 140.0)))
        assertTrue(c.advanceTime(6).coverageBreaks.isEmpty())
        val deadline = c.advanceTime(7)
        assertEquals(
            listOf(
                ChannelCoverageBreak(UUID(0, 100), Metric.POWER_W, CoverageBreakReason.SILENCE, 7)
            ),
            deadline.coverageBreaks,
        )
        assertTrue(c.advanceTime(8).coverageBreaks.isEmpty())
        val hrTimeout = c.advanceTime(10)
        assertEquals(listOf(Metric.HEART_RATE_BPM), hrTimeout.coverageBreaks.map { it.metric })
        assertTrue(c.advanceTime(11).coverageBreaks.isEmpty())
        assertEquals(
            0.0,
            (c.accept(power(4, 12, 0.0)).envelope?.payload as InputPayload.MetricReading).value,
        )
        assertEquals(listOf(Metric.POWER_W), c.advanceTime(17).coverageBreaks.map { it.metric })
        assertEquals(SessionPhase.RECORDING, c.checkpoint().latestSession?.phase)
        assertTrue(
            assertNotNull(c.accept(state(5, 18, RecordingState.IDLE)).checkpoint.latestSession)
                .trendEligible
        )
    }

    @Test
    fun `HR missing breaks HR only and paused samples cannot establish recording continuity`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        c.accept(power(2, 2))
        c.accept(input(3, 2, InputPayload.MetricReading(Metric.HEART_RATE_BPM, 130.0)))
        val missing =
            input(
                4,
                3,
                InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.DROPOUT),
            )
        val loss = c.accept(missing)
        assertEquals(missing.payload, loss.envelope?.payload)
        assertEquals(
            listOf(
                ChannelCoverageBreak(
                    UUID(0, 100),
                    Metric.HEART_RATE_BPM,
                    CoverageBreakReason.MISSING_VALUE,
                    3,
                )
            ),
            loss.coverageBreaks,
        )
        assertTrue(
            c.accept(missing.copy(stamp = missing.stamp.copy(ingestSequence = 5, elapsedNanos = 4)))
                .coverageBreaks
                .isEmpty()
        )
        val pause = c.accept(state(6, 4, RecordingState.PAUSED))
        assertEquals(listOf(Metric.POWER_W), pause.coverageBreaks.map { it.metric })
        c.accept(power(7, 5))
        assertTrue(c.advanceTime(20).coverageBreaks.isEmpty())
        c.accept(state(8, 20, RecordingState.RECORDING))
        assertTrue(c.advanceTime(30).coverageBreaks.isEmpty())
    }

    @Test
    fun `restored active session is interrupted and new epoch creates linked fragment`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        c.accept(power(2, 2))
        val saved = c.checkpoint()
        val nextEpoch = UUID(0, 20)
        val restored = CollectorLifecycle(nextEpoch, policy, { UUID(0, id++) }, saved)
        assertEquals(SessionPhase.INTERRUPTED, restored.checkpoint().latestSession?.phase)
        assertEquals(
            InterruptionReason.PROCESS_RESTART,
            restored.checkpoint().latestSession?.interruptionReason,
        )
        val attach =
            restored.accept(
                input(0, 0, InputPayload.Recording(RecordingState.RECORDING), nextEpoch)
            )
        assertEquals(listOf(UUID(0, 100), UUID(0, 101)), attach.sessionChanges.map { it.id })
        val recovered = assertNotNull(attach.checkpoint.latestSession)
        assertEquals(UUID(0, 100), recovered.ambiguityGroupId)
        assertEquals(SessionOrigin.RECOVERY_FRAGMENT, recovered.origin)
        assertEquals(nextEpoch, recovered.epochId)
        assertFalse(recovered.trendEligible)
        assertEquals(SessionPhase.RECORDING, saved.latestSession?.phase)
        assertEquals(2L, saved.lastReceipt?.ingestSequence)
        assertFailsWith<IllegalArgumentException> {
            CollectorLifecycle(epoch, policy, recovered = saved)
        }
    }

    @Test
    fun `two process deaths preserve ambiguity and restored Idle cannot retroactively end old ride`() {
        var c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        for (e in listOf(UUID(0, 20), UUID(0, 30))) {
            c = CollectorLifecycle(e, policy, { UUID(0, id++) }, c.checkpoint())
            val change = c.accept(input(0, 0, InputPayload.Recording(RecordingState.RECORDING), e))
            assertEquals(UUID(0, 100), change.checkpoint.latestSession?.ambiguityGroupId)
            assertTrue(change.sessionChanges.none { it.trendEligible })
        }
        val fourth = UUID(0, 40)
        c = CollectorLifecycle(fourth, policy, { UUID(0, id++) }, c.checkpoint())
        val idle = c.accept(input(0, 0, InputPayload.Recording(RecordingState.IDLE), fourth))
        assertEquals(SessionPhase.INTERRUPTED, idle.sessionChanges.single().phase)
        assertNull(idle.envelope)
        val start = c.accept(input(1, 1, InputPayload.Recording(RecordingState.RECORDING), fourth))
        assertEquals(SessionOrigin.OBSERVED_START, start.checkpoint.latestSession?.origin)
        val end = c.accept(input(2, 2, InputPayload.Recording(RecordingState.IDLE), fourth))
        assertTrue(assertNotNull(end.checkpoint.latestSession).trendEligible)
    }

    @Test
    fun `outside profile is retained with provenance and never fabricated as another envelope`() {
        val c = collector()
        val profile =
            input(
                0,
                0,
                InputPayload.Profile(
                    RiderProfileSnapshot(
                        "configured-v1",
                        massKg = ProfileSetting(75.0, SettingOrigin.CONFIGURED),
                    )
                ),
            )
        assertNull(c.accept(profile).envelope)
        val original = c.checkpoint()
        val nextEpoch = UUID(0, 20)
        val restored = CollectorLifecycle(nextEpoch, policy, { UUID(0, id++) }, original)
        assertEquals(profile, restored.checkpoint().profileObservation)
        val attach =
            restored.accept(
                input(0, 0, InputPayload.Recording(RecordingState.RECORDING), nextEpoch)
            )
        assertEquals(InputPayload.Recording(RecordingState.RECORDING), attach.envelope?.payload)
        assertEquals(profile, attach.checkpoint.profileObservation)
        assertNull(
            (attach.checkpoint.profileObservation?.payload as InputPayload.Profile)
                .snapshot
                .effectiveAt
        )
    }

    @Test
    fun `wrong epoch duplicate sequence and time reversal reject input and interrupt active session`() {
        val invalid =
            listOf(
                power(2, 2).copy(stamp = power(2, 2).stamp.copy(epochId = UUID(0, 99))) to
                    InputRejection.WRONG_EPOCH,
                power(1, 2) to InputRejection.NON_INCREASING_SEQUENCE,
                power(2, 0) to InputRejection.REGRESSING_TIME,
            )
        for ((observation, reason) in invalid) {
            val c = collector()
            c.accept(state(0, 0, RecordingState.IDLE))
            c.accept(state(1, 1, RecordingState.RECORDING))
            val saved = c.checkpoint()
            val update = c.accept(observation)
            assertNull(update.envelope)
            assertEquals(reason, update.rejection)
            assertEquals(saved.lastReceipt, update.checkpoint.lastReceipt)
            assertEquals(
                InterruptionReason.INVALID_ORDER,
                update.checkpoint.latestSession?.interruptionReason,
            )
            assertEquals(SessionPhase.INTERRUPTED, update.checkpoint.latestSession?.phase)
            val fragment = c.accept(state(3, 3, RecordingState.RECORDING))
            assertEquals(SessionOrigin.RECOVERY_FRAGMENT, fragment.checkpoint.latestSession?.origin)
        }
    }

    @Test
    fun `receipt cannot move behind the advanced clock and UTC reversal leaves ordering intact`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        val original = power(2, 2)
        val shifted =
            original.copy(stamp = original.stamp.copy(utc = Instant.EPOCH.minusSeconds(100)))
        assertEquals(shifted.stamp.utc, c.accept(shifted).envelope?.stamp?.utc)
        c.advanceTime(10)
        assertEquals(InputRejection.REGRESSING_TIME, c.accept(power(3, 9)).rejection)
        assertFailsWith<IllegalArgumentException> { c.advanceTime(9) }
    }

    @Test
    fun `fresh state after transport or control loss stays ambiguous until Idle boundary`() {
        for (reason in
            listOf(
                InterruptionReason.TRANSPORT_LOSS,
                InterruptionReason.CONTROL_OVERFLOW,
                InterruptionReason.STORAGE_FAILURE,
            )) {
            val c = collector()
            c.accept(state(0, 0, RecordingState.IDLE))
            c.accept(state(1, 1, RecordingState.RECORDING))
            c.interrupt(reason)
            val recovered = c.accept(state(2, 2, RecordingState.RECORDING))
            assertEquals(
                SessionOrigin.RECOVERY_FRAGMENT,
                recovered.checkpoint.latestSession?.origin,
            )
            assertFalse(
                assertNotNull(c.accept(state(3, 3, RecordingState.IDLE)).checkpoint.latestSession)
                    .trendEligible
            )
        }
    }

    @Test
    fun `prior idle checkpoint is not trusted after process restart and config versions survive`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        val next = UUID(0, 20)
        val changedPolicy = LifecyclePolicy("coverage-test-v2", 10)
        val restored = CollectorLifecycle(next, changedPolicy, { UUID(0, id++) }, c.checkpoint())
        val attach =
            restored.accept(input(0, 0, InputPayload.Recording(RecordingState.RECORDING), next))
        assertEquals(SessionOrigin.COLD_ATTACH, attach.checkpoint.latestSession?.origin)
        assertEquals(changedPolicy, attach.checkpoint.latestSession?.policy)
        assertEquals(changedPolicy, attach.checkpoint.policy)
        assertEquals("collector-checkpoint-v1", attach.checkpoint.schemaVersion)
        assertFailsWith<IllegalArgumentException> {
            CollectorLifecycle(
                next,
                changedPolicy,
                recovered = c.checkpoint().copy(schemaVersion = "unknown"),
            )
        }
    }

    @Test
    fun `adapter or normalization changes cannot silently merge input sources`() {
        for (replacement in
            listOf(
                power(2, 2).copy(source = SourceRef("different", "1")),
                power(2, 2).copy(normalizationVersion = "canonical-v2"),
            )) {
            val c = collector()
            c.accept(state(0, 0, RecordingState.IDLE))
            c.accept(state(1, 1, RecordingState.RECORDING))
            val changed = c.accept(replacement)
            assertNotNull(changed.rejection)
            assertNull(changed.envelope)
            assertEquals(SessionPhase.INTERRUPTED, changed.checkpoint.latestSession?.phase)
        }
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        c.accept(state(1, 1, RecordingState.RECORDING))
        val retained = c.accept(power(2, 2).copy(source = SourceRef("test", "1", 99)))
        assertNotNull(retained.envelope)
        assertNull(retained.rejection)
    }

    @Test
    fun `Idle from another adapter is not evidence of an observed ride start`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.IDLE))
        val different =
            state(1, 1, RecordingState.RECORDING).copy(source = SourceRef("different", "1"))
        assertEquals(
            SessionOrigin.COLD_ATTACH,
            c.accept(different).checkpoint.latestSession?.origin,
        )
    }

    @Test
    fun `late clock advance reports channel deadlines in acquisition order and only once`() {
        val c = collector()
        c.accept(state(0, 0, RecordingState.RECORDING))
        c.accept(input(1, 1, InputPayload.MetricReading(Metric.HEART_RATE_BPM, 140.0)))
        c.accept(power(2, 2))
        val deadlines = c.advanceTime(20).coverageBreaks
        assertEquals(listOf(Metric.HEART_RATE_BPM, Metric.POWER_W), deadlines.map { it.metric })
        assertEquals(listOf(6L, 7L), deadlines.map { it.detectedAtNanos })
        assertTrue(c.advanceTime(21).coverageBreaks.isEmpty())
    }

    @Test
    fun `failed session ID allocation leaves checkpoint unchanged and input can be retried`() {
        var fail = true
        val c =
            CollectorLifecycle(
                epoch,
                policy,
                {
                    check(!fail) { "Test ID allocation failed" }
                    UUID(0, 100)
                },
            )
        c.accept(state(0, 0, RecordingState.IDLE))
        val before = c.checkpoint()
        assertFailsWith<IllegalStateException> { c.accept(state(1, 1, RecordingState.RECORDING)) }
        assertEquals(before, c.checkpoint())
        fail = false
        val retry = c.accept(state(1, 1, RecordingState.RECORDING))
        assertNull(retry.rejection)
        assertEquals(SessionOrigin.OBSERVED_START, retry.checkpoint.latestSession?.origin)
    }

    @Test
    fun `recovery cannot reopen the ambiguity root ID and failed allocation retains recovery effects`() {
        val original = collector()
        original.accept(state(0, 0, RecordingState.RECORDING))
        val secondEpoch = UUID(0, 20)
        val second =
            CollectorLifecycle(secondEpoch, policy, { UUID(0, 101) }, original.checkpoint())
        second.accept(input(0, 0, InputPayload.Recording(RecordingState.RECORDING), secondEpoch))
        val thirdEpoch = UUID(0, 30)
        var collide = true
        val third =
            CollectorLifecycle(
                thirdEpoch,
                policy,
                { UUID(0, if (collide) 100 else 102) },
                second.checkpoint(),
            )
        val before = third.checkpoint()
        val attach = input(0, 0, InputPayload.Recording(RecordingState.RECORDING), thirdEpoch)
        assertFailsWith<IllegalArgumentException> { third.accept(attach) }
        assertEquals(before, third.checkpoint())
        collide = false
        val retry = third.accept(attach)
        assertNull(retry.rejection)
        assertEquals(listOf(UUID(0, 101), UUID(0, 102)), retry.sessionChanges.map { it.id })
        assertEquals(UUID(0, 100), retry.checkpoint.latestSession?.ambiguityGroupId)
    }
}
