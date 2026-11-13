package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
import java.util.UUID
import java.util.concurrent.CancellationException
import org.json.JSONObject

internal enum class LiveCurveStatus {
    UNAVAILABLE,
    LOADING,
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
    SELECT_SESSION,
    ACTIVE_SESSION,
    INTERRUPTED_SESSION,
    COLD_SESSION,
    AMBIGUOUS_SESSION,
}

internal data class LiveCurveSnapshot(
    val status: LiveCurveStatus = LiveCurveStatus.UNAVAILABLE,
    val reason: LiveCurveReason? = LiveCurveReason.SELECT_SESSION,
    val saved: StoredAnalysis? = null,
    val canCalculate: Boolean = false,
    val session: SessionSnapshot? = null,
)

internal data class LocalSessionCard(
    val session: SessionSnapshot,
    val loading: Boolean = true,
    val status: AnalysisStatus? = null,
    val availableDurations: Int? = null,
    val error: Boolean = false,
)

internal data class LocalSessionsSnapshot(
    val cards: List<LocalSessionCard> = emptyList(),
    val revision: Long = 0,
    val loading: Boolean = false,
    val truncated: Boolean = false,
    val error: Boolean = false,
    val unavailable: Boolean = true,
    val canChange: Boolean = false,
    val dirty: Boolean = false,
)

