package com.oxywatt.diagnostic

import android.content.Context
import java.io.File
import java.util.UUID

/** One explicitly staged maintenance file on the source worker, before live startup. */
internal object FitImportMaintenance {
    private fun prefs(context: Context) =
        context.getSharedPreferences("fit-import-maintenance-v1", 0)

    fun statusResource(context: Context): Int =
        when (prefs(context).getString("status", "NONE")) {
            "RUNNING" -> R.string.fit_import_running
            "SAVED" -> R.string.fit_import_saved
            "FAILED" -> R.string.fit_import_failed
            else -> R.string.fit_import_none
        }

    fun run(context: Context) {
        val parent = File(context.filesDir, "fit-history")
        val pending = File(parent, "pending")
        val working = File(parent, "working")
        // A previous process died during maintenance: retain input and do not retry silently.
        if (working.exists()) {
            prefs(context).edit().putString("status", "FAILED").commit()
            return
        }
        if (!pending.isDirectory) return
        try {
            check(pending.renameTo(working))
            check(prefs(context).edit().putString("status", "RUNNING").commit())
            FitHistoryImporter(context).importArchive(working)
            val settings = HistorySettings(context)
            settings.write(settings.read().copy(source = HistorySource.FIT))
            check(working.renameTo(File(parent, "completed-${UUID.randomUUID()}")))
            check(prefs(context).edit().putString("status", "SAVED").commit())
        } catch (_: Exception) {
            // No private exception text/values in UI or logs; input remains owner-accessible.
            prefs(context).edit().putString("status", "FAILED").commit()
        }
    }
}
