package com.oxywatt.diagnostic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.min

/** Bounded scatter plot. Missing entries have no coordinates; no line implies interpolation. */
internal object HistoryRenderer {
    fun render(context: Context, snapshot: HistorySnapshot, config: ViewConfig): RenderedField {
        val w = config.viewSize.first.coerceAtLeast(1)
        val h = config.viewSize.second.coerceAtLeast(1)
        val scale = min(1f, min(512f / w, 512f / h))
        val width = (w * scale).toInt().coerceIn(1, 512)
        val height = (h * scale).toInt().coerceIn(1, 512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dark = snapshot.config.appearance == FieldAppearance.DARK
        val bg = if (dark) Color.rgb(16, 38, 43) else Color.WHITE
        val ink = if (dark) Color.rgb(243, 248, 247) else Color.rgb(17, 44, 49)
        val accent = if (dark) Color.rgb(97, 223, 185) else Color.rgb(8, 127, 114)
        canvas.drawColor(bg)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
        val body =
            (android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                    11f,
                    context.resources.displayMetrics,
                ) * scale)
                .coerceAtLeast(8f)
        val pad = body
        val available = width - pad * 2
        fun fits(value: String, size: Float = body): Boolean {
            paint.textSize = size
            return paint.measureText(value) <= available
        }
        fun text(value: String, y: Float, x: Float = pad, size: Float = body, color: Int = ink) {
            paint.color = color
            paint.textSize = size
            canvas.drawText(value, x, y, paint)
        }
        val points = historyPoints(snapshot)
        val header =
            context.getString(
                R.string.history_compact_header,
                fieldDuration(context, snapshot.config.duration),
            )
        val range = historyRange(context, snapshot)
        val count = context.getString(R.string.history_points, points.size)
        val bound =
            context.getString(
                if (snapshot.truncated) R.string.history_truncated else R.string.history_bound
            )
        val message = historyMessage(context, snapshot)
        val description =
            historySummary(context, snapshot) + "\n" + historyRows(context, snapshot, true)
        if (height < body * 8 || !fits(header) || !fits(range)) {
            val enlarge = context.getString(R.string.field_enlarge)
            text(
                enlarge,
                height / 2f,
                2f,
                min(body, (width - 4f).coerceAtLeast(1f) / enlarge.length),
            )
            return RenderedField(bitmap, enlarge + ". " + description, FieldLayout.ENLARGE)
        }
        text(header, body * 2, color = accent)
        text(count, body * 3.5f)
        text(range, height - body * 3.5f)
        text(context.getString(R.string.history_utc), height - body * 2)
        // Never cut an error/bound message. Wrap within the bounded available text area.
        fun wrap(value: String): List<String> {
            val lines = mutableListOf<String>()
            var current = ""
            for (word in value.split(' ')) {
                val next = if (current.isEmpty()) word else "$current $word"
                if (current.isNotEmpty() && !fits(next)) {
                    lines.add(current)
                    current = word
                } else current = next
            }
            if (current.isNotEmpty()) lines.add(current)
            return lines
        }
        val boundSize = min(body, available / bound.length.coerceAtLeast(1) * 1.7f)
        if (fits(bound, boundSize)) text(bound, height - body * .5f, size = boundSize)
        else {
            canvas.drawColor(bg)
            text(context.getString(R.string.field_enlarge), height / 2f)
            return RenderedField(bitmap, description, FieldLayout.ENLARGE)
        }
        if (snapshot.status != HistoryStatus.READY || points.isEmpty()) {
            val lines = wrap(message)
            if (lines.any { !fits(it) } || height < body * (9 + lines.size * 1.3f)) {
                canvas.drawColor(bg)
                text(context.getString(R.string.field_enlarge), height / 2f)
                return RenderedField(bitmap, description, FieldLayout.ENLARGE)
            }
            lines.forEachIndexed { i, line -> text(line, body * (5.3f + i * 1.3f)) }
            return RenderedField(bitmap, description, FieldLayout.COMPACT)
        }
        val large = height >= body * 16 && width >= body * 24
        if (large) {
            val left = pad * 2.5f
            val right = width - pad * 2
            val top = body * 6
            val bottom = height - body * 5.5f
            paint.color = ink
            paint.strokeWidth = 1f
            canvas.drawLine(left, top, left, bottom, paint)
            canvas.drawLine(left, bottom, right, bottom, paint)
            text(context.getString(R.string.history_y_axis), body * 5, size = body * .85f)
            val maximum = points.maxOf { it.entry.watts!! }.coerceAtLeast(1.0)
            text(
                context.getString(R.string.history_field_value, maximum),
                top - body * .3f,
                left,
                body * .85f,
            )
            text(
                context.getString(R.string.history_field_value, 0.0),
                bottom - body * .4f,
                left,
                body * .85f,
            )
            val dates = snapshot.entries.map { it.date }
            val tickFormat =
                java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH)
                    .withZone(java.time.ZoneOffset.UTC)
            val firstTick = tickFormat.format(dates.min())
            val lastTick = tickFormat.format(dates.max())
            text(firstTick, bottom + body * 1.2f, left, body * .8f)
            paint.textSize = body * .8f
            text(lastTick, bottom + body * 1.2f, right - paint.measureText(lastTick), body * .8f)
            val groups = points.groupBy { it.entry.date to it.entry.watts }
            for (point in points) {
                val duplicates = groups.getValue(point.entry.date to point.entry.watts)
                val ordinal = duplicates.indexOf(point)
                val offset =
                    if (duplicates.size == 1) 0f
                    else (ordinal - (duplicates.size - 1) / 2f) * body * .4f
                val x = (left + (right - left) * point.x.toFloat() + offset).coerceIn(left, right)
                val y = top + (bottom - top) * point.y.toFloat()
                paint.color = accent
                canvas.drawCircle(x, y, body * .24f, paint)
            }
        }
        return RenderedField(
            bitmap,
            description,
            if (large) FieldLayout.LARGE else FieldLayout.COMPACT,
        )
    }
}
