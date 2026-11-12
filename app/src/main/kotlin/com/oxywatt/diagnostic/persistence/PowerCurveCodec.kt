package com.oxywatt.diagnostic.persistence

import com.oxywatt.core.collection.*
import com.oxywatt.core.power.*
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** A diagnostic observation document, never a history/trend contributor. */
data class StoredPowerCurve(
    val session: SessionSnapshot,
    val snapshot: PowerCurveSnapshot,
    val revision: Long,
    val firstRevision: Long,
    val lastRevision: Long,
    val inputHash: String,
    val manifest: String,
    val reservedWork: Long,
    val workLimited: Boolean,
    val effectCount: Long,
    val intervalCount: Long,
    val acquisition: String? = null,
) {
    init {
        acquisition?.let {
            val metadata = JSONObject(it)
            CollectorCodec.validateLivePowerAcquisition(metadata)
            require(metadata.getString("epoch") == session.epochId.toString())
            require(session.policy == LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000))
            require(
                session.phase == SessionPhase.ENDED_LOCALLY &&
                    session.origin == SessionOrigin.OBSERVED_START &&
                    session.ambiguityGroupId == null
            )
            require(manifest == PowerCurveCalculation.REAL)
            require(
                snapshot.provenance.coverageOrigin == "committed-real-karoo-diagnostic-effects-v1"
            )
        }
        if (acquisition == null)
            require(
                manifest.startsWith("SYNTHETIC:") &&
                    snapshot.provenance.coverageOrigin == "committed-synthetic-effects-v1"
            )
        require(session.phase in setOf(SessionPhase.ENDED_LOCALLY, SessionPhase.INTERRUPTED))
        require(
            snapshot.sessionId == session.id &&
                revision >= lastRevision &&
                firstRevision in 1..lastRevision
        )
        require(inputHash.matches(Regex("[0-9a-f]{64}")) && manifest.isNotBlank())
        require(reservedWork >= 0 && effectCount > 0 && intervalCount >= 0)
        require(!workLimited || snapshot.resourceLimited)
        require(snapshot.durations.map { it.durationSeconds } == PowerCurveEngine.DURATIONS)
        snapshot.durations.forEach { d ->
            require((d.record == null) != (d.unavailableReason == null))
            d.record?.let { r ->
                require(
                    r.durationSeconds == d.durationSeconds &&
                        r.sessionId == session.id &&
                        r.epochId == session.epochId &&
                        r.segmentIndex in 0..session.segmentIndex
                )
                require(
                    r.meanWatts.isFinite() &&
                        r.meanWatts >= 0 &&
                        r.startNanos >= 0 &&
                        r.endNanos - r.startNanos == d.durationSeconds * 1_000_000_000L
                )
            }
        }
    }
}

internal object PowerCurveCodec {
    fun encode(v: StoredPowerCurve): JSONObject {
        val s = v.snapshot
        return JSONObject()
            .put("codec", if (v.acquisition == null) 1 else 3)
            .apply {
                v.acquisition?.let {
                    put("acquisition", JSONObject(it))
                    put("acquisitionOrigin", "real-karoo-diagnostic")
                }
            }
            .put("session", CollectorCodec.session(v.session))
            .put("revision", v.revision)
            .put("firstRevision", v.firstRevision)
            .put("lastRevision", v.lastRevision)
            .put("hash", v.inputHash)
            .put("manifest", v.manifest)
            .put("reserved", v.reservedWork)
            .put("workLimited", v.workLimited)
            .put("effects", v.effectCount)
            .put("intervals", v.intervalCount)
            .put("preprocessing", s.provenance.preprocessingVersion)
            .put("origin", s.provenance.coverageOrigin)
            .put("policy", s.policy.version)
            .put("cap", s.policy.maxIntervals)
            .put("resourceLimited", s.resourceLimited)
            .put("retained", s.retainedIntervals)
            .put("windows", s.evaluatedWindows)
            .put("fragments", s.integratedFragments)
            .put(
                "durations",
                JSONArray(
                    s.durations.map { d ->
                        JSONObject()
                            .put("duration", d.durationSeconds)
                            .put("reason", d.unavailableReason?.name ?: JSONObject.NULL)
                            .put(
                                "record",
                                d.record?.let { r ->
                                    JSONObject()
                                        .put("watts", java.lang.Double.toHexString(r.meanWatts))
                                        .put("start", r.startNanos)
                                        .put("end", r.endNanos)
                                        .put("epoch", r.epochId.toString())
                                        .put("segment", r.segmentIndex)
                                } ?: JSONObject.NULL,
                            )
                    }
                ),
            )
    }

    fun decode(j: JSONObject): StoredPowerCurve {
        require(j.getInt("codec") in setOf(1, 3))
        if (j.getInt("codec") == 3)
            require(j.getString("acquisitionOrigin") == "real-karoo-diagnostic")
        val session = CollectorCodec.session(j.getJSONObject("session"))
        val ds = j.getJSONArray("durations")
        val durations =
            java.util.Collections.unmodifiableList(
                (0 until ds.length()).map { i ->
                    val d = ds.getJSONObject(i)
                    val seconds = d.getInt("duration")
                    PowerDurationResult(
                        seconds,
                        if (d.isNull("record")) null
                        else
                            d.getJSONObject("record").let { r ->
                                PowerRecord(
                                    seconds,
                                    r.getString("watts").toDouble(),
                                    r.getLong("start"),
                                    r.getLong("end"),
                                    session.id,
                                    UUID.fromString(r.getString("epoch")),
                                    r.getLong("segment"),
                                )
                            },
                        if (d.isNull("reason")) null
                        else PowerUnavailableReason.valueOf(d.getString("reason")),
                    )
                }
            )
        return StoredPowerCurve(
            session,
            PowerCurveSnapshot(
                session.id,
                PowerProvenance(j.getString("preprocessing"), j.getString("origin")),
                PowerCurvePolicy(j.getString("policy"), j.getInt("cap")),
                durations,
                j.getBoolean("resourceLimited"),
                j.getInt("retained"),
                j.getLong("windows"),
                j.getLong("fragments"),
            ),
            j.getLong("revision"),
            j.getLong("firstRevision"),
            j.getLong("lastRevision"),
            j.getString("hash"),
            j.getString("manifest"),
            j.getLong("reserved"),
            j.getBoolean("workLimited"),
            j.getLong("effects"),
            j.getLong("intervals"),
            if (j.getInt("codec") == 3) j.getJSONObject("acquisition").toString() else null,
        )
    }
}
