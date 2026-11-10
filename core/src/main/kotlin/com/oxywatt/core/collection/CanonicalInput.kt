package com.oxywatt.core.collection

import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Receipt clock is local to an acquisition epoch, never sensor measurement time. */
data class ReceiptStamp(
    val utc: Instant,
    val epochId: UUID,
    val elapsedNanos: Long,
    val ingestSequence: Long,
) {
    init {
        require(elapsedNanos >= 0) { "Elapsed time must be nonnegative" }
        require(ingestSequence >= 0) { "Ingest sequence must be nonnegative" }
    }

    /** Clock difference only: a shared epoch does not establish recording continuity across pauses/gaps. */
    fun elapsedSince(earlier: ReceiptStamp): Duration? =
        if (epochId != earlier.epochId || elapsedNanos < earlier.elapsedNanos) null
        else Duration.ofNanos(elapsedNanos - earlier.elapsedNanos)
}

/** Adapter identifiers/versions, not sensor identifiers or native ride identity. */
data class SourceRef(val adapterId: String, val adapterVersion: String, val sourceSequence: Long? = null) {
    init {
        require(adapterId.isNotBlank()) { "Adapter ID is required" }
        require(adapterVersion.isNotBlank()) { "Adapter version is required" }
        require(sourceSequence == null || sourceSequence >= 0) { "Source sequence must be nonnegative" }
    }
}

data class InputEnvelope(
    val sessionId: UUID,
    val stamp: ReceiptStamp,
    val source: SourceRef,
    val normalizationVersion: String,
    val payload: InputPayload,
) {
    init { require(normalizationVersion.isNotBlank()) { "Normalization version is required" } }
}

enum class Metric { POWER_W, HEART_RATE_BPM, CADENCE_RPM }
enum class MissingReason { UNAVAILABLE, DROPOUT, INVALID_SOURCE_VALUE }
enum class SourceTimeMeaning { MEASUREMENT, FILE_RECORD }
data class SourceTime(val utc: Instant, val meaning: SourceTimeMeaning)
enum class RecordingState { UNKNOWN, IDLE, RECORDING, PAUSED }
enum class SettingOrigin { CONFIGURED, MANUAL }
data class ProfileSetting(val value: Double, val origin: SettingOrigin) {
    init { require(value.isFinite() && value > 0) { "Configured setting must be finite and positive" } }
}
data class RiderProfileSnapshot(
    val version: String,
    val effectiveAt: Instant? = null,
    val massKg: ProfileSetting? = null,
    val configuredMaxHeartRateBpm: ProfileSetting? = null,
    val configuredFtpWatts: ProfileSetting? = null,
) {
    init { require(version.isNotBlank()) { "Profile version is required" } }
}

sealed interface InputPayload {
    data class MetricReading(
        val metric: Metric,
        val value: Double?,
        val missingReason: MissingReason? = null,
        val sourceTime: SourceTime? = null,
    ) : InputPayload {
        init {
            require((value == null) == (missingReason != null)) { "Only missing values require a reason" }
            if (value != null) {
                require(value.isFinite()) { "Metric value must be finite" }
                require(if (metric == Metric.HEART_RATE_BPM) value > 0 else value >= 0) {
                    "Power/cadence must be nonnegative; heart rate must be positive"
                }
            }
        }
    }
    data class Recording(val state: RecordingState) : InputPayload
    data class Profile(val snapshot: RiderProfileSnapshot) : InputPayload
}
