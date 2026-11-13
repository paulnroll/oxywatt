package com.oxywatt.diagnostic

import android.content.Context
import com.oxywatt.diagnostic.persistence.*
import java.util.Locale

/** Read-only projection of the current/last attempt, independent of selected saved provenance. */
internal fun curveTimingText(context: Context, snapshot: CurveTimingSnapshot?): String {
    fun text(id: Int) = context.getString(id)
    fun duration(label: Int, nanos: Long) =
        context.getString(
            R.string.curve_timing_duration,
            text(label),
            String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0),
        )
    return buildList {
            add(text(R.string.curve_timing_title))
            add(text(R.string.curve_timing_scope))
            if (snapshot == null) {
                add(text(R.string.curve_timing_none))
                return@buildList
            }
            add(text(R.string.curve_timing_limits))
            val outcome =
                when (snapshot.outcome) {
                    CurveOutcome.RUNNING -> R.string.curve_timing_outcome_running
                    CurveOutcome.SAVED -> R.string.curve_timing_outcome_saved
                    CurveOutcome.FAILED -> R.string.curve_timing_outcome_failed
                    CurveOutcome.CANCELLED -> R.string.curve_timing_outcome_cancelled
                    CurveOutcome.DISCARDED -> R.string.curve_timing_outcome_discarded
                    CurveOutcome.STOPPED -> R.string.curve_timing_outcome_stopped
                }
            add(context.getString(R.string.curve_timing_outcome, text(outcome)))
            add(
                text(
                    if (snapshot.incomplete) R.string.curve_timing_incomplete
                    else R.string.curve_timing_complete
                )
            )
            add(
                context.getString(
                    R.string.curve_timing_pacing,
                    snapshot.pacingPolicy,
                    snapshot.maximumTurnsBetweenWaits.toString(),
                )
            )
            add(duration(R.string.curve_timing_elapsed, snapshot.elapsedNanos))
            add(duration(R.string.curve_timing_residual, snapshot.residualNanos))
            listOf(
                    CurveWork.CONTROL to R.string.curve_timing_work_control,
                    CurveWork.HASH_PAGE to R.string.curve_timing_work_hash_page,
                    CurveWork.HASH to R.string.curve_timing_work_hash,
                    CurveWork.REQUEST to R.string.curve_timing_work_request,
                    CurveWork.REPLAY_PAGE to R.string.curve_timing_work_replay_page,
                    CurveWork.REPLAY to R.string.curve_timing_work_replay,
                    CurveWork.COMPLETE to R.string.curve_timing_work_complete,
                    CurveWork.READBACK to R.string.curve_timing_work_readback,
                    CurveWork.CANONICAL to R.string.curve_timing_work_canonical,
                    CurveWork.WAIT to R.string.curve_timing_work_wait,
                    CurveWork.BACKOFF to R.string.curve_timing_work_backoff,
                )
                .forEach { (key, label) -> add(duration(label, snapshot.work.getValue(key))) }
            listOf(
                    CurveCount.STEPS to R.string.curve_timing_count_steps,
                    CurveCount.COMPLETED_STEPS to R.string.curve_timing_count_completed_steps,
                    CurveCount.HASH_SCANNED to R.string.curve_timing_count_hash_scanned,
                    CurveCount.HASH_SELECTED to R.string.curve_timing_count_hash_selected,
                    CurveCount.HASH_PAGES to R.string.curve_timing_count_hash_pages,
                    CurveCount.REPLAY_SCANNED to R.string.curve_timing_count_replay_scanned,
                    CurveCount.REPLAY_SELECTED to R.string.curve_timing_count_replay_selected,
                    CurveCount.REPLAY_PAGES to R.string.curve_timing_count_replay_pages,
                    CurveCount.WAITS to R.string.curve_timing_count_waits,
                    CurveCount.RETRIES to R.string.curve_timing_count_retries,
                )
                .forEach { (key, label) ->
                    add(
                        context.getString(
                            R.string.curve_timing_count,
                            text(label),
                            snapshot.counts.getValue(key).toString(),
                        )
                    )
                }
            listOf(
                    CurveMark.FIRST_WORKER to R.string.curve_timing_mark_first_worker,
                    CurveMark.HASHED to R.string.curve_timing_mark_hashed,
                    CurveMark.REQUESTED to R.string.curve_timing_mark_requested,
                    CurveMark.REPLAYED to R.string.curve_timing_mark_replayed,
                    CurveMark.PUBLISHED to R.string.curve_timing_mark_published,
                )
                .forEach { (key, label) ->
                    add(
                        snapshot.marks[key]?.let { duration(label, it) }
                            ?: context.getString(R.string.curve_timing_absent, text(label))
                    )
                }
        }
        .joinToString("\n")
}
