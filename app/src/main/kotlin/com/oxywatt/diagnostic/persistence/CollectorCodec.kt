package com.oxywatt.diagnostic.persistence

import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Explicit storage format. Encoded observations contain private data and must never be logged. */
internal object CollectorCodec {
    private fun obj(vararg entries: Pair<String, Any?>) =
        JSONObject().apply {
            entries.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
        }

    private fun JSONObject.optional(key: String) = if (isNull(key)) null else getJSONObject(key)

    private fun JSONObject.text(key: String) = if (isNull(key)) null else getString(key)

    private fun JSONObject.longOrNull(key: String) = if (isNull(key)) null else getLong(key)

    private fun <T> JSONArray.objects(convert: (JSONObject) -> T) =
        (0 until length()).map { convert(getJSONObject(it)) }

    private fun stamp(v: ReceiptStamp) =
        obj(
            "utc" to v.utc.toString(),
            "epoch" to v.epochId.toString(),
            "elapsed" to v.elapsedNanos,
            "sequence" to v.ingestSequence,
        )

    private fun stamp(v: JSONObject) =
        ReceiptStamp(
            Instant.parse(v.getString("utc")),
            UUID.fromString(v.getString("epoch")),
            v.getLong("elapsed"),
            v.getLong("sequence"),
        )

    private fun source(v: SourceRef) =
        obj("adapter" to v.adapterId, "version" to v.adapterVersion, "sequence" to v.sourceSequence)

    private fun source(v: JSONObject) =
        SourceRef(v.getString("adapter"), v.getString("version"), v.longOrNull("sequence"))

    private fun policy(v: LifecyclePolicy) =
        obj("version" to v.version, "silence" to v.channelSilenceNanos)

    private fun policy(v: JSONObject) =
        LifecyclePolicy(v.getString("version"), v.getLong("silence"))

    private fun setting(v: ProfileSetting?) = v?.let {
        obj("value" to java.lang.Double.toHexString(it.value), "origin" to it.origin.name)
    }

    private fun setting(v: JSONObject?) = v?.let {
        ProfileSetting(
            java.lang.Double.valueOf(it.getString("value")),
            SettingOrigin.valueOf(it.getString("origin")),
        )
    }

    private fun payload(v: InputPayload): JSONObject =
        when (v) {
            is InputPayload.Recording -> obj("kind" to "recording", "state" to v.state.name)
            is InputPayload.MetricReading ->
                obj(
                    "kind" to "metric",
                    "metric" to v.metric.name,
                    "value" to v.value?.let(java.lang.Double::toHexString),
                    "missing" to v.missingReason?.name,
                    "sourceUtc" to v.sourceTime?.utc?.toString(),
                    "meaning" to v.sourceTime?.meaning?.name,
                )
            is InputPayload.Profile ->
                v.snapshot.let {
                    obj(
                        "kind" to "profile",
                        "version" to it.version,
                        "effective" to it.effectiveAt?.toString(),
                        "mass" to setting(it.massKg),
                        "hr" to setting(it.configuredMaxHeartRateBpm),
                        "ftp" to setting(it.configuredFtpWatts),
                    )
                }
        }

    private fun payload(v: JSONObject): InputPayload =
        when (v.getString("kind")) {
            "recording" -> InputPayload.Recording(RecordingState.valueOf(v.getString("state")))
            "metric" ->
                InputPayload.MetricReading(
                    Metric.valueOf(v.getString("metric")),
                    v.text("value")?.let(java.lang.Double::valueOf),
                    v.text("missing")?.let(MissingReason::valueOf),
                    v.text("sourceUtc")?.let {
                        SourceTime(
                            Instant.parse(it),
                            SourceTimeMeaning.valueOf(v.getString("meaning")),
                        )
                    },
                )
            "profile" ->
                InputPayload.Profile(
                    RiderProfileSnapshot(
                        v.getString("version"),
                        v.text("effective")?.let(Instant::parse),
                        setting(v.optional("mass")),
                        setting(v.optional("hr")),
                        setting(v.optional("ftp")),
                    )
                )
            else -> error("Unsupported payload kind")
        }

    fun observation(v: SourceObservation) =
        obj(
            "stamp" to stamp(v.stamp),
            "source" to source(v.source),
            "normalization" to v.normalizationVersion,
            "payload" to payload(v.payload),
        )

    fun observation(v: JSONObject) =
        SourceObservation(
            stamp(v.getJSONObject("stamp")),
            source(v.getJSONObject("source")),
            v.getString("normalization"),
            payload(v.getJSONObject("payload")),
        )

    fun session(v: SessionSnapshot) =
        obj(
            "id" to v.id.toString(),
            "epoch" to v.epochId.toString(),
            "phase" to v.phase.name,
            "origin" to v.origin.name,
            "first" to stamp(v.firstReceipt),
            "last" to stamp(v.lastReceipt),
            "segment" to v.segmentIndex,
            "policy" to policy(v.policy),
            "source" to source(v.source),
            "normalization" to v.normalizationVersion,
            "ambiguity" to v.ambiguityGroupId?.toString(),
            "interruption" to v.interruptionReason?.name,
        )

    fun session(v: JSONObject) =
        SessionSnapshot(
            UUID.fromString(v.getString("id")),
            UUID.fromString(v.getString("epoch")),
            SessionPhase.valueOf(v.getString("phase")),
            SessionOrigin.valueOf(v.getString("origin")),
            stamp(v.getJSONObject("first")),
            stamp(v.getJSONObject("last")),
            v.getLong("segment"),
            policy(v.getJSONObject("policy")),
            source(v.getJSONObject("source")),
            v.getString("normalization"),
            v.text("ambiguity")?.let(UUID::fromString),
            v.text("interruption")?.let(InterruptionReason::valueOf),
        )

