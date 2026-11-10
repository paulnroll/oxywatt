package com.oxywatt.core.collection

import java.time.Duration
import java.time.Instant
import kotlin.test.*

class SyntheticReplayTest {
    @Test
    fun `replay delivers irregular events in supplied order on every invocation`() {
        val trace = SyntheticReplayFixtures.irregularRide()
        val received = mutableListOf<InputEnvelope>()
        replaySynthetic(trace, received::add)
        assertEquals(12, received.size)
        assertEquals(
            listOf(
                0L,
                0L,
                250_000_000L,
                500_000_000L,
                1_750_000_000L,
                1_750_000_000L,
                2_000_000_000L,
                2_750_000_000L,
                3_000_000_000L,
                4_000_000_000L,
                4_250_000_000L,
                5_000_000_000L,
            ),
            received.map { it.stamp.elapsedNanos },
        )
        assertEquals((0L..11L).toList(), received.map { it.stamp.ingestSequence })
        assertEquals(Duration.ofMillis(1250), received[4].stamp.elapsedSince(received[3].stamp))
        replaySynthetic(trace, received::add)
        assertEquals(24, received.size)
        assertEquals(received.take(12), received.drop(12))
        assertEquals(trace, SyntheticReplayFixtures.irregularRide())
    }

    @Test
    fun `missing HR does not replace power and equal readings retain their keys`() {
        val received = mutableListOf<InputEnvelope>()
        replaySynthetic(SyntheticReplayFixtures.irregularRide(), received::add)
        val metrics = received.mapNotNull { it.payload as? InputPayload.MetricReading }
        assertEquals(
            listOf(
                Metric.POWER_W,
                Metric.POWER_W,
                Metric.HEART_RATE_BPM,
                Metric.POWER_W,
                Metric.POWER_W,
                Metric.HEART_RATE_BPM,
            ),
            metrics.map { it.metric },
        )
        assertEquals(listOf(200.0, 0.0, null, 200.0, 200.0, 150.0), metrics.map { it.value })
        assertEquals(
            listOf(null, null, MissingReason.DROPOUT, null, null, null),
            metrics.map { it.missingReason },
        )
        assertEquals(received[8].payload, received[9].payload)
        assertNotEquals(received[8].stamp.ingestSequence, received[9].stamp.ingestSequence)
    }

    @Test
    fun `initial idle pause resume and profile are explicit inputs only`() {
        val received = mutableListOf<InputEnvelope>()
        replaySynthetic(SyntheticReplayFixtures.irregularRide(), received::add)
        assertEquals(
            listOf(
                RecordingState.IDLE,
                RecordingState.RECORDING,
                RecordingState.PAUSED,
                RecordingState.RECORDING,
                RecordingState.IDLE,
            ),
            received.mapNotNull {
                (it.payload as? InputPayload.Recording)?.state
            },
        )
        val profile = (received[2].payload as InputPayload.Profile).snapshot
        assertEquals("synthetic-profile-v1", profile.version)
        assertEquals(ProfileSetting(75.0, SettingOrigin.MANUAL), profile.massKg)
        assertNull(profile.effectiveAt)
        assertNull(profile.configuredFtpWatts)
        assertNull(profile.configuredMaxHeartRateBpm)
    }

    @Test
    fun `synthetic file variant preserves values with distinct timestamp provenance`() {
        val live = SyntheticReplayFixtures.irregularRide()
        val file = SyntheticReplayFixtures.irregularRide(fileLike = true)
        assertEquals(live.map { it.stamp }, file.map { it.stamp })
        assertEquals(live.map { it.sessionId }, file.map { it.sessionId })
        assertTrue(file.all { it.normalizationVersion == "normalization-v1" })
        assertTrue(live.all { it.source == SourceRef("synthetic-live", "1") })
        assertTrue(file.all { it.source == SourceRef("synthetic-file", "1") })
        val firstFileReading = file[3].payload as InputPayload.MetricReading
        assertEquals(Instant.parse("2025-12-31T23:59:50.500Z"), firstFileReading.sourceTime?.utc)
        assertEquals(Instant.parse("2026-01-01T00:00:00.500Z"), file[3].stamp.utc)
        live.zip(file).forEach { (liveEvent, fileEvent) ->
            val liveReading = liveEvent.payload as? InputPayload.MetricReading
            if (liveReading != null) {
                val fileReading = fileEvent.payload as InputPayload.MetricReading
                assertNull(liveReading.sourceTime)
                assertEquals(
                    SourceTime(fileEvent.stamp.utc.minusSeconds(10), SourceTimeMeaning.FILE_RECORD),
                    fileReading.sourceTime,
                )
                assertEquals(liveReading, fileReading.copy(sourceTime = null))
            } else assertEquals(liveEvent.payload, fileEvent.payload)
        }
    }

    @Test
    fun `cold attachment restart stays separate and has no cross epoch duration`() {
        val received = mutableListOf<InputEnvelope>()
        replaySynthetic(SyntheticReplayFixtures.coldAttachAndRestart(), received::add)
        assertEquals(2, received.size)
        assertEquals(
            listOf(RecordingState.RECORDING, RecordingState.RECORDING),
            received.map {
                (it.payload as InputPayload.Recording).state
            },
        )
        assertNotEquals(received[0].sessionId, received[1].sessionId)
        assertNotEquals(received[0].stamp.epochId, received[1].stamp.epochId)
        assertEquals(listOf(0L, 0L), received.map { it.stamp.ingestSequence })
        assertNull(received[1].stamp.elapsedSince(received[0].stamp))
    }

    @Test
    fun `replay does not sort deduplicate or emit anything for an empty trace`() {
        val trace = SyntheticReplayFixtures.irregularRide()
        val received = mutableListOf<InputEnvelope>()
        replaySynthetic(emptyList(), received::add)
        assertTrue(received.isEmpty())
        replaySynthetic(listOf(trace[9], trace[3], trace[9]), received::add)
        assertEquals(listOf(9L, 3L, 9L), received.map { it.stamp.ingestSequence })
    }
}