/** All SQL and work are confined to the canonical worker. UI actions coalesce under one gate. */
internal class LiveCurveController(
    private val inputs: InputRepository,
    private val analyses: AnalysisRepository,
    private val owner: LiveDiagnosticCoordinator,
    private val codeVersion: String,
    private val elapsed: () -> Long,
    private val executionClock: (() -> Long)? = System::nanoTime,
    private val pacing: AnalysisPacing = AnalysisPacing(),
) {
    @Volatile private var timing: CurveTiming? = null

    fun timingSnapshot(): CurveTimingSnapshot? = timing?.snapshot()

    private var analysisProgressed = false

    /** Worker-only: list reads and retry/fault/cancellation turns cannot consume a burst. */
    fun shouldWaitAfterTurn(): Boolean =
        pacing.shouldWait(
            analysisProgressed &&
                busy &&
                job != null &&
                healthy() &&
                elapsed() >= retryAt &&
                !owner.analysisMustCancel() &&
                (completion == null || completion?.terminal?.status == AnalysisStatus.AVAILABLE)
        )

    fun <T> measureWorker(bucket: CurveWork, action: () -> T): T {
        val t = timing
        return if (t == null) action() else t.measure(bucket, action)
    }

    fun waitForWorker(action: () -> Unit) {
        val t = timing
        t?.count(CurveCount.WAITS)
        val bucket = if (elapsed() < retryAt) CurveWork.BACKOFF else CurveWork.WAIT
        measureWorker(bucket, action)
    }

    fun stopped() {
        pacing.shouldWait(false)
        timing?.finish(CurveOutcome.STOPPED)
    }

    private val actionGate = Any()
    private var requested: UUID? = null
    private var selection: UUID? = null
    private var refresh = true
    private var refreshSelection = false
    @Volatile private var busy = false
    @Volatile private var view = LiveCurveSnapshot()
    @Volatile private var list = LocalSessionsSnapshot(loading = true, unavailable = false)
    private var enrich = 0

    fun snapshot(): LiveCurveSnapshot {
        val current = view
        val sourceReady = owner.canAnalyze()
        return current.copy(
            status =
                if (current.status == LiveCurveStatus.READY && !sourceReady)
                    LiveCurveStatus.UNAVAILABLE
                else current.status,
            reason =
                if (!sourceReady && current.session != null && current.reason == null)
                    LiveCurveReason.SOURCE_NOT_IDLE
                else current.reason,
            canCalculate =
                current.canCalculate &&
                    !busy &&
                    sourceReady &&
                    synchronized(actionGate) { selection == null },
        )
    }

    fun sessions(): LocalSessionsSnapshot =
        list.copy(
            canChange = !busy && healthy(),
            dirty = owner.snapshot().revision != list.revision,
        )

    fun select(id: UUID) =
        synchronized(actionGate) {
            if (!busy) {
                selection = id
                if (requested != null) timing?.finish(CurveOutcome.DISCARDED)
                requested = null
            }
        }

    fun refreshSessions() =
        synchronized(actionGate) {
            if (!busy) {
                refresh = true
                refreshSelection = true
            }
        }

    fun request() =
        synchronized(actionGate) {
            if (!busy && requested == null && selection == null && snapshot().canCalculate) {
                requested = view.session?.id
                // An acknowledged same-code result is read back, not a new attempt.
                if (
                    view.saved?.let {
                        JSONObject(it.provenance).getString("codeVersion") == codeVersion &&
                            it.status == AnalysisStatus.AVAILABLE
                    } != true
                )
                    timing = executionClock?.let {
                        CurveTiming(it, pacing.policy, pacing.maximumTurns)
                    }
            }
        }

    private var job: PowerCurveCalculation.Job? = null
    private var completion: PreparedPowerCompletion? = null
    private var target: UUID? = null
    private var session: SessionSnapshot? = null
    private var loaded = false
    private var retryAt = 0L
    private var retryFailed = false
    private var loadFailed = false
    private var completionAttempted = false
    private var problem: LiveCurveReason? = null
    private var stored: StoredAnalysis? = null

    private fun refusal(s: SessionSnapshot?): LiveCurveReason? =
        when {
            s == null -> LiveCurveReason.NO_ENDED_SESSION
            s.phase == SessionPhase.RECORDING || s.phase == SessionPhase.PAUSED ->
                LiveCurveReason.ACTIVE_SESSION
            s.origin == SessionOrigin.COLD_ATTACH -> LiveCurveReason.COLD_SESSION
            s.ambiguityGroupId != null -> LiveCurveReason.AMBIGUOUS_SESSION
            s.phase != SessionPhase.ENDED_LOCALLY -> LiveCurveReason.INTERRUPTED_SESSION
            s.origin != SessionOrigin.OBSERVED_START -> LiveCurveReason.AMBIGUOUS_SESSION
            s.source.adapterId != "karoo" ||
                s.source.adapterVersion != "1.1.9" ||
                s.normalizationVersion != "karoo-normalization-v1" ||
                s.policy != LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000) ->
                LiveCurveReason.ANALYSIS_ERROR
            else -> null
        }

    private fun healthy() =
        owner.snapshot().status in setOf(LiveStorageStatus.SAVED, LiveStorageStatus.COLLECTING)

    val hasWork: Boolean
        get() =
            healthy() &&
                elapsed() >= retryAt &&
                (busy ||
                    loadFailed ||
                    view.status == LiveCurveStatus.LOADING ||
                    synchronized(actionGate) {
                        requested != null || selection != null || refresh
                    } ||
                    enrich < list.cards.size)

    private fun publish() {
        val invalid = refusal(session)
        val cancelled = stored?.terminal?.reason == "CANCELLED"
        val reason =
            problem
                ?: if (cancelled) LiveCurveReason.CANCELLED
                else invalid ?: if (!owner.canAnalyze()) LiveCurveReason.SOURCE_NOT_IDLE else null
        val saved = if (problem == null && !cancelled && invalid == null) stored else null
        view =
            LiveCurveSnapshot(
                when {
                    problem != null || cancelled -> LiveCurveStatus.FAILED
                    invalid != null -> LiveCurveStatus.UNAVAILABLE
                    else ->
                        when (saved?.status) {
                            AnalysisStatus.AVAILABLE -> LiveCurveStatus.SAVED
                            AnalysisStatus.PENDING -> LiveCurveStatus.PENDING
                            AnalysisStatus.FAILED,
                            AnalysisStatus.UNAVAILABLE -> LiveCurveStatus.FAILED
                            null ->
                                if (owner.canAnalyze()) LiveCurveStatus.READY
                                else LiveCurveStatus.UNAVAILABLE
                        }
                },
                reason,
                saved,
                invalid == null && owner.canAnalyze(),
                session,
            )
    }

    private fun readSaved(selected: SessionSnapshot): StoredAnalysis? =
        readRealSavedCurve(analyses, selected)

    private fun updateCard(error: Boolean = problem != null, working: Boolean = false) {
        list =
            list.copy(
                cards =
                    java.util.Collections.unmodifiableList(
                        list.cards.map { card ->
                            if (card.session.id == target)
                                card.copy(
                                    loading = working,
                                    status = if (error || working) null else stored?.status,
                                    availableDurations =
                                        if (error || working) null
                                        else
                                            stored?.terminal?.curve?.snapshot?.durations?.count {
                                                it.record != null
                                            },
                                    error = error,
                                )
                            else card
                        }
                    )
            )
    }

    /** One bounded page or one newest-result read per turn; never on render. */
    private fun pumpList(): Boolean {
        val (requestedRefresh, reloadSelection) =
            synchronized(actionGate) {
                Pair(refresh, refreshSelection).also {
                    refresh = false
                    refreshSelection = false
                }
            }
        if (requestedRefresh) {
            try {
                val page = inputs.recentSessions()
                list =
                    LocalSessionsSnapshot(
                        java.util.Collections.unmodifiableList(
                            page.sessions.map(::LocalSessionCard)
                        ),
                        page.revision,
                        page.sessions.isNotEmpty(),
                        page.truncated,
                        unavailable = false,
                    )
                enrich = 0
                if (reloadSelection && target != null) {
                    view = LiveCurveSnapshot(LiveCurveStatus.LOADING, null, session = session)
                    session = null
                    stored = null
                    loaded = false
                    loadFailed = false
                    problem = null
                }
            } catch (_: Exception) {
                list = list.copy(loading = false, error = true)
                enrich = list.cards.size
            }
            return true
        }
        if (enrich >= list.cards.size) return false
        val index = enrich++
        val card = list.cards[index]
        val updated =
            if (card.session.id == target && problem != null) {
                card.copy(loading = false, status = null, availableDurations = null, error = true)
            } else
                try {
                    val result = readSaved(card.session)
                    card.copy(
                        loading = false,
                        status = result?.status,
                        availableDurations =
                            result?.terminal?.curve?.snapshot?.durations?.count {
                                it.record != null
                            },
                    )
                } catch (_: Exception) {
                    card.copy(loading = false, error = true)
                }
        list =
            list.copy(
                cards =
                    java.util.Collections.unmodifiableList(
                        list.cards.mapIndexed { i, v -> if (i == index) updated else v }
                    ),
                loading = enrich < list.cards.size,
            )
        return true
    }

    fun pump() {
        analysisProgressed = false
        val t = timing
        if (t != null) {
            t.measure(CurveWork.CONTROL) { pumpInternal() }
            if (!busy && synchronized(actionGate) { requested == null }) {
                when {
                    view.status == LiveCurveStatus.SAVED -> {
                        t.mark(CurveMark.PUBLISHED)
                        t.finish(CurveOutcome.SAVED)
                    }
                    view.reason == LiveCurveReason.CANCELLED -> t.finish(CurveOutcome.CANCELLED)
                    view.status == LiveCurveStatus.FAILED -> t.finish(CurveOutcome.FAILED)
                    view.status == LiveCurveStatus.UNAVAILABLE -> t.finish(CurveOutcome.DISCARDED)
                }
            }
        } else pumpInternal()
    }

    private fun pumpInternal() {
        if (job != null && owner.analysisMustCancel()) {
            completion = if (!completionAttempted) job!!.cancel(true) else completion
            if (completion == null) {
                job = null
                busy = false
                problem = LiveCurveReason.CANCELLED
                updateCard(error = true)
                view =
                    LiveCurveSnapshot(
                        LiveCurveStatus.FAILED,
                        problem,
                        canCalculate = refusal(session) == null && owner.canAnalyze(),
                        session = session,
                    )
            }
        }
        if (!healthy()) {
            view = LiveCurveSnapshot(reason = LiveCurveReason.STORAGE_BLOCKED, session = session)
            return
        }
        if (elapsed() < retryAt) return
        try {
            if (!busy) {
                val chosen = synchronized(actionGate) { selection.also { selection = null } }
                if (chosen != null) {
                    target = chosen
                    session = null
                    stored = null
                    loaded = false
                    loadFailed = false
                    problem = null
                }
                val click = synchronized(actionGate) { requested.also { requested = null } }
                if (click != null) timing?.mark(CurveMark.FIRST_WORKER)
                if (chosen == null && click == null && !loadFailed) {
                    if (target != null && loaded) publish()
                    if (pumpList()) return
                }
                if (target == null) {
                    view = LiveCurveSnapshot()
                    return
                }
                if (session == null || click == target || loadFailed) {
                    session = inputs.session(target!!)
                    loadFailed = false
                }
                if (refusal(session) != null) {
                    stored = null
                    publish()
                    return
                }
                if (problem != null && click == null && !loadFailed) {
                    publish()
                    return
                }
                if (!loaded || loadFailed || (click != null && problem != null)) {
                    problem = null
                    stored = null
                    loaded = false
                    stored = readSaved(session!!)
                    loaded = true
                    loadFailed = false
                }
                val savedForCode =
                    stored?.let {
                        it.status == AnalysisStatus.AVAILABLE &&
                            JSONObject(it.provenance).getString("codeVersion") == codeVersion
                    } == true
                if (click == target && !savedForCode) {
                    val armed =
                        synchronized(actionGate) {
                            if (selection == null && owner.armAnalysis()) {
                                busy = true
                                requested = null
                                true
                            } else false
                        }
                    if (armed) {
                        updateCard(working = true)
                        job =
                            PowerCurveCalculation(inputs, analyses)
                                .also { it.timing = timing }
                                .beginReal(target!!, codeVersion, owner::analysisMustCancel)
                        completion = null
                        completionAttempted = false
                        problem = null
                        view =
                            LiveCurveSnapshot(LiveCurveStatus.CALCULATING, null, session = session)
                    } else timing?.finish(CurveOutcome.DISCARDED)
                }
                if (!busy) {
                    publish()
                    updateCard()
                    return
                }
            }
            if (completion == null) {
                job!!.step()
                completion = job!!.completion
                analysisProgressed = true
            } else {
                completionAttempted = true
                measureWorker(CurveWork.COMPLETE) {
                    analyses.complete(completion!!.generation, completion!!.terminal)
                }
                measureWorker(CurveWork.READBACK) {
                    stored = readSaved(session!!)
                    job = null
                    completion = null
                    busy = false
                    publish()
                    updateCard()
                }
            }
            retryAt = 0L
            retryFailed = false
        } catch (_: CancellationException) {
            completion = job?.cancel()
            if (completion == null) {
                job = null
                busy = false
            }
            problem = LiveCurveReason.CANCELLED
            updateCard(error = true)
            view =
                LiveCurveSnapshot(
                    LiveCurveStatus.FAILED,
                    problem,
                    canCalculate = refusal(session) == null && owner.canAnalyze(),
                    session = session,
                )
        } catch (_: android.database.sqlite.SQLiteException) {
            timing?.count(CurveCount.RETRIES)
            // No job/request exists if its initial frozen-revision read failed.
            if (busy && job == null)
                synchronized(actionGate) {
                    busy = false
                    if (selection == null) requested = target
                }
            retryAt = elapsed() + if (retryFailed) 5_000_000_000L else 1_000_000_000L
            retryFailed = true
            loadFailed = !busy
            if (!busy) updateCard(error = true)
            view =
                LiveCurveSnapshot(
                    LiveCurveStatus.RETRYING,
                    LiveCurveReason.STORAGE_BLOCKED,
                    session = session,
                )
        } catch (_: Exception) {
            job = null
            completion = null
            busy = false
            loadFailed = false
            synchronized(actionGate) { requested = null }
            problem = LiveCurveReason.ANALYSIS_ERROR
            updateCard(error = true)
            view =
                LiveCurveSnapshot(
                    LiveCurveStatus.FAILED,
                    problem,
                    canCalculate = refusal(session) == null && owner.canAnalyze(),
                    session = session,
                )
        }
    }
}

