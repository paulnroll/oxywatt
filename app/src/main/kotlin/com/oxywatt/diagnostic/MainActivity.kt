package com.oxywatt.diagnostic

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.oxywatt.core.collection.InterruptionReason
import com.oxywatt.core.collection.Metric
import com.oxywatt.core.collection.MissingReason
import com.oxywatt.core.collection.RecordingState
import com.oxywatt.core.collection.SessionOrigin
import com.oxywatt.core.collection.SessionPhase
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var capture: DiagnosticCapture
    private lateinit var status: TextView
    private lateinit var report: TextView
    private lateinit var storage: TextView
    private lateinit var curve: TextView
    private lateinit var timing: TextView
    private lateinit var calculate: Button
    private lateinit var sessionStatus: TextView
    private lateinit var sessionRows: LinearLayout
    private lateinit var refreshSessions: Button
    private lateinit var historyControls: HistoryControls
    private var historySubscription: AutoCloseable? = null
    private lateinit var savedFieldControls: SavedFieldControls
    private var renderedCards: List<LocalSessionCard>? = null
    private var renderedSelection: java.util.UUID? = null
    private var renderedEnabled = false
    private val metrics = mutableMapOf<Metric, TextView>()
    private val refresh =
        object : Runnable {
            override fun run() {
                render()
                handler.postDelayed(this, 500)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capture = DiagnosticRuntime.capture(applicationContext)
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
            }
        fun line(size: Float = 17f) =
            TextView(this).apply {
                textSize = size
                setPadding(0, 7, 0, 7)
                root.addView(this)
            }
        line(24f).setText(R.string.diagnostic_live_title)
        status = line()
        for (metric in Metric.entries) metrics[metric] = line(21f)
        storage = line()
        line(21f).setText(R.string.local_sessions_title)
        refreshSessions =
            Button(this).apply {
                setText(R.string.local_sessions_refresh)
                setOnClickListener { DiagnosticRuntime.refreshSessions() }
                root.addView(this)
            }
        sessionStatus = line()
        sessionRows =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                root.addView(this)
            }
        calculate =
            Button(this).apply {
                setText(R.string.live_curve_calculate)
                setOnClickListener { DiagnosticRuntime.calculateCurve() }
                root.addView(this)
            }
        curve = line()
        savedFieldControls = SavedFieldControls(this)
        root.addView(savedFieldControls)
        historyControls = HistoryControls(this)
        root.addView(historyControls)
        timing = line(15f)
        report = line()
        line(15f).setText(R.string.diagnostic_report_scope)
        line(14f).text =
            getString(
                R.string.diagnostic_build,
                BuildConfig.VERSION_NAME,
                if (BuildConfig.CODE_DIRTY)
                    getString(R.string.diagnostic_dirty_build, BuildConfig.CODE_REVISION.take(7))
                else BuildConfig.CODE_REVISION.take(7),
            )
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        historySubscription = DiagnosticRuntime.subscribeHistory()
        handler.post(refresh)
    }

    override fun onPause() {
        historySubscription?.close()
        historySubscription = null
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun renderSessions(snapshot: LocalSessionsSnapshot, selected: java.util.UUID?) {
        refreshSessions.isEnabled = snapshot.canChange
        sessionStatus.text =
            buildList {
                    if (snapshot.unavailable) add(getString(R.string.local_sessions_unavailable))
                    else {
                        if (snapshot.error) add(getString(R.string.local_sessions_error))
                        if (snapshot.loading) add(getString(R.string.local_sessions_loading))
                        if (!snapshot.loading && !snapshot.error && snapshot.cards.isEmpty())
                            add(getString(R.string.local_sessions_empty))
                        add(getString(R.string.local_sessions_revision, snapshot.revision))
                        if (snapshot.truncated) add(getString(R.string.local_sessions_truncated))
                        if (snapshot.dirty) add(getString(R.string.local_sessions_dirty))
                        if (selected != null && snapshot.cards.none { it.session.id == selected })
                            add(getString(R.string.local_sessions_outside))
                    }
                }
                .joinToString("\n")
        if (
            renderedCards == snapshot.cards &&
                renderedSelection == selected &&
                renderedEnabled == snapshot.canChange
        )
            return
        renderedCards = snapshot.cards
        renderedSelection = selected
        renderedEnabled = snapshot.canChange
        sessionRows.removeAllViews()
        snapshot.cards.forEachIndexed { index, card ->
            sessionRows.addView(
                Button(this).apply {
                    tag = card.session.id
                    text =
                        getString(
                            R.string.local_sessions_row,
                            index + 1,
                            if (card.session.id == selected)
                                getString(R.string.local_sessions_selected)
                            else "",
                            sessionCardText(this@MainActivity, card),
                        )
                    isAllCaps = false
                    isEnabled = snapshot.canChange
                    setOnClickListener { DiagnosticRuntime.selectCurve(card.session.id) }
                }
            )
        }
    }

    private fun render() {
        val curveState = DiagnosticRuntime.curve()
        renderSessions(DiagnosticRuntime.sessions(), curveState.session?.id)
        savedFieldControls.render(
            DiagnosticRuntime.savedField(),
            curveState,
            !DiagnosticRuntime.readyForSource(),
        )
        historyControls.render(DiagnosticRuntime.history())
        curve.text = liveCurveText(this, curveState)
        val timingText = curveTimingText(this, DiagnosticRuntime.curveTiming())
        if (timing.text.toString() != timingText) timing.text = timingText
        calculate.isEnabled = curveState.canCalculate
        calculate.setText(
            if (curveState.status in setOf(LiveCurveStatus.PENDING, LiveCurveStatus.FAILED))
                R.string.live_curve_retry
            else R.string.live_curve_calculate
        )
        val state = capture.snapshot()
        val saved = DiagnosticRuntime.storage()
        val storageLabel =
            getString(
                when (saved.status) {
                    LiveStorageStatus.WAITING -> R.string.canonical_waiting
                    LiveStorageStatus.COLLECTING -> R.string.canonical_collecting
                    LiveStorageStatus.SAVED -> R.string.canonical_saved
                    LiveStorageStatus.RETRYING -> R.string.canonical_retrying
                    LiveStorageStatus.FATAL -> R.string.canonical_fatal
                    LiveStorageStatus.DRAINED -> R.string.canonical_drained
                }
            )
        storage.text =
            getString(
                R.string.canonical_line,
                storageLabel,
                saved.received,
                saved.committed,
                saved.pending,
                saved.bytes,
                saved.loss,
                saved.rejected,
                saved.revision,
                saved.receipt?.utc?.toString() ?: getString(R.string.canonical_none),
                getString(
                    when (saved.session?.phase) {
                        SessionPhase.RECORDING -> R.string.diagnostic_state_recording
                        SessionPhase.PAUSED -> R.string.diagnostic_state_paused
                        SessionPhase.ENDED_LOCALLY -> R.string.canonical_ended
                        SessionPhase.INTERRUPTED -> R.string.canonical_interrupted
                        null -> R.string.canonical_none
                    }
                ),
                getString(
                    when (saved.session?.origin) {
                        SessionOrigin.OBSERVED_START -> R.string.canonical_observed
                        SessionOrigin.COLD_ATTACH -> R.string.canonical_cold
                        SessionOrigin.RECOVERY_FRAGMENT -> R.string.canonical_fragment
                        null -> R.string.canonical_none
                    }
                ),
                getString(
                    when (saved.session?.interruptionReason) {
                        InterruptionReason.PROCESS_RESTART ->
                            R.string.canonical_reason_process_restart
                        InterruptionReason.SOURCE_STOPPED ->
                            R.string.canonical_reason_source_stopped
                        InterruptionReason.SOURCE_FAILED -> R.string.canonical_reason_source_failed
                        InterruptionReason.UNKNOWN_STATE -> R.string.canonical_reason_unknown_state
                        InterruptionReason.TRANSPORT_LOSS ->
                            R.string.canonical_reason_transport_loss
                        InterruptionReason.INVALID_ORDER -> R.string.canonical_reason_invalid_order
                        InterruptionReason.SOURCE_CHANGED ->
                            R.string.canonical_reason_source_changed
                        InterruptionReason.CONTROL_OVERFLOW ->
                            R.string.canonical_reason_control_overflow
                        InterruptionReason.STORAGE_FAILURE ->
                            R.string.canonical_reason_storage_failure
                        null -> R.string.canonical_none
                    }
                ),
                saved.error ?: getString(R.string.canonical_none),
                getString(
                    if (saved.restartBlocked) R.string.canonical_restart_blocked
                    else R.string.canonical_restart_ready
                ),
            )
        val connection =
            getString(
                when (state.connected) {
                    true -> R.string.diagnostic_connected
                    false -> R.string.diagnostic_disconnected
                    null -> R.string.diagnostic_waiting_host
                }
            )
        val recording =
            getString(
                when (state.recording) {
                    RecordingState.UNKNOWN -> R.string.diagnostic_state_unknown
                    RecordingState.IDLE -> R.string.diagnostic_state_idle
                    RecordingState.RECORDING -> R.string.diagnostic_state_recording
                    RecordingState.PAUSED -> R.string.diagnostic_state_paused
                }
            )
        status.text =
            getString(
                R.string.diagnostic_state_line,
                connection,
                recording,
                getString(
                    if (state.sourceActive) R.string.diagnostic_source_running
                    else R.string.diagnostic_source_waiting
                ),
            )
        for ((metric, view) in metrics) {
            val reading = state.metrics[metric]
            val label =
                getString(
                    when (metric) {
                        Metric.POWER_W -> R.string.diagnostic_power
                        Metric.HEART_RATE_BPM -> R.string.diagnostic_hr
                        Metric.CADENCE_RPM -> R.string.diagnostic_cadence
                    }
                )
            val unit =
                getString(
                    when (metric) {
                        Metric.POWER_W -> R.string.diagnostic_watts
                        Metric.HEART_RATE_BPM -> R.string.diagnostic_bpm
                        Metric.CADENCE_RPM -> R.string.diagnostic_rpm
                    }
                )
            val value = reading?.value?.let { String.format(Locale.US, "%.0f", it) }
            val display =
                if (value != null) getString(R.string.diagnostic_value, value, unit)
                else
                    getString(
                        when (reading?.reason) {
                            MissingReason.DROPOUT -> R.string.diagnostic_searching
                            MissingReason.INVALID_SOURCE_VALUE -> R.string.diagnostic_invalid
                            MissingReason.UNAVAILABLE -> R.string.diagnostic_unavailable
                            null -> R.string.diagnostic_waiting_sensor
                        }
                    )
            val age =
                reading?.ageNanos?.let { getString(R.string.diagnostic_age, it / 1_000_000_000.0) }
                    ?: getString(R.string.diagnostic_age_unknown)
            view.text =
                getString(
                    R.string.diagnostic_metric_line,
                    label,
                    display,
                    age,
                    reading?.callbacks ?: 0,
                )
        }
        val reportState =
            getString(
                when (state.reportStatus) {
                    DiagnosticReportStatus.WAITING -> R.string.diagnostic_report_waiting
                    DiagnosticReportStatus.SAVED -> R.string.diagnostic_report_saved
                    DiagnosticReportStatus.LIMIT_REACHED -> R.string.diagnostic_report_limit
                    DiagnosticReportStatus.STORAGE_ERROR -> R.string.diagnostic_report_error
                    DiagnosticReportStatus.INCOMPLETE -> R.string.diagnostic_report_incomplete
                }
            )
        report.text =
            getString(
                R.string.diagnostic_report_line,
                reportState,
                state.savedBytes,
                state.droppedEvents,
            )
    }
}
