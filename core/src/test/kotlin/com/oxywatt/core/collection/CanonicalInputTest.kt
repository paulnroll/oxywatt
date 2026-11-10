package com.oxywatt.core.collection

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.*

class CanonicalInputTest {
    private val epoch = UUID(0, 1)
    private val utc = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `receipt durations never cross process epochs`() {
        val start = ReceiptStamp(utc, epoch, 5_000_000_000, 0)
        val restarted = ReceiptStamp(utc.plusSeconds(2), UUID(0, 2), 7_500_000_000, 0)
        assertNull(restarted.elapsedSince(start))
    }

    @Test
    fun `zero watts and dropout remain distinct`() {
        val zero = InputPayload.MetricReading(Metric.POWER_W, 0.0)
        val missing = InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.DROPOUT)
        assertEquals(0.0, zero.value)
        assertNull(missing.value)
        assertEquals(MissingReason.DROPOUT, missing.missingReason)
    }

    @Test
    fun `invalid readings cannot enter canonical inputs`() {
        for (metric in Metric.entries) {
            for (value in
                listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
                assertFailsWith<IllegalArgumentException> {
                    InputPayload.MetricReading(metric, value)
                }
            }
            assertFailsWith<IllegalArgumentException> { InputPayload.MetricReading(metric, null) }
            assertFailsWith<IllegalArgumentException> {
                InputPayload.MetricReading(metric, 100.0, MissingReason.UNAVAILABLE)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            InputPayload.MetricReading(Metric.HEART_RATE_BPM, 0.0)
        }
        assertEquals(0.0, InputPayload.MetricReading(Metric.CADENCE_RPM, 0.0).value)
    }

    @Test
    fun `elapsed time ignores wall clock changes and rejects reversed order`() {
        val start = ReceiptStamp(utc, epoch, 5_000_000_000, 0)
        val later = ReceiptStamp(utc.minusSeconds(10), epoch, 7_500_000_000, 1)
        assertEquals(Duration.ofMillis(2500), later.elapsedSince(start))
        assertEquals(Duration.ZERO, start.elapsedSince(start))
        assertNull(start.elapsedSince(later))
    }

    @Test
    fun `invalid metadata and configured settings are rejected`() {
        assertFailsWith<IllegalArgumentException> { ReceiptStamp(utc, epoch, -1, 0) }
        assertFailsWith<IllegalArgumentException> { ReceiptStamp(utc, epoch, 0, -1) }
        assertFailsWith<IllegalArgumentException> { SourceRef(" ", "1") }
        assertFailsWith<IllegalArgumentException> { SourceRef("live", " ") }
        assertFailsWith<IllegalArgumentException> { SourceRef("live", "1", -1) }
        assertFailsWith<IllegalArgumentException> { RiderProfileSnapshot(" ") }
        for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                ProfileSetting(value, SettingOrigin.CONFIGURED)
            }
        }
        assertFailsWith<IllegalArgumentException> {
            InputEnvelope(
                UUID(0, 3),
                ReceiptStamp(utc, epoch, 0, 0),
                SourceRef("live", "1"),
                " ",
                InputPayload.Recording(RecordingState.IDLE),
            )
        }
    }

    @Test
    fun `source timestamp provenance survives identical decoded values`() {
        val live = InputPayload.MetricReading(Metric.POWER_W, 210.0)
        val file =
            InputPayload.MetricReading(
                Metric.POWER_W,
                210.0,
                sourceTime = SourceTime(utc.minusSeconds(2), SourceTimeMeaning.FILE_RECORD),
            )
        assertEquals(live.value, file.value)
        assertEquals(live.metric, file.metric)
        assertNull(live.sourceTime)
        assertEquals(SourceTimeMeaning.FILE_RECORD, file.sourceTime?.meaning)
        assertNotEquals(live, file)
    }

    @Test
    fun `recording and profile changes retain facts without implying completion`() {
        val profile =
            RiderProfileSnapshot("profile-v1", massKg = ProfileSetting(75.0, SettingOrigin.MANUAL))
        val stamp = ReceiptStamp(utc, epoch, 0, 0)
        val initial =
            InputEnvelope(
                UUID(0, 3),
                stamp,
                SourceRef("synthetic", "1"),
                "normalization-v1",
                InputPayload.Recording(RecordingState.IDLE),
            )
        val recording =
            initial.copy(
                stamp = stamp.copy(ingestSequence = 1),
                payload = InputPayload.Recording(RecordingState.RECORDING),
            )
        val changed =
            initial.copy(
                stamp = stamp.copy(ingestSequence = 2),
                payload = InputPayload.Profile(profile),
            )
        assertEquals(initial.sessionId, recording.sessionId)
        assertEquals(InputPayload.Recording(RecordingState.IDLE), initial.payload)
        assertEquals(profile, (changed.payload as InputPayload.Profile).snapshot)
        assertNull(profile.effectiveAt)
        assertNull(profile.configuredMaxHeartRateBpm)
        assertNull(profile.configuredFtpWatts)
        assertEquals(SettingOrigin.MANUAL, profile.massKg?.origin)
    }

    @Test
    fun `largest supported elapsed interval does not overflow`() {
        val origin = ReceiptStamp(utc, epoch, 0, 0)
        val last = ReceiptStamp(utc, epoch, Long.MAX_VALUE, Long.MAX_VALUE)
        assertEquals(Duration.ofSeconds(9_223_372_036, 854_775_807), last.elapsedSince(origin))
        assertNull(origin.elapsedSince(last))
    }

    @Test
    fun `copies cannot bypass canonical input validation`() {
        val reading = InputPayload.MetricReading(Metric.POWER_W, 0.0)
        assertFailsWith<IllegalArgumentException> { reading.copy(metric = Metric.HEART_RATE_BPM) }
        assertFailsWith<IllegalArgumentException> { reading.copy(value = null) }
        val dropout = reading.copy(value = null, missingReason = MissingReason.DROPOUT)
        assertFailsWith<IllegalArgumentException> { dropout.copy(value = 100.0) }
        assertEquals(reading, dropout.copy(value = 0.0, missingReason = null))
        assertFailsWith<IllegalArgumentException> {
            ReceiptStamp(utc, epoch, 0, 0).copy(elapsedNanos = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            ProfileSetting(75.0, SettingOrigin.MANUAL).copy(value = Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `repeated equal measurements retain distinct local ingestion keys`() {
        val first =
            InputEnvelope(
                UUID(0, 3),
                ReceiptStamp(utc, epoch, 0, 0),
                SourceRef("synthetic", "1"),
                "normalization-v1",
                InputPayload.MetricReading(Metric.POWER_W, 200.0),
            )
        val next = first.copy(stamp = first.stamp.copy(ingestSequence = 1))
        assertEquals(first.payload, next.payload)
        assertNotEquals(first, next)
        assertNull(first.source.sourceSequence)
        assertEquals(listOf(0L, 1L), listOf(first, next).map { it.stamp.ingestSequence })
    }
}
