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
