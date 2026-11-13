package com.oxywatt.diagnostic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.RemoteViews
import io.hammerhead.karooext.models.ViewConfig
import kotlin.math.min

internal enum class FieldLayout {
    LARGE,
    COMPACT,
    ENLARGE,
}

internal data class RenderedField(
    val bitmap: Bitmap,
    val description: String,
    val layout: FieldLayout,
)

/** Bounded bitmap inside an SDK RemoteViews ImageView. Contains no storage, source or analytics. */
internal object SavedFieldRenderer {
    fun render(context: Context, snapshot: SavedFieldSnapshot, config: ViewConfig): RenderedField {
        val hostWidth = config.viewSize.first.coerceAtLeast(1)
        val hostHeight = config.viewSize.second.coerceAtLeast(1)
        val scale = min(1f, min(512f / hostWidth, 512f / hostHeight))
        val width = (hostWidth * scale).toInt().coerceIn(1, 512)
        val height = (hostHeight * scale).toInt().coerceIn(1, 512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dark = snapshot.binding?.appearance == FieldAppearance.DARK
        val bg = Color.rgb(if (dark) 16 else 255, if (dark) 38 else 255, if (dark) 43 else 255)
        val ink = if (dark) Color.rgb(243, 248, 247) else Color.rgb(17, 44, 49)
        val muted = if (dark) Color.rgb(167, 191, 192) else Color.rgb(82, 108, 112)
        val accent = if (dark) Color.rgb(97, 223, 185) else Color.rgb(8, 127, 114)
        val grid = if (dark) Color.rgb(49, 75, 80) else Color.rgb(220, 231, 229)
        canvas.drawColor(bg)
        val metrics = context.resources.displayMetrics
        fun sp(value: Float) =
            android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP,
                value,
                metrics,
            )
        val body = (sp(11f) * scale).coerceAtLeast(8f)
        val pad = body
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun text(
            value: String,
            x: Float,
            y: Float,
            size: Float = body,
            color: Int = ink,
            bold: Boolean = false,
        ) {
            paint.color = color
            paint.textSize = size
            paint.typeface =
                if (bold) Typeface.create("sans-serif", Typeface.BOLD)
                else Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText(value, x, y, paint)
        }
        fun fits(value: String, size: Float, available: Float): Boolean {
            paint.textSize = size
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            return paint.measureText(value) <= available
        }
        val duration = fieldDuration(context, snapshot.binding?.duration ?: 300)
        val value = fieldValue(context, snapshot)
        val message = fieldMessage(context, snapshot)
        val date = fieldDate(context, snapshot)
        val fullHeader = context.getString(R.string.field_header, duration)
        val fullDate = context.getString(R.string.field_session_date, date)
        val description =
            context.getString(R.string.field_accessibility, fullHeader, value, fullDate, message)
        val available = width - 2 * pad
        val large = width >= body * 22 && height >= body * 15 && fits(fullDate, body, available)
        val header =
            if (large) fullHeader else context.getString(R.string.field_header_compact, duration)
        val footer = if (large) fullDate else date
        val footerStatus =
            if (
                snapshot.status == SavedFieldStatus.SAVED &&
                    message == context.getString(R.string.field_best)
            )
                context.getString(R.string.field_saved)
            else message
        if (height < body * 9 || !fits(header, body, available) || !fits(footer, body, available)) {
            val enlarge = context.getString(R.string.field_enlarge)
            // For physically tiny cells, let the host scale this accessible fallback.
            val size = min(body, (width - 4f).coerceAtLeast(1f) / enlarge.length)
            text(enlarge, 2f, (height / 2f).coerceAtLeast(size), size, muted)
            return RenderedField(bitmap, enlarge + ". " + description, FieldLayout.ENLARGE)
        }
        text(header, pad, pad + body, body, muted, true)
        var numberSize =
            if (large)
                (sp(config.textSize.toFloat()) * scale).coerceIn(
                    body * 3,
                    min(body * 6, height - body * 11).coerceAtLeast(body * 3),
                )
            else body * 3
        while (
            numberSize > body * 2 && !fits(value, numberSize, available - body * 2)
        ) numberSize -= 1
        if (!fits(value, numberSize, available - body * 2)) {
            text(context.getString(R.string.field_enlarge), pad, height / 2f, body, muted)
            return RenderedField(bitmap, description, FieldLayout.ENLARGE)
        }
        val baseline = if (large) pad + body * 2 + numberSize else pad + body * 1.8f + numberSize
        text(value, pad, baseline, numberSize, ink, true)
        paint.textSize = numberSize
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val unitX = pad + paint.measureText(value) + body * .5f
        text(context.getString(R.string.field_watts), unitX, baseline, body * 1.4f, muted)
        val dateY = height - pad - body * 1.4f
        text(footer, pad, dateY, body, muted)
        // Wrap the status inside the field; never silently cut a failure or limited-scan label.
        var statusSize = body
        while (statusSize > body * .8f && !fits(footerStatus, statusSize, available)) statusSize -=
            .5f
        if (fits(footerStatus, statusSize, available))
            text(footerStatus, pad, height - pad * .35f, statusSize, accent)
        else {
            // A long status needs more room than a compact cell can honestly provide.
            canvas.drawColor(bg)
            val words = footerStatus.split(' ')
            val lines = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (current.isNotEmpty() && !fits(candidate, body, available)) {
                    lines.add(current)
                    current = word
                } else current = candidate
            }
            if (current.isNotEmpty()) lines.add(current)
            text(header, pad, pad + body, body, muted, true)
            if (lines.size > 3 || lines.any { !fits(it, body, available) }) {
                text(context.getString(R.string.field_enlarge), pad, height / 2f, body, muted)
                return RenderedField(bitmap, description, FieldLayout.ENLARGE)
            }
            lines.forEachIndexed { i, line ->
                text(line, pad, pad + body * (3 + i * 1.3f), body, ink, true)
            }
            text(footer, pad, height - pad, body, muted)
            return RenderedField(bitmap, description, FieldLayout.COMPACT)
        }
        if (large) {
            val chartTop = baseline + body * 1.5f
            val chartBottom = dateY - body * 2.3f
            val chartLeft = pad + body * .5f
            val chartRight = width - pad - body * .5f
            paint.color = grid
            paint.strokeWidth = 1f
            canvas.drawLine(pad, dateY - body, width - pad, dateY - body, paint)
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paint)
            var previous: Pair<Float, Float>? = null
            for (point in fieldChart(snapshot.points)) {
                val x = chartLeft + (chartRight - chartLeft) * point.x.toFloat()
                val y = chartTop + (chartBottom - chartTop) * point.y.toFloat()
                paint.color = accent
                paint.strokeWidth = 2.2f * scale.coerceAtLeast(.5f)
                if (point.connected)
                    previous?.let { canvas.drawLine(it.first, it.second, x, y, paint) }
                canvas.drawCircle(
                    x,
                    y,
                    if (point.seconds == snapshot.binding?.duration) body * .3f else body * .15f,
                    paint,
                )
                if (point.seconds == snapshot.binding?.duration) {
                    paint.color = bg
                    canvas.drawCircle(x, y, body * .15f, paint)
                }
                previous = x to y
            }
            for (seconds in listOf(5, 60, 300, 3600)) {
                // Tick labels use resource-backed duration text too.
                val x =
                    chartLeft +
                        (chartRight - chartLeft) *
                            (kotlin.math.ln(seconds / 5.0) / kotlin.math.ln(720.0)).toFloat()
                val tick = fieldDuration(context, seconds)
                paint.textSize = body * .75f
                text(tick, x - paint.measureText(tick) / 2, chartBottom + body, body * .75f, muted)
            }
        }
        return RenderedField(
            bitmap,
            description,
            if (large) FieldLayout.LARGE else FieldLayout.COMPACT,
        )
    }

    fun remoteViews(context: Context, rendered: RenderedField): RemoteViews =
        RemoteViews(context.packageName, R.layout.saved_curve_field).apply {
            setImageViewBitmap(R.id.saved_curve_image, rendered.bitmap)
            setContentDescription(R.id.saved_curve_image, rendered.description)
        }
}