    fun checkpoint(v: CollectorCheckpoint) =
        obj(
            "schema" to v.schemaVersion,
            "epoch" to v.epochId.toString(),
            "policy" to policy(v.policy),
            "last" to v.lastReceipt?.let(::stamp),
            "session" to v.latestSession?.let(::session),
            "profile" to v.profileObservation?.let(::observation),
        )

    fun checkpoint(v: JSONObject): CollectorCheckpoint {
        require(v.getString("schema") == "collector-checkpoint-v1") {
            "Unsupported checkpoint format"
        }
        return CollectorCheckpoint(
            UUID.fromString(v.getString("epoch")),
            policy(v.getJSONObject("policy")),
            v.optional("last")?.let(::stamp),
            v.optional("session")?.let(::session),
            v.optional("profile")?.let(::observation),
        )
    }

    fun update(v: LifecycleUpdate) =
        obj(
            "checkpoint" to checkpoint(v.checkpoint),
            "envelope" to
                v.envelope?.let {
                    obj(
                        "session" to it.sessionId.toString(),
                        "input" to
                            observation(
                                SourceObservation(
                                    it.stamp,
                                    it.source,
                                    it.normalizationVersion,
                                    it.payload,
                                )
                            ),
                    )
                },
            "segment" to v.recordingSegment,
            "sessions" to JSONArray(v.sessionChanges.map(::session)),
            "breaks" to
                JSONArray(
                    v.coverageBreaks.map {
                        obj(
                            "session" to it.sessionId.toString(),
                            "metric" to it.metric.name,
                            "reason" to it.reason.name,
                            "time" to it.detectedAtNanos,
                        )
                    }
                ),
            "rejection" to v.rejection?.name,
        )

    fun update(v: JSONObject): LifecycleUpdate {
        val envelope =
            v.optional("envelope")?.let {
                val input = observation(it.getJSONObject("input"))
                InputEnvelope(
                    UUID.fromString(it.getString("session")),
                    input.stamp,
                    input.source,
                    input.normalizationVersion,
                    input.payload,
                )
            }
        return LifecycleUpdate(
            checkpoint(v.getJSONObject("checkpoint")),
            envelope,
            v.longOrNull("segment"),
            v.getJSONArray("sessions").objects(::session),
            v.getJSONArray("breaks").objects {
                ChannelCoverageBreak(
                    UUID.fromString(it.getString("session")),
                    Metric.valueOf(it.getString("metric")),
                    CoverageBreakReason.valueOf(it.getString("reason")),
                    it.getLong("time"),
                )
            },
            v.text("rejection")?.let(InputRejection::valueOf),
        )
    }

    fun metadata(batch: AdmissionBatch) = metadata(batch.policy, batch.loss, batch.terminal)

    fun metadata(
        policy: AdmissionPolicy,
        loss: AdmissionLoss? = null,
        terminal: InterruptionReason? = null,
    ) =
        obj(
            "codec" to 1,
            "policy" to
                obj(
                    "version" to policy.version,
                    "events" to policy.events,
                    "bytes" to policy.bytes,
                    "reservedEvents" to policy.reservedEvents,
                    "reservedBytes" to policy.reservedBytes,
                    "envelopeBytes" to policy.envelopeBytes,
                    "flushNanos" to policy.flushNanos,
                ),
            "terminal" to terminal?.name,
            "loss" to
                loss?.let {
                    obj(
                        "first" to stamp(it.first),
                        "last" to stamp(it.last),
                        "count" to it.count,
                        "reasons" to JSONArray(it.reasons.map { reason -> reason.name }.sorted()),
                    )
                },
        )

    fun loss(v: JSONObject): AdmissionLoss? =
        v.optional("loss")?.let {
            val reasons = it.getJSONArray("reasons")
            AdmissionLoss(
                stamp(it.getJSONObject("first")),
                stamp(it.getJSONObject("last")),
                it.getLong("count"),
                (0 until reasons.length())
                    .map { n -> AdmissionLossReason.valueOf(reasons.getString(n)) }
                    .toSet(),
            )
        }

    fun admissionPolicy(v: JSONObject): AdmissionPolicy =
        v.getJSONObject("policy").let {
            AdmissionPolicy(
                it.getString("version"),
                it.getInt("events"),
                it.getInt("bytes"),
                it.getInt("reservedEvents"),
                it.getInt("reservedBytes"),
                it.getInt("envelopeBytes"),
                it.getLong("flushNanos"),
            )
        }

    fun validateAcquisition(value: JSONObject) {
        require(value.toString().length <= 8192)
        require(value.getInt("version") == 1) { "Unsupported acquisition version" }
        require(value.getString("origin") == "real-karoo-diagnostic")
        for (key in
            listOf(
                "appVersion",
                "codeRevision",
                "epoch",
                "ordering",
                "retry",
                "storage",
                "silence",
            )) require(value.getString(key).isNotBlank())
        java.util.UUID.fromString(value.getString("epoch"))
        value.getBoolean("dirty")
        require(value.getLong("firstRetryNanos") == 1_000_000_000L)
        require(value.getLong("subsequentRetryNanos") == 5_000_000_000L)
        require(value.getLong("silenceNanos") > 0 && value.getLong("maximumBytes") > 0)
    }

    fun acquisition(metadata: JSONObject): String? {
        require(metadata.getInt("codec") == 1) { "Unsupported metadata codec" }
        return if (metadata.isNull("acquisition")) null
        else metadata.getJSONObject("acquisition").also(::validateAcquisition).toString()
    }
}
