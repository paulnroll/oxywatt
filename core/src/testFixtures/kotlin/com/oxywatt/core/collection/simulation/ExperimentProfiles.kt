package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.time.Instant

/** Evidence describes supplied inputs, never a physiological eligibility decision. */
data class ExperimentProfile(
    val schema: String,
    val snapshot: RiderProfileSnapshot,
    val context: String,
    val evidence: Map<String, String>,
    val effortWindows: List<String> = emptyList(),
    val protocol: String? = null,
    val restingHrBpm: ProfileSetting? = null,
    val calibration: String? = null,
    val reference: String? = null,
    val unresolved: List<String> = listOf("method-specific requirements not approved"),
) {
    init {
        require(schema in setOf("ftp-input-profile-v1", "vo2-input-profile-v1"))
        require(context in setOf("indoor", "outdoor", "unknown"))
        require(evidence.keys == setOf("mass", "hrmax", "ftp", "restingHr", "calibration"))
        require(evidence.values.all { it.isNotBlank() })
    }

    val automaticResult: String
        get() = "model_not_approved"

    fun digest(): String =
        semanticDigest(
            "experiment-profile-v1",
            listOf(
                schema,
                snapshot.version,
                snapshot.effectiveAt?.toString(),
                snapshot.massKg?.value?.toString(),
                snapshot.massKg?.origin?.name,
                snapshot.configuredMaxHeartRateBpm?.value?.toString(),
                snapshot.configuredMaxHeartRateBpm?.origin?.name,
                snapshot.configuredFtpWatts?.value?.toString(),
                snapshot.configuredFtpWatts?.origin?.name,
                context,
                protocol,
                restingHrBpm?.value?.toString(),
                restingHrBpm?.origin?.name,
                calibration,
                reference,
            ) +
                evidence.toSortedMap().flatMap { listOf(it.key, it.value) } +
                listOf(effortWindows.size.toString()) +
                effortWindows +
                listOf(unresolved.size.toString()) +
                unresolved,
        )
}

fun experimentProfiles(): List<ExperimentProfile> {
    val snapshot =
        RiderProfileSnapshot(
            "invented-profile-v1",
            Instant.EPOCH,
            ProfileSetting(75.0, SettingOrigin.MANUAL),
            ProfileSetting(190.0, SettingOrigin.CONFIGURED),
            ProfileSetting(250.0, SettingOrigin.MANUAL),
        )
    val evidence =
        mapOf(
            "mass" to "synthetic manual kg",
            "hrmax" to "synthetic configured bpm",
            "ftp" to "synthetic manual W; baseline only",
            "restingHr" to "missing",
            "calibration" to "missing",
        )
    return listOf("ftp-input-profile-v1", "vo2-input-profile-v1").map {
        ExperimentProfile(it, snapshot, "unknown", evidence)
    }
}
