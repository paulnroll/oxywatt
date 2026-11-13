package com.oxywatt.diagnostic

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.oxywatt.core.power.PowerCurveEngine
import io.hammerhead.karooext.models.ViewConfig

/** UI projects snapshots and emits commands only; no storage or acquisition ownership. */
internal class HistoryControls(context: Context) : LinearLayout(context) {
    private val duration = Spinner(context)
    private val appearance = Spinner(context)
    private val refresh = Button(context)
    private val summary = TextView(context)
    private val rows = TextView(context)
    private val chart =
        object : ImageView(context) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                drawHistory()
            }
        }
    private var current: HistorySnapshot? = null
    private var restored = false

    init {
        orientation = VERTICAL
        fun label(id: Int) {
            addView(
                TextView(context).apply {
                    setText(id)
                    setPadding(0, 12, 0, 6)
                }
            )
        }
        label(R.string.history_title)
        label(R.string.history_duration)
        duration.contentDescription = context.getString(R.string.history_duration)
        duration.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                PowerCurveEngine.DURATIONS.map { fieldDuration(context, it) },
            )
        duration.setSelection(PowerCurveEngine.DURATIONS.indexOf(300))
        addView(duration)
        label(R.string.history_appearance)
        appearance.contentDescription = context.getString(R.string.history_appearance)
        appearance.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    context.getString(R.string.field_light),
                    context.getString(R.string.field_dark),
                ),
            )
        addView(appearance)
        refresh.setText(R.string.history_refresh)
        refresh.setOnClickListener {
            DiagnosticRuntime.refreshHistory(
                HistoryConfig(
                    PowerCurveEngine.DURATIONS[duration.selectedItemPosition],
                    FieldAppearance.entries[appearance.selectedItemPosition],
                )
            )
        }
        addView(refresh)
        addView(summary)
        addView(
            chart,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.density * 300).toInt(),
            ),
        )
        addView(rows)
        label(R.string.history_hint)
        label(R.string.history_clock)
        label(R.string.history_cohort)
    }

    fun render(snapshot: HistorySnapshot) {
        val enabled = snapshot.status != HistoryStatus.NO_WORKER
        refresh.isEnabled = enabled
        duration.isEnabled = enabled
        appearance.isEnabled = enabled
        if (
            !restored && snapshot.status !in setOf(HistoryStatus.LOADING, HistoryStatus.NO_WORKER)
        ) {
            duration.setSelection(PowerCurveEngine.DURATIONS.indexOf(snapshot.config.duration))
            appearance.setSelection(snapshot.config.appearance.ordinal)
            restored = true
        }
        if (current == snapshot) return
        current = snapshot
        summary.text = historySummary(context, snapshot)
        rows.text = historyRows(context, snapshot)
        drawHistory()
    }

    private fun drawHistory() {
        val snapshot = current ?: return
        if (chart.width <= 0 || chart.height <= 0) return
        val rendered =
            HistoryRenderer.render(
                context,
                snapshot,
                ViewConfig(1 to 1, chart.width to chart.height, 20),
            )
        chart.setImageBitmap(rendered.bitmap)
        chart.contentDescription = rendered.description
    }
}
