package com.oxywatt.diagnostic

import android.content.Context
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class HistoryPoint(val entry: HistoryEntry, val x: Double, val y: Double)

/** Geometry only: raw values and identity are preserved; no fit or smoothing. */
internal fun historyPoints(snapshot: HistorySnapshot): List<HistoryPoint> {
    if (snapshot.status != HistoryStatus.READY || snapshot.entries.isEmpty()) return emptyList()
    val first = snapshot.entries.minOf { it.date }
    val last = snapshot.entries.maxOf { it.date }
    fun seconds(a: Instant, b: Instant): Double =
        java.time.Duration.between(a, b).let { it.seconds.toDouble() + it.nano / 1e9 }
    val span = seconds(first, last)
    val maximum = snapshot.entries.mapNotNull { it.watts }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    return snapshot.entries
        .filter { it.reason == HistoryReason.VALUE && it.watts != null }
        .map {
            HistoryPoint(
                it,
                if (span == 0.0) 0.5 else seconds(first, it.date) / span,
                1.0 - it.watts!! / maximum,
            )
        }
}

internal fun historyDate(date: Instant): String =
    DateTimeFormatter.ofPattern("d MMM uuuu HH:mm:ss", Locale.ENGLISH)
        .withZone(ZoneOffset.UTC)
        .format(date)

internal fun historyReason(context: Context, reason: HistoryReason): String =
    context.getString(
        when (reason) {
            HistoryReason.VALUE -> R.string.field_best
            HistoryReason.INCOMPLETE -> R.string.history_incomplete
            HistoryReason.INCOMPATIBLE -> R.string.history_incompatible
            HistoryReason.NO_RESULT -> R.string.history_no_result
            HistoryReason.PENDING -> R.string.field_pending
            HistoryReason.CALCULATING -> R.string.history_calculating
            HistoryReason.FAILED -> R.string.field_failed
            HistoryReason.UNAVAILABLE -> R.string.field_unavailable
            HistoryReason.REMOVED -> R.string.history_removed
            HistoryReason.LIMITED -> R.string.field_limited
            HistoryReason.NO_WINDOW -> R.string.field_missing
        }
    )

internal fun historyMessage(context: Context, snapshot: HistorySnapshot): String =
    context.getString(
        when (snapshot.status) {
            HistoryStatus.NO_WORKER -> R.string.history_no_worker
            HistoryStatus.LOADING -> R.string.history_loading
            HistoryStatus.READ_ERROR -> R.string.history_error
            HistoryStatus.SETTINGS_ERROR -> R.string.history_settings_error
            HistoryStatus.READY ->
                if (historyPoints(snapshot).isEmpty()) R.string.history_empty
                else R.string.history_utc
        }
    )

internal fun historyRange(context: Context, snapshot: HistorySnapshot): String {
    val dates = snapshot.entries.map { it.date }
    val format = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH).withZone(ZoneOffset.UTC)
    return if (dates.isEmpty()) context.getString(R.string.field_dash)
    else
        context.getString(
            R.string.history_range,
            format.format(dates.min()),
            format.format(dates.max()),
        )
}

internal fun historySummary(context: Context, snapshot: HistorySnapshot): String = buildList {
    add(
        context.getString(R.string.history_header, fieldDuration(context, snapshot.config.duration))
    )
    add(historyMessage(context, snapshot))
    add(context.getString(R.string.history_bound))
    if (snapshot.truncated) add(context.getString(R.string.history_truncated))
    if (snapshot.status == HistoryStatus.READY) {
        val entries = snapshot.entries
        add(
            context.getString(
                R.string.history_count,
                entries.count { it.reason == HistoryReason.VALUE },
                entries.count { it.reason == HistoryReason.INCOMPLETE },
                entries.count { it.reason == HistoryReason.INCOMPATIBLE },
                entries.count {
                    it.reason !in
                        setOf(
                            HistoryReason.VALUE,
                            HistoryReason.INCOMPLETE,
                            HistoryReason.INCOMPATIBLE,
                        )
                },
            )
        )
        add(historyRange(context, snapshot))
    }
    snapshot.refreshedAt?.let {
        add(context.getString(R.string.history_refreshed, historyDate(it)))
    }
}
    .joinToString("\n")

internal fun historyRows(
    context: Context,
    snapshot: HistorySnapshot,
    wholeWatts: Boolean = false,
): String =
    if (snapshot.status != HistoryStatus.READY) ""
    else
        snapshot.entries
            .mapIndexed { index, entry ->
                context.getString(
                    R.string.history_row,
                    index + 1,
                    historyDate(entry.date),
                    entry.session.toString().take(8),
                    entry.watts
                        ?.takeIf { entry.reason == HistoryReason.VALUE }
                        ?.let {
                            context.getString(
                                if (wholeWatts) R.string.history_field_value
                                else R.string.history_value,
                                it,
                            )
                        } ?: historyReason(context, entry.reason),
                )
            }
            .joinToString("\n")
