package com.oxywatt.core.collection

import java.time.Instant
import java.util.UUID
import kotlin.test.*
import org.junit.Test

class BoundedAdmissionTest {
    private val epoch = UUID(0, 20)

    private fun input(
        n: Long,
        payload: InputPayload = InputPayload.MetricReading(Metric.POWER_W, 0.0),
    ) =
        SourceObservation(
            ReceiptStamp(Instant.EPOCH, epoch, n, n),
            SourceRef("test", "1"),
            "v1",
            payload,
        )

    @Test
    fun `telemetry capacity rejects incoming data without using control reserve`() {
        val admission = BoundedAdmission()
        repeat(4032) { assertTrue(admission.offer(input(it.toLong()))) }
        assertFalse(admission.offer(input(4032)))
    }

    private fun small(bytes: Int = 2000) =
        BoundedAdmission(AdmissionPolicy("test-v1", 4, bytes, 1, 200, 1024, 5))

    private fun state(n: Long, state: RecordingState) = input(n, InputPayload.Recording(state))

    @Test
    fun `in flight writes retain both budgets and failed writes can retry unchanged`() {
        val admission = small()
        repeat(3) { assertTrue(admission.offer(input(it.toLong()))) }
        val batch = assertNotNull(admission.takeBatch())
        val bytes = admission.pendingBytes
        assertNull(admission.takeBatch())
        assertFalse(admission.offer(input(3)))
        assertEquals(3, admission.pendingCount)
        assertEquals(bytes, admission.pendingBytes)
        assertTrue(admission.offer(state(4, RecordingState.PAUSED)))
        assertFalse(admission.offer(state(5, RecordingState.IDLE)))
        assertTrue(admission.stopped)
        assertFalse(admission.offer(input(6)))
        admission.committed(batch)
        assertEquals(1, admission.pendingCount)
        assertFailsWith<IllegalArgumentException> { admission.committed(batch) }
        val tail = assertNotNull(admission.takeBatch())
        assertEquals(listOf(4L), tail.observations.map { it.stamp.ingestSequence })
        assertEquals(2, tail.loss!!.count)
        assertEquals(InterruptionReason.CONTROL_OVERFLOW, tail.terminal)
        admission.committed(tail)
        assertEquals(0, admission.pendingBytes)
    }

    @Test
    fun `payload ceiling and oversize domain strings reject explicitly`() {
        val admission = small(700)
        assertTrue(admission.offer(input(0)))
        assertTrue(admission.offer(input(1)))
        assertFalse(admission.offer(input(2).copy(source = SourceRef("é".repeat(400), "1"))))
        assertFalse(admission.offer(input(3).copy(normalizationVersion = "x".repeat(2000))))
        val batch = assertNotNull(admission.takeBatch())
        assertEquals(2, batch.loss!!.count)
        assertTrue(AdmissionLossReason.OVERSIZE in batch.loss.reasons)
        assertTrue(admission.pendingBytes <= 700)
    }

    @Test
    fun `silence reaches exact flush deadline and control retains FIFO order`() {
        val admission = small()
        assertTrue(admission.offer(input(0)))
        assertFalse(admission.flushDue(4))
        assertTrue(admission.flushDue(5))
        assertTrue(admission.offer(state(5, RecordingState.RECORDING)))
        assertEquals(
            listOf(0L, 5L),
            admission.takeBatch()!!.observations.map { it.stamp.ingestSequence },
        )
        assertTrue(admission.flushDue(6))
    }

    @Test
    fun `loss interrupts before later controls and never bridges analytics continuity`() {
        val admission = small()
        val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test", 100))
        admission.offer(state(0, RecordingState.IDLE))
        admission.offer(state(1, RecordingState.RECORDING))
        admission.offer(input(2))
        admission.offer(input(3))
        admission.offer(input(4))
        assertFalse(admission.offer(input(5)))
        val first = assertNotNull(admission.takeBatch())
        val effects = first.applyTo(lifecycle)
        assertEquals(SessionPhase.INTERRUPTED, effects.last().checkpoint.latestSession!!.phase)
        assertFalse(effects.last().checkpoint.latestSession!!.trendEligible)
        admission.committed(first)
        admission.offer(state(6, RecordingState.RECORDING))
        val next = assertNotNull(admission.takeBatch()).applyTo(lifecycle).last()
        assertEquals(SessionOrigin.RECOVERY_FRAGMENT, next.checkpoint.latestSession!!.origin)
    }

