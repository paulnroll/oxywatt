package com.oxywatt.core.collection

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FitReplayTest {
    @Test
    fun `decoded FIT fixture replays canonical readings with file record provenance`() {
        val session = UUID(0, 31)
        val epoch = UUID(0, 32)
        val fileStart = Instant.parse("2025-12-31T00:00:00Z")
        val receiptStart = Instant.parse("2026-01-01T00:00:00Z")
        val fixture = checkNotNull(javaClass.getResource("/fit-replay.tsv")).readText()
        val inputs =
            fixture
                .lineSequence()
                .filter { it.isNotEmpty() }
                .mapIndexed { index, line ->
                    val columns = line.split('\t')
                    require(columns.size == 4) { "Invalid fixture row ${index + 1}" }
                    val (timestamp, kind, value, reason) = columns
                    val fileTime = Instant.parse(timestamp)
                    val offset = Duration.between(fileStart, fileTime)
                    val payload =
                        if (kind == "STATE") {
                            InputPayload.Recording(RecordingState.valueOf(value))
                        } else {
                            InputPayload.MetricReading(
                                Metric.valueOf(kind),
                                value.takeUnless { it == "-" }?.toDouble(),
                                reason.takeUnless { it == "-" }?.let(MissingReason::valueOf),
                                SourceTime(fileTime, SourceTimeMeaning.FILE_RECORD),
                            )
                        }
                    InputEnvelope(
                        session,
                        ReceiptStamp(
                            receiptStart.plus(offset),
                            epoch,
                            offset.toNanos(),
                            index.toLong(),
                        ),
                        SourceRef("fitdecode-test", "0.11.0"),
                        "normalization-v1",
                        payload,
                    )
                }
                .toList()

        val expected =
            listOf(
                InputPayload.Recording(RecordingState.RECORDING),
                InputPayload.MetricReading(Metric.POWER_W, 200.0),
                InputPayload.MetricReading(Metric.HEART_RATE_BPM, 145.0),
                InputPayload.MetricReading(Metric.CADENCE_RPM, 90.0),
                InputPayload.MetricReading(Metric.POWER_W, 0.0),
                InputPayload.MetricReading(
                    Metric.HEART_RATE_BPM,
                    null,
                    MissingReason.INVALID_SOURCE_VALUE,
                ),
                InputPayload.MetricReading(Metric.CADENCE_RPM, 0.0),
                InputPayload.Recording(RecordingState.PAUSED),
                InputPayload.Recording(RecordingState.RECORDING),
                InputPayload.MetricReading(Metric.POWER_W, 210.0),
                InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.UNAVAILABLE),
                InputPayload.MetricReading(Metric.CADENCE_RPM, null, MissingReason.UNAVAILABLE),
                InputPayload.MetricReading(
                    Metric.POWER_W,
                    null,
                    MissingReason.INVALID_SOURCE_VALUE,
                ),
                InputPayload.MetricReading(
                    Metric.HEART_RATE_BPM,
                    null,
                    MissingReason.INVALID_SOURCE_VALUE,
                ),
                InputPayload.MetricReading(
                    Metric.CADENCE_RPM,
                    null,
                    MissingReason.INVALID_SOURCE_VALUE,
                ),
            )
        val offsets = listOf(0L, 0L, 0L, 0L, 2L, 2L, 2L, 3L, 8L, 8L, 8L, 8L, 9L, 9L, 9L)
        val liveLike = expected.mapIndexed { index, payload ->
            val offset = offsets[index]
            InputEnvelope(
                session,
                ReceiptStamp(
                    receiptStart.plusSeconds(offset),
                    epoch,
                    offset * 1_000_000_000,
                    index.toLong(),
                ),
                SourceRef("synthetic-live", "1"),
                "normalization-v1",
                payload,
            )
        }
        val received = mutableListOf<InputEnvelope>()
        replaySynthetic(inputs, received::add)
        assertEquals(liveLike.size, received.size)
        received.zip(liveLike).forEach { (fit, live) ->
            val reading = fit.payload as? InputPayload.MetricReading
            if (reading != null) {
                assertEquals(
                    SourceTime(
                        fileStart.plusNanos(live.stamp.elapsedNanos),
                        SourceTimeMeaning.FILE_RECORD,
                    ),
                    reading.sourceTime,
                )
            }
            assertEquals(SourceRef("fitdecode-test", "0.11.0"), fit.source)
            assertEquals(
                live,
                fit.copy(
                    source = live.source,
                    payload = reading?.copy(sourceTime = null) ?: fit.payload,
                ),
            )
        }
    }
}
