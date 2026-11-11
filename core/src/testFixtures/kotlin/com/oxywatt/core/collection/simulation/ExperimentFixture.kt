package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/** Versioned length-delimited nullable UTF-8 strings; no serializer or map iteration dependency. */
internal fun semanticDigest(domain: String, fields: List<String?>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    (listOf(domain) + fields).forEach {
        if (it == null) digest.update("N;".toByteArray(Charsets.UTF_8))
        else {
            val bytes = it.toByteArray(Charsets.UTF_8)
            digest.update("S${bytes.size}:".toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

enum class UsePermission {
    ALLOWED,
    RESTRICTED,
    UNKNOWN,
}

data class FixtureRights(
    val creator: String,
    val evidence: String,
    val terms: String,
    val privacyReview: String,
    val processing: UsePermission,
    val redistribution: UsePermission,
) {
    fun validate(publication: Boolean = false) {
        require(listOf(creator, evidence, terms, privacyReview).all { it.isNotBlank() })
        require(processing == UsePermission.ALLOWED)
        if (publication) require(redistribution == UsePermission.ALLOWED)
    }
}

/** Explicit ordered table is the recipe for small fixtures, including every profile/UTC event. */
data class ExperimentFixture(
    val id: String,
    val revision: String,
    val manifest: RunManifest,
    val rights: FixtureRights,
    val events: List<ScheduledObservation>,
    val profiles: List<ExperimentProfile>,
    val expectedCount: Int,
    val expectedEnd: Long,
    val expectation: String,
    val policies: Map<String, String>,
    val schema: String = "experiment-fixture-v1",
    val origin: String = "SYNTHETIC",
    val referenceStatus: String = "SYNTHETIC_ORACLE",
    val units: String = "W,bpm,rpm,kg,ns,UTC",
    val recipe: String = "explicit-ordered-event-table-v1; RNG unused; noise=0; no implicit events",
    val sourceRevision: String = "b6258b177093e187dd5be5d94c325a03440664f8",
    val sourceReferences: List<String> = emptyList(),
) {
    fun validate(publication: Boolean = false) {
        rights.validate(publication)
        require(manifest.fixtureDigest == null) {
            "Descriptor references the base manifest, not itself"
        }
        require(
            manifest.generatorVersion == "1" &&
                manifest.normalizationVersion in setOf("canonical-v1", "fit-normalization-v1")
        )
        require(manifest.schedulerPolicy == "timers-before-input-v1")
        require(profiles.size == 2)
        require(schema == "experiment-fixture-v1" && origin == "SYNTHETIC")
        require(referenceStatus == "SYNTHETIC_ORACLE")
        require(units == "W,bpm,rpm,kg,ns,UTC")
        require(listOf(id, revision, expectation, recipe, sourceRevision).all { it.isNotBlank() })
        require(expectedCount == events.size && expectedEnd == manifest.endNanos)
        require(events.isNotEmpty() && events.last().offsetNanos == expectedEnd)
        require(events.all { it.offsetNanos in 0..expectedEnd })
        require(events.zipWithNext().all { (a, b) -> a.offsetNanos <= b.offsetNanos })
        require(
            profiles.map { it.schema }.toSet() ==
                setOf("ftp-input-profile-v1", "vo2-input-profile-v1")
        )
        require(profiles.map { it.snapshot.version }.distinct() == listOf(manifest.profileVersion))
        events
            .mapNotNull { (it.payload as? InputPayload.Profile)?.snapshot }
            .forEach { snapshot ->
                require(profiles.any { it.snapshot == snapshot })
            }
        require(
            policies.keys ==
                setOf(
                    "preprocessing",
                    "model",
                    "work",
                    "silenceNs",
                    "admission",
                    "flush",
                    "tolerance",
                    "cap",
                    "budget",
                )
        )
        require(policies.values.all { it.isNotBlank() })
    }

    fun observations(expectedDigest: String? = null): Sequence<ScheduledObservation> {
        validate()
        require(policies == experimentPolicies) { "Unsupported experiment execution policy" }
        if (expectedDigest != null)
            require(expectedDigest == digest()) { "Fixture digest mismatch" }
        return events.toList().asSequence()
    }

    fun digest(): String {
        validate()
        return semanticDigest(
            schema,
            listOf(
                id,
                revision,
                manifest.hash(),
                origin,
                referenceStatus,
                units,
                recipe,
                sourceRevision,
                expectedCount.toString(),
                expectedEnd.toString(),
                expectation,
                rights.creator,
                rights.evidence,
                rights.terms,
                rights.privacyReview,
                rights.processing.name,
                rights.redistribution.name,
            ) +
                listOf(sourceReferences.size.toString()) +
                sourceReferences +
                profiles.map { it.digest() } +
                policies.toSortedMap().flatMap { listOf(it.key, it.value) } +
                events.flatMap {
                    listOf(
                        it.offsetNanos.toString(),
                        it.utcShiftNanos.toString(),
                        payloadFields(it.payload),
                    )
                },
        )
    }

    fun linkedManifest(): RunManifest = manifest.copy(fixtureDigest = digest())
}

/** Explicit payload fields, not data-class toString, form the pinned semantic representation. */
private fun payloadFields(p: InputPayload): String =
    when (p) {
        is InputPayload.Recording -> semanticDigest("recording", listOf(p.state.name))
        is InputPayload.MetricReading ->
            semanticDigest(
                "metric",
                listOf(
                    p.metric.name,
                    p.value?.toString(),
                    p.missingReason?.name,
                    p.sourceTime?.utc?.toString(),
                    p.sourceTime?.meaning?.name,
                ),
            )
        is InputPayload.Profile ->
            semanticDigest(
                "profile-event",
                listOf(
                    p.snapshot.version,
                    p.snapshot.effectiveAt?.toString(),
                    p.snapshot.massKg?.value?.toString(),
                    p.snapshot.massKg?.origin?.name,
                    p.snapshot.configuredMaxHeartRateBpm?.value?.toString(),
                    p.snapshot.configuredMaxHeartRateBpm?.origin?.name,
                    p.snapshot.configuredFtpWatts?.value?.toString(),
                    p.snapshot.configuredFtpWatts?.origin?.name,
                ),
            )
    }

fun experimentFixture(kind: String): ExperimentFixture {
    require(kind in setOf("constant", "irregular", "break-profile", "expiry"))
    val profiles = experimentProfiles()
    val events =
        mutableListOf(
            ScheduledObservation(0, InputPayload.Recording(RecordingState.IDLE)),
            ScheduledObservation(0, InputPayload.Recording(RecordingState.RECORDING)),
        )
    if (kind == "constant") {
        for (s in 0L until 60) events.add(
            ScheduledObservation(
                s * 1_000_000_000,
                InputPayload.MetricReading(Metric.POWER_W, 200.0),
            )
        )
        events.add(
            ScheduledObservation(60_000_000_000, InputPayload.Recording(RecordingState.IDLE))
        )
    } else if (kind == "expiry") {
        events.add(ScheduledObservation(0, InputPayload.MetricReading(Metric.POWER_W, 200.0)))
        events.add(
            ScheduledObservation(12_000_000_000, InputPayload.Recording(RecordingState.IDLE))
        )
    } else {
        // Irregular: [0,2)=100, [2,7)=200, [7,8)=0. Break: [0,2), [4,8).
        events.add(ScheduledObservation(0, InputPayload.MetricReading(Metric.POWER_W, 100.0)))
        events.add(
            ScheduledObservation(
                2_000_000_000,
                if (kind == "irregular") InputPayload.MetricReading(Metric.POWER_W, 200.0)
                else InputPayload.Recording(RecordingState.PAUSED),
            )
        )
        if (kind == "break-profile") {
            events.add(
                ScheduledObservation(3_000_000_000, InputPayload.Profile(profiles[0].snapshot))
            )
            events.add(
                ScheduledObservation(
                    4_000_000_000,
                    InputPayload.Recording(RecordingState.RECORDING),
                )
            )
            events.add(
                ScheduledObservation(
                    4_000_000_000,
                    InputPayload.MetricReading(Metric.POWER_W, 200.0),
                )
            )
        }
        events.add(
            ScheduledObservation(7_000_000_000, InputPayload.MetricReading(Metric.POWER_W, 0.0))
        )
        events.add(
            ScheduledObservation(
                8_000_000_000,
                InputPayload.MetricReading(Metric.POWER_W, null, MissingReason.DROPOUT),
            )
        )
        events.add(
            ScheduledObservation(12_000_000_000, InputPayload.Recording(RecordingState.IDLE))
        )
    }
    val end = if (kind == "constant") 60_000_000_000 else 12_000_000_000
    return ExperimentFixture(
        kind,
        "1",
        RunManifest(
            kind,
            0,
            UUID(0, 502),
            Instant.EPOCH,
            end,
            profileVersion = profiles[0].snapshot.version,
        ),
        FixtureRights(
            "OxyWatt generated fixture contribution",
            "docs/spdd/tasks/t05a-fixture-reproducibility.md: owner approval",
            "Owner-authorized project contribution and public PR; no third-party data license asserted",
            "Invented profile, UUIDs and UTC only; no GPS or sensor IDs",
            UsePermission.ALLOWED,
            UsePermission.ALLOWED,
        ),
        events.toList(),
        profiles,
        when (kind) {
            "constant" -> 63
            "irregular" -> 7
            "expiry" -> 4
            else -> 10
        },
        end,
        when (kind) {
            "constant" -> "200 W [0,60); 5/30/60 s at 0; others unavailable"
            "irregular" -> "5 s [2,7)=200 W; longer unavailable"
            "expiry" -> "hold expires at 6 s; 5 s [0,5)=200 W; longer unavailable"
            else -> "pause partitions [0,2),[4,8); all nine unavailable"
        },
        experimentPolicies,
    )
}

/** Only this pinned configuration is executable by the current fixture tests. */
private val experimentPolicies =
    mapOf(
        "preprocessing" to "canonical-power-hold-v1",
        "model" to "power-range-means-v2",
        "work" to "power-replay-work-v2",
        "silenceNs" to "6000000000",
        "admission" to "admission-v1",
        "flush" to "every-observation; timers-before-input-v1",
        "tolerance" to "max(1e-8,1e-12*magnitude)",
        "cap" to "16384",
        "budget" to "100000000",
    )
