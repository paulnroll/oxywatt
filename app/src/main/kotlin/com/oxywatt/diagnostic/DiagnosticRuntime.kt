package com.oxywatt.diagnostic

import android.content.Context
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** The extension owns subscriptions; the Activity only reads this process-scoped model. */
internal object DiagnosticRuntime {
    private var model: DiagnosticCapture? = null

    @Synchronized
    fun capture(context: Context): DiagnosticCapture {
        model?.let {
            return it
        }
        val capture =
            DiagnosticCapture(File(context.applicationContext.filesDir, "diagnostic-report.jsonl"))
        model = capture
        Executors.newSingleThreadScheduledExecutor { work ->
                Thread(work, "oxywatt-diagnostic-report").apply { isDaemon = true }
            }
            .scheduleWithFixedDelay({ capture.flush() }, 0, 1, TimeUnit.SECONDS)
        return capture
    }
}
