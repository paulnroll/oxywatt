package com.oxywatt.diagnostic

import android.content.Context
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ln

internal data class FieldChartPoint(
    val x: Double,
    val y: Double,
    val connected: Boolean,
    val seconds: Int,
)

/** Presentation coordinates only; numeric field values always come directly from stored results. */
internal fun fieldChart(points: List<FieldPower>): List<FieldChartPoint> {
    val maximum = (points.mapNotNull { it.watts }.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    var previous = false
    return buildList {
        points.forEach {
            val watts = it.watts
            if (watts == null) previous = false
            else {
                add(
                    FieldChartPoint(
                        ln(it.seconds / 5.0) / ln(720.0),
                        1 - watts / maximum,
                        previous,
                        it.seconds,
                    )
                )
                previous = true
            }
        }
    }
}

internal fun fieldDuration(context: Context, seconds: Int): String =
    if (seconds < 60) context.getString(R.string.field_seconds, seconds)
    else context.getString(R.string.field_minutes, seconds / 60)

internal fun fieldDate(context: Context, snapshot: SavedFieldSnapshot): String =
    snapshot.receiptDate?.let {
        DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC)
            .format(it)
    } ?: context.getString(R.string.field_dash)

internal fun fieldMessage(context: Context, snapshot: SavedFieldSnapshot): String {
    val point = snapshot.points.find { it.seconds == snapshot.binding?.duration }
    return context.getString(
        when (snapshot.status) {
            SavedFieldStatus.SETUP -> R.string.field_setup
            SavedFieldStatus.LOADING -> R.string.field_loading
            SavedFieldStatus.REMOVED -> R.string.field_removed
            SavedFieldStatus.PENDING -> R.string.field_pending
            SavedFieldStatus.FAILED -> R.string.field_failed
            SavedFieldStatus.UNAVAILABLE -> R.string.field_unavailable
            SavedFieldStatus.INVALID -> R.string.field_invalid
            SavedFieldStatus.READ_ERROR -> R.string.field_read_error
            SavedFieldStatus.SETTINGS_ERROR -> R.string.field_settings_error
            SavedFieldStatus.NO_WORKER -> R.string.field_no_worker
            SavedFieldStatus.SAVED ->
                when {
                    snapshot.limited || point?.resourceLimited == true -> R.string.field_limited
                    point?.watts == null -> R.string.field_missing
                    else -> R.string.field_best
                }
        }
    )
}

internal fun fieldValue(context: Context, snapshot: SavedFieldSnapshot): String =
    snapshot.points
        .find { it.seconds == snapshot.binding?.duration }
        ?.watts
        ?.takeIf { snapshot.status == SavedFieldStatus.SAVED }
        ?.let { String.format(Locale.US, "%.0f", it) } ?: context.getString(R.string.field_dash)
