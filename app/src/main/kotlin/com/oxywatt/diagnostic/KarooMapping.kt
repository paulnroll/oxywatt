package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import io.hammerhead.karooext.models.*
import java.security.MessageDigest

internal object KarooMapping {
    fun metric(metric: Metric, field: String, state: StreamState): InputPayload.MetricReading {
        val value = (state as? StreamState.Streaming)?.dataPoint?.values?.get(field)
        val reason =
            when {
                state == StreamState.Searching -> MissingReason.DROPOUT
                value == null -> MissingReason.UNAVAILABLE
                !value.isFinite() ||
                    value < 0 ||
                    (metric == Metric.HEART_RATE_BPM && value == 0.0) ->
                    MissingReason.INVALID_SOURCE_VALUE
                else -> null
            }
        return InputPayload.MetricReading(metric, if (reason == null) value else null, reason)
    }

    fun recording(state: RideState): InputPayload.Recording =
        InputPayload.Recording(
            when (state) {
                RideState.Idle -> RecordingState.IDLE
                RideState.Recording -> RecordingState.RECORDING
                is RideState.Paused -> RecordingState.PAUSED
            }
        )

    fun profile(profile: UserProfile): InputPayload.Profile {
        fun setting(value: Double) =
            value
                .takeIf { it.isFinite() && it > 0 }
                ?.let { ProfileSetting(it, SettingOrigin.CONFIGURED) }
        val mass = setting(profile.weight.toDouble())
        val hr = setting(profile.maxHr.toDouble())
        val ftp = setting(profile.ftp.toDouble())
        val values = "karoo-profile-v1|${mass?.value}|${hr?.value}|${ftp?.value}|CONFIGURED"
        val hash =
            MessageDigest.getInstance("SHA-256")
                .digest(values.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        return InputPayload.Profile(
            RiderProfileSnapshot(
                "karoo-profile-v1:$hash",
                massKg = mass,
                configuredMaxHeartRateBpm = hr,
                configuredFtpWatts = ftp,
            )
        )
    }
}
