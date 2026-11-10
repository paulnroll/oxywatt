package com.oxywatt.core.collection

import java.time.Instant
import java.util.UUID

/** Synthetic contract examples only; neither an SDK mapping nor a decoded FIT. */
internal object SyntheticReplayFixtures {
    private val origin = Instant.parse("2026-01-01T00:00:00Z")

    private val fileOrigin = Instant.parse("2025-12-31T23:59:50Z")

    fun irregularRide(fileLike: Boolean = false): List<InputEnvelope> {
        val rows =
            listOf(
                0L to InputPayload.Recording(RecordingState.IDLE),
                0L to InputPayload.Recording(RecordingState.RECORDING),
                250L to
                    InputPayload.Profile(
                        RiderProfileSnapshot(
                            "synthetic-profile-v1",
                            massKg = ProfileSetting(75.0, SettingOrigin.MANUAL),
                        )
                    ),
                500L to InputPayload.MetricReading(Metric.POWER_W, 200.0),
                1750L to InputPayload.MetricReading(Metric.POWER_W, 0.0),
                1750L to
                    InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.DROPOUT),
                2000L to InputPayload.Recording(RecordingState.PAUSED),
                2750L to InputPayload.Recording(RecordingState.RECORDING),
                3000L to InputPayload.MetricReading(Metric.POWER_W, 200.0),
                4000L to InputPayload.MetricReading(Metric.POWER_W, 200.0),
                4250L to InputPayload.MetricReading(Metric.HEART_RATE_BPM, 150.0),
                5000L to InputPayload.Recording(RecordingState.IDLE),
            )
        return rows.mapIndexed { index, (millis, payload) ->
            val stampedPayload =
                if (fileLike && payload is InputPayload.MetricReading) {
                    payload.copy(
                        sourceTime =
                            SourceTime(fileOrigin.plusMillis(millis), SourceTimeMeaning.FILE_RECORD)
                    )
                } else payload
            InputEnvelope(
                UUID(0, 1),
                ReceiptStamp(
                    origin.plusMillis(millis),
                    UUID(0, 10),
                    millis * 1_000_000,
                    index.toLong(),
                ),
                SourceRef(if (fileLike) "synthetic-file" else "synthetic-live", "1"),
                "normalization-v1",
                stampedPayload,
            )
        }
    }

    /** Cold attachment and restart are deliberately not merged into a known complete ride. */
    fun coldAttachAndRestart(): List<InputEnvelope> =
        listOf(
            InputEnvelope(
                UUID(0, 2),
                ReceiptStamp(origin, UUID(0, 20), 0, 0),
                SourceRef("synthetic-live", "1"),
                "normalization-v1",
                InputPayload.Recording(RecordingState.RECORDING),
            ),
            InputEnvelope(
                UUID(0, 3),
                ReceiptStamp(origin.plusSeconds(10), UUID(0, 21), 0, 0),
                SourceRef("synthetic-live", "1"),
                "normalization-v1",
                InputPayload.Recording(RecordingState.RECORDING),
            ),
        )
}

/** Test-only synchronous delivery; the consumer owns all domain interpretation. */
internal fun replaySynthetic(inputs: List<InputEnvelope>, accept: (InputEnvelope) -> Unit) {
    inputs.forEach(accept)
}
