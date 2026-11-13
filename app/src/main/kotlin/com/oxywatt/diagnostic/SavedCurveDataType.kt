package com.oxywatt.diagnostic

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.ViewConfig
import java.util.concurrent.atomic.AtomicBoolean

internal class SavedCurveDataType : DataTypeImpl("oxywatt-diagnostic", TYPE), AutoCloseable {
    private val active = java.util.Collections.synchronizedSet(mutableSetOf<AutoCloseable>())
    private val closed = AtomicBoolean(false)

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val handler = Handler(Looper.getMainLooper())
        val cancelled = AtomicBoolean(false)
        val subscription = DiagnosticRuntime.subscribeSavedField()
        var previous: SavedFieldSnapshot? = null
        var release: (() -> Unit)? = null
        val update =
            object : Runnable {
                override fun run() {
                    if (cancelled.get()) return
                    try {
                        val current = DiagnosticRuntime.savedField()
                        if (current != previous) {
                            val rendered = SavedFieldRenderer.render(context, current, config)
                            if (!cancelled.get())
                                emitter.updateView(
                                    SavedFieldRenderer.remoteViews(context, rendered)
                                )
                            previous = current
                        }
                    } catch (error: Exception) {
                        // A failed/dead field host must not crash the acquisition process.
                        release?.invoke()
                        try {
                            emitter.onError(error)
                        } catch (_: Exception) {}
                        return
                    }
                    if (!cancelled.get()) handler.postDelayed(this, 1000)
                }
            }
        val cancellation =
            object : AutoCloseable {
                override fun close() {
                    if (cancelled.compareAndSet(false, true)) {
                        handler.removeCallbacks(update)
                        subscription.close()
                        previous = null
                        active.remove(this)
                    }
                }
            }
        active.add(cancellation)
        release = { cancellation.close() }
        emitter.setCancellable { cancellation.close() }
        if (closed.get()) cancellation.close()
        if (!cancelled.get()) handler.post(update)
    }

    override fun close() {
        closed.set(true)
        synchronized(active) { active.toList() }.forEach { it.close() }
    }

    companion object {
        const val TYPE = "saved-power-curve"
    }
}
