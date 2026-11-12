package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
import java.util.UUID
import java.util.concurrent.CancellationException
import org.json.JSONObject

internal enum class LiveCurveStatus {
    UNAVAILABLE,
    READY,
    CALCULATING,
    SAVED,
    PENDING,
    FAILED,
    RETRYING,
}

internal enum class LiveCurveReason {
    NO_ENDED_SESSION,
    SOURCE_NOT_IDLE,
    STORAGE_BLOCKED,
    CANCELLED,
    ANALYSIS_ERROR,
}

internal data class LiveCurveSnapshot(
    val status: LiveCurveStatus = LiveCurveStatus.UNAVAILABLE,
    val reason: LiveCurveReason? = LiveCurveReason.NO_ENDED_SESSION,
    val saved: StoredAnalysis? = null,
    val canCalculate: Boolean = false,
)

/** Confined to the live input worker; the sole cross-thread action is a coalesced tap. */
internal class LiveCurveController(
    private val inputs: InputRepository,
    private val analyses: AnalysisRepository,
    private val owner: LiveDiagnosticCoordinator,
    private val codeVersion: String,
    private val elapsed: () -> Long,
) {
    private val actionGate = Any()
    private var requested = false
    @Volatile private var busy = false
    @Volatile private var view = LiveCurveSnapshot()

    fun snapshot() = view

    fun request() =
        synchronized(actionGate) {
            if (!busy && view.canCalculate) requested = true
        }

    private var job: PowerCurveCalculation.Job? = null
    private var completion: PreparedPowerCompletion? = null
    private var target: UUID? = null
    private var loaded = false
    private var retryAt = 0L
    private var retryFailed = false
    private var loadFailed = false
    private var completionAttempted = false
    private var problem: LiveCurveReason? = null
    private var stored: StoredAnalysis? = null

    private fun eligible(s: SessionSnapshot?) =
        s != null &&
            s.phase == SessionPhase.ENDED_LOCALLY &&
            s.origin == SessionOrigin.OBSERVED_START &&
            s.ambiguityGroupId == null

    private fun healthy() =
        owner.snapshot().status in setOf(LiveStorageStatus.SAVED, LiveStorageStatus.COLLECTING)

    val hasWork
        get() =
            (busy && healthy() && elapsed() >= retryAt) || synchronized(actionGate) { requested }

    fun pump() {
        val session = owner.snapshot().session
        // A sticky received-state latch also notices Recording -> Idle between worker turns.
        if (job != null && owner.analysisMustCancel()) {
            completion = if (!completionAttempted) job!!.cancel(true) else completion
            if (completion == null) {
                job = null
                busy = false
                problem = LiveCurveReason.CANCELLED
                view = LiveCurveSnapshot(LiveCurveStatus.FAILED, LiveCurveReason.CANCELLED)
            }
        }
        if (!healthy()) {
            view = LiveCurveSnapshot(LiveCurveStatus.UNAVAILABLE, LiveCurveReason.STORAGE_BLOCKED)
            return
        }
        if (elapsed() < retryAt) return
        try {
            if (!busy) {
                if (!eligible(session) || !owner.canAnalyze()) {
                    synchronized(actionGate) { requested = false }
                    val cancelledTarget =
                        problem == LiveCurveReason.CANCELLED && target == session?.id
                    view =
                        LiveCurveSnapshot(
                            status =
                                if (cancelledTarget) LiveCurveStatus.FAILED
                                else LiveCurveStatus.UNAVAILABLE,
                            reason =
                                when {
                                    cancelledTarget -> LiveCurveReason.CANCELLED
                                    !eligible(session) -> LiveCurveReason.NO_ENDED_SESSION
                                    else -> LiveCurveReason.SOURCE_NOT_IDLE
                                },
                        )
                    return
                }
                val click = synchronized(actionGate) { requested.also { requested = false } }
                if (problem != null && target == session!!.id && !click && !loadFailed) {
                    view = LiveCurveSnapshot(LiveCurveStatus.FAILED, problem, canCalculate = true)
                    return
                }
                if (!loaded || target != session!!.id || loadFailed || (click && problem != null)) {
                    target = session!!.id
                    problem = null
                    stored = null
                    loaded = false
                    stored = analyses.newest(PowerCurveCalculation.realSlot(target!!))
                    loaded = true
                    loadFailed = false
                }
                val savedForCode =
                    stored?.let {
                        it.status == AnalysisStatus.AVAILABLE &&
                            JSONObject(it.provenance).getString("codeVersion") == codeVersion
                    } == true
                if (click && !savedForCode && owner.armAnalysis()) {
                    job =
                        PowerCurveCalculation(inputs, analyses)
                            .beginReal(target!!, codeVersion, owner::analysisMustCancel)
                    completion = null
                    completionAttempted = false
                    problem = null
                    synchronized(actionGate) {
                        busy = true
                        requested = false
                    }
                    view = LiveCurveSnapshot(LiveCurveStatus.CALCULATING, reason = null)
                } else {
                    view =
                        LiveCurveSnapshot(
                            if (problem != null) LiveCurveStatus.FAILED
                            else
                                when (stored?.status) {
                                    AnalysisStatus.AVAILABLE -> LiveCurveStatus.SAVED
                                    AnalysisStatus.PENDING -> LiveCurveStatus.PENDING
                                    AnalysisStatus.FAILED,
                                    AnalysisStatus.UNAVAILABLE -> LiveCurveStatus.FAILED
                                    null -> LiveCurveStatus.READY
                                },
                            problem,
                            if (problem == null) stored else null,
                            true,
                        )
                    return
                }
            }
            if (completion == null) {
                job!!.step()
                completion = job!!.completion
            } else {
                // Input pump ran first; only the exact frozen terminal is retried here.
                completionAttempted = true
                analyses.complete(completion!!.generation, completion!!.terminal)
                stored = analyses.newest(PowerCurveCalculation.realSlot(target!!))
                job = null
                completion = null
                busy = false
                view =
                    if (
                        owner.canAnalyze() &&
                            eligible(owner.snapshot().session) &&
                            owner.snapshot().session?.id == target
                    )
                        LiveCurveSnapshot(
                            if (stored!!.status == AnalysisStatus.AVAILABLE) LiveCurveStatus.SAVED
                            else LiveCurveStatus.FAILED,
                            null,
                            stored,
                            true,
                        )
                    else LiveCurveSnapshot(reason = LiveCurveReason.SOURCE_NOT_IDLE)
            }
            retryAt = 0L
            retryFailed = false
        } catch (cancelled: CancellationException) {
            completion = job?.cancel()
            if (completion == null) {
                job = null
                busy = false
            }
            problem = LiveCurveReason.CANCELLED
            view = LiveCurveSnapshot(LiveCurveStatus.FAILED, problem)
        } catch (failure: android.database.sqlite.SQLiteException) {
            retryAt = elapsed() + if (retryFailed) 5_000_000_000L else 1_000_000_000L
            retryFailed = true
            loadFailed = !busy
            view = LiveCurveSnapshot(LiveCurveStatus.RETRYING, LiveCurveReason.STORAGE_BLOCKED)
        } catch (failure: Exception) {
            // A contract/codec conflict is not a transient storage retry. Persisted status stays
            // authoritative; another attempt requires an explicit tap, not a busy retry loop.
            job = null
            completion = null
            busy = false
            loadFailed = false
            synchronized(actionGate) { requested = false }
            problem = LiveCurveReason.ANALYSIS_ERROR
            view = LiveCurveSnapshot(LiveCurveStatus.FAILED, problem)
        }
    }
}