internal fun liveCurveText(context: android.content.Context, state: LiveCurveSnapshot): String {
    fun text(id: Int) = context.getString(id)
    val status =
        when (state.status) {
            LiveCurveStatus.LOADING -> R.string.local_sessions_loading
            LiveCurveStatus.READY -> R.string.live_curve_ready
            LiveCurveStatus.CALCULATING -> R.string.live_curve_calculating
            LiveCurveStatus.SAVED -> R.string.live_curve_saved
            LiveCurveStatus.PENDING -> R.string.live_curve_pending
            LiveCurveStatus.FAILED -> R.string.live_curve_failed
            LiveCurveStatus.RETRYING -> R.string.live_curve_retrying
            LiveCurveStatus.UNAVAILABLE ->
                if (state.reason == LiveCurveReason.SELECT_SESSION) R.string.local_sessions_select
                else R.string.live_curve_no_session
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
            LiveCurveReason.SELECT_SESSION -> R.string.local_sessions_select
            LiveCurveReason.ACTIVE_SESSION -> R.string.local_sessions_active
            LiveCurveReason.INTERRUPTED_SESSION -> R.string.canonical_interrupted
            LiveCurveReason.COLD_SESSION -> R.string.canonical_cold
            LiveCurveReason.AMBIGUOUS_SESSION -> R.string.local_sessions_ambiguous
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
    return listOf(
            text(R.string.live_curve_scope),
            state.session?.let { sessionText(context, it) },
            text(status),
            reason?.let(::text),
            details,
        )
        .filterNotNull()
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString("\n")
}
