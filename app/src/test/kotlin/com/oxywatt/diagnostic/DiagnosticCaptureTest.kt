package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import java.io.File
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class DiagnosticCaptureTest {
    @get:Rule val temp = TemporaryFolder()
    private var now = 10L
    private val epoch = UUID(0, 1)

    private fun input(value: Double?, reason: MissingReason? = null) =
        SourceObservation(
            ReceiptStamp(Instant.EPOCH, epoch, now, now),
            SourceRef("karoo", "1.1.9"),
            "karoo-normalization-v1",
            InputPayload.MetricReading(Metric.POWER_W, value, reason),
        )

    @Test
    fun `zero remains a received value and explicit missing removes the previous number`() {
        val capture = DiagnosticCapture(File(temp.root, "report.jsonl"), nowNanos = { now })
        capture.accept(input(0.0))
        now = 30
        assertEquals(0.0, capture.snapshot().metrics[Metric.POWER_W]!!.value!!, 0.0)
        assertEquals(20L, capture.snapshot().metrics[Metric.POWER_W]!!.ageNanos)
        capture.accept(input(null, MissingReason.DROPOUT))
        val reading = capture.snapshot().metrics[Metric.POWER_W]!!
        assertNull(reading.value)
        assertEquals(MissingReason.DROPOUT, reading.reason)
        assertEquals(2L, reading.callbacks)
    }

    @Test
    fun `private report survives reopening without retaining live numbers or profile`() {
        val file = File(temp.root, "report.jsonl")
        val capture = DiagnosticCapture(file, nowNanos = { now })
        capture.accept(input(821.125))
        capture.accept(
            input(1.0)
                .copy(
                    payload =
                        InputPayload.Profile(
                            RiderProfileSnapshot(
                                "private-profile-231.875",
                                massKg = ProfileSetting(73.625, SettingOrigin.MANUAL),
                            )
                        )
                )
        )
        capture.flush()
        assertTrue("Report must actually reach the private file", file.isFile)
        val report = file.readText()
        assertFalse(report.contains("821.125"))
        assertFalse(report.contains("231.875"))
        assertFalse(report.contains("73.625"))
        assertTrue(report.contains("POWER_W"))
        assertEquals(DiagnosticReportStatus.SAVED, capture.snapshot().reportStatus)
        assertEquals(file.length(), capture.snapshot().savedBytes)
        val reopened = DiagnosticCapture(file, nowNanos = { now })
        assertTrue(reopened.snapshot().metrics.isEmpty())
        reopened.flush()
        assertTrue(file.readText().length > report.length)
    }

    @Test
    fun `file limit storage failure and incomplete tail never claim saved`() {
        val capped = File(temp.root, "capped.jsonl")
        val capture = DiagnosticCapture(capped, maxBytes = 1)
        capture.flush()
        assertEquals(DiagnosticReportStatus.LIMIT_REACHED, capture.snapshot().reportStatus)
        assertFalse(capped.exists())
        val badParent = temp.newFile("not-a-directory")
        val failed = DiagnosticCapture(File(badParent, "report.jsonl"))
        failed.flush()
        assertEquals(DiagnosticReportStatus.STORAGE_ERROR, failed.snapshot().reportStatus)
        assertEquals(0L, failed.snapshot().savedBytes)
        val partial = temp.newFile("partial.jsonl").apply { writeText("unfinished") }
        val incomplete = DiagnosticCapture(partial)
        incomplete.flush()
        assertEquals(DiagnosticReportStatus.INCOMPLETE, incomplete.snapshot().reportStatus)
        assertEquals("unfinished", partial.readText())
    }

    @Test
    fun `bounded event overflow is visible and source stop clears live values`() {
        val file = File(temp.root, "bounded.jsonl")
        val capture = DiagnosticCapture(file, maxEvents = 2)
        repeat(50) { capture.connection(it % 2 == 0) }
        assertEquals(2, capture.snapshot().pendingEvents)
        assertTrue(capture.snapshot().droppedEvents > 0)
        capture.accept(input(100.0))
        capture.sourceStopped(false)
        assertNull(capture.snapshot().metrics[Metric.POWER_W]!!.value)
        capture.flush()
        assertTrue(file.readText().contains("diagnostic_events_dropped"))
        assertEquals(0, capture.snapshot().pendingEvents)
    }

    @Test
    fun `event capacity includes the batch blocked in the file writer`() {
        val capture = DiagnosticCapture(File(temp.root, "slow.jsonl"), maxEvents = 2)
        capture.connection(true)
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
        capture.beforeAppendForTest = {
            entered.countDown()
            check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
        }
        val writer = Thread {
            try {
                capture.flush()
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        writer.start()
        try {
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
            repeat(50) { capture.connection(it % 2 == 0) }
            assertEquals(2, capture.snapshot().pendingEvents)
            assertEquals(50L, capture.snapshot().droppedEvents)
        } finally {
            release.countDown()
            writer.join(5000)
        }
        assertFalse(writer.isAlive)
        assertNull(failure.get())
        capture.beforeAppendForTest = {}
        capture.flush()
        assertEquals(0, capture.snapshot().pendingEvents)
    }

    @Test
    fun `stopped source rejects late readings until a new explicit source start`() {
        val capture = DiagnosticCapture(File(temp.root, "late.jsonl"))
        capture.sourceStarted()
        capture.accept(input(150.0))
        capture.sourceStopped(true)
        capture.accept(input(999.0))
        assertNull(capture.snapshot().metrics[Metric.POWER_W]!!.value)
        assertFalse(capture.snapshot().sourceActive)
        capture.sourceStarted()
        capture.accept(input(180.0))
        assertEquals(180.0, capture.snapshot().metrics[Metric.POWER_W]!!.value!!, 0.0)
    }
}
