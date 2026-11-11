package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*

/** Safe fixed acceptance table: no Idle at EOF. */
fun previewScenario(): Sequence<ScheduledObservation> =
    sequenceOf(
        ScheduledObservation(0, InputPayload.Recording(RecordingState.IDLE)),
        ScheduledObservation(0, InputPayload.Recording(RecordingState.RECORDING)),
        ScheduledObservation(1_000_000_000, InputPayload.MetricReading(Metric.POWER_W, 0.0)),
        ScheduledObservation(1_000_000_000, InputPayload.MetricReading(Metric.CADENCE_RPM, 85.0)),
        ScheduledObservation(
            2_000_000_000,
            InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.UNAVAILABLE),
        ),
        ScheduledObservation(3_000_000_000, InputPayload.Recording(RecordingState.PAUSED)),
        ScheduledObservation(5_000_000_000, InputPayload.Recording(RecordingState.RECORDING)),
        ScheduledObservation(6_000_000_000, InputPayload.MetricReading(Metric.POWER_W, 180.0)),
    )

/** Extends existing diagnostic states with six covered seconds and an observed local end. */
fun powerCurvePreviewScenario(): Sequence<ScheduledObservation> = sequence {
    yieldAll(previewScenario())
    for (t in 10L..15) yield(
        ScheduledObservation(t * 1_000_000_000, InputPayload.MetricReading(Metric.POWER_W, 200.0))
    )
    yield(ScheduledObservation(16_000_000_000, InputPayload.Recording(RecordingState.IDLE)))
}
