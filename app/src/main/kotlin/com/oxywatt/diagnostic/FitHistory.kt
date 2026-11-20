package com.oxywatt.diagnostic

import com.oxywatt.core.collection.SessionSnapshot
import com.oxywatt.diagnostic.persistence.*
import java.time.Instant
import org.json.JSONObject

/** A separate file-source cohort, never admission into the live observation/trend contract. */
internal fun loadFitHistory(
    analyses: AnalysisRepository,
    session: SessionSnapshot,
    config: HistoryConfig,
): HistoryEntry {
    val date = session.firstReceipt.utc
    try {
        FitSource.requireSession(session)
    } catch (_: IllegalArgumentException) {
        return HistoryEntry(session.id, date, HistoryReason.INCOMPLETE)
    }
    val saved =
        readRealSavedCurve(analyses, session, fit = true)
            ?: return HistoryEntry(session.id, date, HistoryReason.NO_RESULT)
    val curve = saved.terminal?.curve
    val sourceDate =
        curve?.acquisition?.let { Instant.parse(JSONObject(it).getString("fileDate")) } ?: date
    if (curve != null) {
        val fields = JSONObject(saved.provenance).getJSONArray("fields")
        require(
            fields.getString(6) == "not-applicable-v1" && fields.getString(7) == "not-applicable-v1"
        )
        require(curve.snapshot.policy == com.oxywatt.core.power.PowerCurvePolicy())
        require(curve.snapshot.provenance.preprocessingVersion == "canonical-power-hold-v1")
    }
    val duration = curve?.snapshot?.durations?.single { it.durationSeconds == config.duration }
    val reason =
        when (saved.status) {
            AnalysisStatus.PENDING -> HistoryReason.PENDING
            AnalysisStatus.FAILED -> HistoryReason.FAILED
            AnalysisStatus.UNAVAILABLE -> HistoryReason.UNAVAILABLE
            AnalysisStatus.AVAILABLE ->
                when {
                    curve!!.snapshot.resourceLimited -> HistoryReason.LIMITED
                    duration?.record == null -> HistoryReason.NO_WINDOW
                    else -> HistoryReason.VALUE
                }
        }
    return HistoryEntry(
        session.id,
        sourceDate,
        reason,
        duration?.record?.meanWatts?.takeIf { reason == HistoryReason.VALUE },
        saved.generation,
        saved.provenance,
    )
}
