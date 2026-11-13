package com.oxywatt.diagnostic

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.oxywatt.core.power.PowerCurveEngine
import com.oxywatt.diagnostic.persistence.AnalysisStatus

/** Configuration emits commands only. Preference/database access stays on the existing worker. */
internal class SavedFieldControls(context: Context) : LinearLayout(context) {
    private val info = TextView(context)
    private val duration = Spinner(context)
    private val appearance = Spinner(context)
    private val apply = Button(context)
    private val clear = Button(context)
    private var selected: java.util.UUID? = null
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
        label(R.string.field_controls_title)
        addView(info)
        label(R.string.field_duration_label)
        duration.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                PowerCurveEngine.DURATIONS.map { fieldDuration(context, it) },
            )
        duration.setSelection(PowerCurveEngine.DURATIONS.indexOf(300))
        duration.contentDescription = context.getString(R.string.field_duration_label)
        addView(duration)
        label(R.string.field_appearance_label)
        appearance.adapter =
            ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    context.getString(R.string.field_light),
                    context.getString(R.string.field_dark),
                ),
            )
        appearance.contentDescription = context.getString(R.string.field_appearance_label)
        addView(appearance)
        apply.setText(R.string.field_bind)
        apply.setOnClickListener {
            selected?.let {
                DiagnosticRuntime.bindSavedField(
                    SavedFieldBinding(
                        it,
                        PowerCurveEngine.DURATIONS[duration.selectedItemPosition],
                        FieldAppearance.entries[appearance.selectedItemPosition],
                    )
                )
            }
        }
        addView(apply)
        clear.setText(R.string.field_clear)
        clear.setOnClickListener { DiagnosticRuntime.clearSavedField() }
        addView(clear)
        label(R.string.field_controls_hint)
    }

    fun render(
        snapshot: SavedFieldSnapshot,
        selection: LiveCurveSnapshot,
        workerAvailable: Boolean,
    ) {
        selected = selection.session?.id
        apply.isEnabled =
            workerAvailable &&
                selection.status == LiveCurveStatus.SAVED &&
                selection.saved?.status == AnalysisStatus.AVAILABLE
        clear.isEnabled = workerAvailable
        duration.isEnabled = workerAvailable
        appearance.isEnabled = workerAvailable
        if (
            !restored &&
                snapshot.status !in setOf(SavedFieldStatus.LOADING, SavedFieldStatus.NO_WORKER)
        ) {
            snapshot.binding?.let {
                duration.setSelection(PowerCurveEngine.DURATIONS.indexOf(it.duration))
                appearance.setSelection(it.appearance.ordinal)
            }
            restored = true
        }
        val binding = snapshot.binding
        info.text =
            if (binding == null) fieldMessage(context, snapshot)
            else
                context.getString(
                    R.string.field_configuration,
                    binding.session.toString(),
                    fieldDate(context, snapshot),
                    fieldDuration(context, binding.duration),
                    context.getString(
                        if (binding.appearance == FieldAppearance.LIGHT) R.string.field_light
                        else R.string.field_dark
                    ),
                    fieldMessage(context, snapshot),
                )
        snapshot.bindingError?.let {
            info.append(
                "\n" +
                    context.getString(
                        R.string.field_bind_error,
                        fieldMessage(context, snapshot.copy(status = it)),
                    )
            )
        }
    }
}
