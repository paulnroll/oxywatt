package com.oxywatt.diagnostic

import android.content.Context
import com.oxywatt.core.collection.SessionOrigin
import com.oxywatt.core.collection.SessionPhase
import com.oxywatt.core.collection.SessionSnapshot
import com.oxywatt.diagnostic.persistence.AnalysisStatus

internal fun sessionText(context: Context, session: SessionSnapshot): String {
    val phase =
        when (session.phase) {
            SessionPhase.RECORDING -> R.string.local_sessions_recording
            SessionPhase.PAUSED -> R.string.local_sessions_paused
            SessionPhase.ENDED_LOCALLY -> R.string.canonical_ended
            SessionPhase.INTERRUPTED -> R.string.canonical_interrupted
        }
    val origin =
        when (session.origin) {
            SessionOrigin.OBSERVED_START -> R.string.canonical_observed
            SessionOrigin.COLD_ATTACH -> R.string.canonical_cold
            SessionOrigin.RECOVERY_FRAGMENT -> R.string.canonical_fragment
        }
    return context.getString(
        R.string.local_sessions_identity,
        session.id.toString(),
        session.firstReceipt.utc.toString(),
        (session.lastReceipt.elapsedNanos - session.firstReceipt.elapsedNanos) / 1_000_000_000.0,
        context.getString(phase),
        context.getString(origin),
        context.getString(
            if (session.ambiguityGroupId == null) R.string.local_sessions_unambiguous
            else R.string.local_sessions_ambiguous
        ),
    )
}

internal fun sessionCardText(context: Context, card: LocalSessionCard): String {
    val status =
        when {
            card.error -> R.string.local_sessions_error
            card.loading -> R.string.local_sessions_loading
            card.status == null -> R.string.local_sessions_not_calculated
            card.status == AnalysisStatus.AVAILABLE -> R.string.live_curve_saved
            card.status == AnalysisStatus.PENDING -> R.string.live_curve_pending
            else -> R.string.live_curve_failed
        }
    val coverage =
        card.availableDurations?.let { context.getString(R.string.local_sessions_coverage, it) }
            ?: context.getString(R.string.local_sessions_coverage_unknown)
    return listOf(sessionText(context, card.session), context.getString(status), coverage)
        .joinToString("\n")
}
