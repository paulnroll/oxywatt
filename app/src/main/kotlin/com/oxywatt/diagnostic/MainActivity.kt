package com.oxywatt.diagnostic

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.oxywatt.core.collection.Metric
import com.oxywatt.core.collection.MissingReason
import com.oxywatt.core.collection.RecordingState
import java.util.Locale

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var capture: DiagnosticCapture
    private lateinit var status: TextView
    private lateinit var report: TextView
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
        report = line()
        line(15f).setText(R.string.diagnostic_report_scope)
        line(14f).text =
            getString(
                R.string.diagnostic_build,
                BuildConfig.VERSION_NAME,
                BuildConfig.CODE_REVISION.take(7),
            )
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun render() {
        val state = capture.snapshot()
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