internal fun liveCurveText(context: android.content.Context, state: LiveCurveSnapshot): String {
    fun text(id: Int) = context.getString(id)
    val status =
        when (state.status) {
            LiveCurveStatus.READY -> R.string.live_curve_ready
            LiveCurveStatus.CALCULATING -> R.string.live_curve_calculating
            LiveCurveStatus.SAVED -> R.string.live_curve_saved
            LiveCurveStatus.PENDING -> R.string.live_curve_pending
            LiveCurveStatus.FAILED -> R.string.live_curve_failed
            LiveCurveStatus.RETRYING -> R.string.live_curve_retrying
            LiveCurveStatus.UNAVAILABLE -> R.string.live_curve_no_session
        }
    val effectiveReason =
        state.reason
            ?: when (state.saved?.terminal?.reason) {
                "CANCELLED" -> LiveCurveReason.CANCELLED
                "INVALID_OR_UNSUPPORTED_POWER_REPLAY" -> LiveCurveReason.ANALYSIS_ERROR
                else -> null
            }
    val reason =
        when (effectiveReason) {
            LiveCurveReason.NO_ENDED_SESSION -> R.string.live_curve_no_session
            LiveCurveReason.SOURCE_NOT_IDLE -> R.string.live_curve_not_idle
            LiveCurveReason.STORAGE_BLOCKED -> R.string.live_curve_storage
            LiveCurveReason.CANCELLED -> R.string.live_curve_cancelled
            LiveCurveReason.ANALYSIS_ERROR -> R.string.live_curve_invalid
            null -> null
        }
    val saved = state.saved
    val curve = saved?.terminal?.curve
    val details =
        if (saved == null) ""
        else {
            val header =
                context.getString(R.string.live_curve_generation, saved.generation, text(status))
            if (curve == null) header
            else {
                val values =
                    curve.snapshot.durations.joinToString("\n") { d ->
                        context.getString(
                            R.string.live_curve_duration,
                            d.durationSeconds,
                            d.record?.let {
                                String.format(
                                    java.util.Locale.US,
                                    text(R.string.live_curve_watts),
                                    it.meanWatts,
                                )
                            }
                                ?: text(
                                    if (
                                        d.unavailableReason ==
                                            com.oxywatt.core.power.PowerUnavailableReason
                                                .RESOURCE_LIMIT
                                    )
                                        R.string.live_curve_duration_limited
                                    else R.string.live_curve_no_window
                                ),
                        )
                    }
                header +
                    "\n" +
                    context.getString(
                        R.string.live_curve_details,
                        curve.revision,
                        curve.inputHash,
                        text(
                            if (curve.snapshot.resourceLimited) R.string.live_curve_limited
                            else R.string.live_curve_complete
                        ),
                        values,
                    )
            }
        }
    return listOf(text(R.string.live_curve_scope), text(status), reason?.let(::text), details)
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .joinToString("\n")
}