    @Test
    fun `stalled storage keeps loss bounded through a long burst`() {
        val admission = small()
        repeat(3) { admission.offer(input(it.toLong())) }
        val batch = assertNotNull(admission.takeBatch())
        repeat(100000) { assertFalse(admission.offer(input(it + 3L))) }
        assertEquals(3, admission.pendingCount)
        admission.committed(batch)
        val lossOnly = assertNotNull(admission.takeBatch())
        assertTrue(lossOnly.observations.isEmpty())
        assertEquals(100000, lossOnly.loss!!.count)
        assertEquals(3, lossOnly.loss.first.ingestSequence)
        assertEquals(100002, lossOnly.loss.last.ingestSequence)
        assertEquals(setOf(AdmissionLossReason.CAPACITY), lossOnly.loss.reasons)
    }

    @Test
    fun `invalid order latches terminal and requires a new acquisition epoch`() {
        val admission = small()
        admission.offer(state(1, RecordingState.RECORDING))
        assertFalse(admission.offer(input(1)))
        assertTrue(admission.stopped)
        val batch = assertNotNull(admission.takeBatch())
        assertEquals(InterruptionReason.INVALID_ORDER, batch.terminal)
    }

    @Test
    fun `fully reserved empty admission does not request perpetual flush`() {
        val admission = BoundedAdmission(AdmissionPolicy("control-only", 1, 1024, 1, 1024))
        assertFalse(admission.flushDue(0))
    }

    @Test
    fun `batch cannot apply lifecycle transitions twice during write retry`() {
        val admission = small()
        admission.offer(state(0, RecordingState.RECORDING))
        val batch = assertNotNull(admission.takeBatch())
        val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test", 100))
        batch.applyTo(lifecycle)
        assertFailsWith<IllegalStateException> { batch.applyTo(lifecycle) }
    }

    @Test
    fun `lost telemetry before Idle cannot complete the old session`() {
        val admission = small()
        val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test", 100))
        admission.offer(state(0, RecordingState.IDLE))
        admission.offer(state(1, RecordingState.RECORDING))
        val start = assertNotNull(admission.takeBatch())
        start.applyTo(lifecycle)
        admission.committed(start)
        repeat(3) { admission.offer(input(it + 2L)) }
        assertFalse(admission.offer(input(5)))
        assertTrue(admission.offer(state(6, RecordingState.IDLE)))
        val updates = assertNotNull(admission.takeBatch()).applyTo(lifecycle)
        assertEquals(SessionPhase.INTERRUPTED, updates.last().checkpoint.latestSession!!.phase)
        assertEquals(listOf(2L, 3L, 4L), updates.mapNotNull { it.envelope?.stamp?.ingestSequence })
    }

    @Test
    fun `profile reserve and hard byte ceiling include outstanding controls`() {
        val admission = small(700)
        admission.offer(input(0))
        admission.offer(input(1))
        assertFalse(admission.offer(input(2)))
        assertTrue(
            admission.offer(input(3, InputPayload.Profile(RiderProfileSnapshot("profile-v1"))))
        )
        val batch = assertNotNull(admission.takeBatch())
        assertTrue(admission.pendingBytes <= 700)
        assertFalse(admission.offer(state(4, RecordingState.RECORDING)))
        assertTrue(admission.stopped)
        assertEquals(listOf(0L, 1L, 3L), batch.observations.map { it.stamp.ingestSequence })
    }

    @Test
    fun `coalesced losses surrounding a new fragment interrupt that fragment too`() {
        val admission = small()
        val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test", 100))
        repeat(3) { admission.offer(input(it.toLong())) }
        admission.offer(input(3))
        admission.offer(state(4, RecordingState.RECORDING))
        admission.offer(input(5))
        val updates = assertNotNull(admission.takeBatch()).applyTo(lifecycle)
        assertEquals(SessionPhase.INTERRUPTED, updates.last().checkpoint.latestSession!!.phase)
    }
}
