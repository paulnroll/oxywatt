package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import io.hammerhead.karooext.models.*
import org.junit.Assert.*
import org.junit.Test

class KarooMappingTest {
    @Test
    fun `named field preserves zero while absent and invalid remain missing`() {
        fun map(values: Map<String, Double>) =
            KarooMapping.metric(
                Metric.POWER_W,
                DataType.Field.POWER,
                StreamState.Streaming(DataPoint(DataType.Type.POWER, values)),
            )
        assertEquals(
            InputPayload.MetricReading(Metric.POWER_W, 0.0),
            map(mapOf(DataType.Field.POWER to 0.0)),
        )
        assertEquals(
            InputPayload.MetricReading(Metric.POWER_W, null, MissingReason.UNAVAILABLE),
            map(mapOf(DataType.Field.SINGLE to 200.0)),
        )
        for (value in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(
                InputPayload.MetricReading(
                    Metric.POWER_W,
                    null,
                    MissingReason.INVALID_SOURCE_VALUE,
                ),
                map(mapOf(DataType.Field.POWER to value)),
            )
        }
    }

    @Test
    fun `all metric states retain absence and validity rules`() {
        val channels =
            listOf(
                Triple(Metric.POWER_W, DataType.Type.POWER, DataType.Field.POWER),
                Triple(Metric.HEART_RATE_BPM, DataType.Type.HEART_RATE, DataType.Field.HEART_RATE),
                Triple(Metric.CADENCE_RPM, DataType.Type.CADENCE, DataType.Field.CADENCE),
            )
        for ((metric, type, field) in channels) {
            for (state in listOf(StreamState.Idle, StreamState.NotAvailable)) {
                assertEquals(
                    InputPayload.MetricReading(metric, null, MissingReason.UNAVAILABLE),
                    KarooMapping.metric(metric, field, state),
                )
            }
            assertEquals(
                InputPayload.MetricReading(metric, null, MissingReason.DROPOUT),
                KarooMapping.metric(metric, field, StreamState.Searching),
            )
            for (value in
                listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
                assertEquals(
                    InputPayload.MetricReading(metric, null, MissingReason.INVALID_SOURCE_VALUE),
                    KarooMapping.metric(
                        metric,
                        field,
                        StreamState.Streaming(DataPoint(type, mapOf(field to value))),
                    ),
                )
            }
            val present =
                KarooMapping.metric(
                    metric,
                    field,
                    StreamState.Streaming(DataPoint(type, mapOf(field to 101.0))),
                )
            assertEquals(InputPayload.MetricReading(metric, 101.0), present)
            assertNull(present.sourceTime)
        }
    }
}
