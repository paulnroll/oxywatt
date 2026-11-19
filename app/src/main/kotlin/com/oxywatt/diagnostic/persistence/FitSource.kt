package com.oxywatt.diagnostic.persistence

import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID
import org.json.JSONObject

internal object FitSource {
    const val ORIGIN = "real-fit-import"
    const val MANIFEST = "REAL_FIT_FILE:v1"
    const val COHORT = "real-fit-file-power-hold-1s-v1"
    const val NORMALIZATION = "fit-history-normalization-v1"
    const val COVERAGE = "committed-real-fit-file-effects-v1"
    val policy = LifecyclePolicy("fit-file-silence-v1", 5_000_000_000)

    fun slot(id: UUID) = AnalysisSlot(id, "observed-power-curve", COHORT)

    fun validate(j: JSONObject) {
        require(j.getInt("version") == 1 && j.getString("origin") == ORIGIN)
        require(j.getString("decoder") == "fitdecode:0.11.0")
        require(j.getString("clock") == "reconstructed-file-time-v1")
        require(j.getString("recordSupport") == "hold-to-next-record-max-1s-v1")
        require(j.getString("completion") == "file-session-activity-stop-v1")
        require(j.getString("sourceHash").matches(Regex("[0-9a-f]{64}")))
        require(j.getString("projectionHash").matches(Regex("[0-9a-f]{64}")))
        UUID.fromString(j.getString("epoch"))
        Instant.parse(j.getString("importedAt"))
        Instant.parse(j.getString("fileDate"))
        require(j.getLong("rows") in 1..131072)
        require(
            j.getString("storage") == "karoo-diagnostic-storage-v3" &&
                j.getLong("maximumBytes") == 536870912L
        )
    }

    fun requireSession(s: SessionSnapshot) {
        require(s.source.adapterId == "fit-file" && s.source.adapterVersion == "fitdecode-0.11.0")
        require(s.normalizationVersion == NORMALIZATION && s.policy == policy)
        require(s.phase == SessionPhase.INTERRUPTED && s.origin == SessionOrigin.COLD_ATTACH)
        require(
            s.interruptionReason == InterruptionReason.SOURCE_STOPPED && s.ambiguityGroupId == s.id
        )
    }
}
