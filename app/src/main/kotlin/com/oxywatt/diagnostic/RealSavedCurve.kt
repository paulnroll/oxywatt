package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
import org.json.JSONObject

/** Existing canonical worker is the sole input/result writer; no writes interleave these reads. */
internal fun readRealSavedCurve(
    analyses: AnalysisRepository,
    selected: SessionSnapshot,
): StoredAnalysis? {
    val slot = PowerCurveCalculation.realSlot(selected.id)
    val result = analyses.newest(slot) ?: return null
    require(result.slot == slot) { "Saved result does not own selected slot" }
    val provenance = JSONObject(result.provenance)
    require(provenance.getInt("codec") == 1 && provenance.getString("codeVersion").isNotBlank())
    val fields = provenance.getJSONArray("fields")
    require(fields.length() == 8)
    if (result.status == AnalysisStatus.AVAILABLE) require(result.terminal?.curve != null)
    result.terminal?.curve?.let { curve ->
        require(curve.acquisition != null && curve.session == selected)
        require(fields.getString(0) == curve.inputHash)
        require(fields.getString(1) == "karoo:1.1.9:karoo-normalization-v1")
        require(fields.getString(3) == "power-range-means-v2")
        require(fields.getString(4) == "canonical-power-hold-v1")
        require(
            fields.getString(5) ==
                "power-replay-work-v2:16384:100000000:karoo-diagnostic-silence-v1:5000000000"
        )
    }
    return result
}
