package com.oxywatt.diagnostic

import android.content.Context
import android.os.SystemClock
import com.oxywatt.diagnostic.persistence.InputRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** The extension owns subscriptions; Activity access never starts live acquisition. */
internal object DiagnosticRuntime {
    private var model: DiagnosticCapture? = null
    private var run: LiveRun? = null
    private var restartBlocked = false

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

    @Synchronized
    fun storage(): LiveStorageSnapshot =
        (run?.snapshot() ?: LiveStorageSnapshot()).copy(restartBlocked = restartBlocked)

    @Synchronized fun readyForSource(): Boolean = run == null || run!!.finished

    @Synchronized
    fun startLive(
        context: Context,
        source: (LiveDiagnosticCoordinator, DiagnosticCapture, Long, () -> Unit) -> AutoCloseable,
    ): AutoCloseable {
        if (!readyForSource()) {
            restartBlocked = true
            return AutoCloseable {}
        }
        restartBlocked = false
        return LiveRun(context.applicationContext, capture(context), source).also {
            run = it
            it.start()
        }
    }

    private class LiveRun(
        private val context: Context,
        private val capture: DiagnosticCapture,
        private val source:
            (LiveDiagnosticCoordinator, DiagnosticCapture, Long, () -> Unit) -> AutoCloseable,
    ) : AutoCloseable {
        private val wake = Object()
        @Volatile private var requestedStop = false
        @Volatile private var handle: AutoCloseable? = null
        @Volatile private var owner: LiveDiagnosticCoordinator? = null
        @Volatile private var failure: String? = null
        @Volatile
        var finished = false
            private set

        fun snapshot(): LiveStorageSnapshot =
            failure?.let { LiveStorageSnapshot(status = LiveStorageStatus.FATAL, error = it) }
                ?: owner?.snapshot()
                ?: LiveStorageSnapshot()

        private fun signal() = synchronized(wake) { wake.notifyAll() }

        fun start() {
            Thread({ execute() }, "oxywatt-live-input").apply {
                isDaemon = true
                start()
            }
        }

        private fun execute() {
            var repository: InputRepository? = null
            try {
                val epoch = UUID.randomUUID()
                val origin = SystemClock.elapsedRealtimeNanos()
                val metadata =
                    JSONObject()
                        .put("version", 1)
                        .put("origin", "real-karoo-diagnostic")
                        .put("appVersion", BuildConfig.VERSION_NAME)
                        .put("codeRevision", BuildConfig.CODE_REVISION)
                        .put("dirty", BuildConfig.CODE_DIRTY)
                        .put("epoch", epoch.toString())
                        .put("ordering", "karoo-diagnostic-order-v1")
                        .put("retry", "karoo-diagnostic-retry-v1")
                        .put("firstRetryNanos", 1_000_000_000L)
                        .put("subsequentRetryNanos", 5_000_000_000L)
                        .put("storage", "karoo-diagnostic-storage-v1")
                        .put("maximumBytes", 32L * 1024 * 1024)
                        .put("silence", "karoo-diagnostic-silence-v1")
                        .put("silenceNanos", 5_000_000_000L)
                repository =
                    InputRepository(
                        context,
                        "oxywatt-live-diagnostic.db",
                        32L * 1024 * 1024,
                        metadata.toString(),
                    )
                val coordinator =
                    LiveDiagnosticCoordinator(
                        repository,
                        epoch,
                        { SystemClock.elapsedRealtimeNanos() - origin },
                    )
                owner = coordinator
                var started = false
                while (true) {
                    if (requestedStop || coordinator.shouldStopSource) handle?.close()
                    coordinator.pump()
                    if (!started && coordinator.ready && !requestedStop) {
                        started = true
                        handle = source(coordinator, capture, origin, ::signal)
                        if (requestedStop) handle?.close()
                    }
                    if (requestedStop && !started) coordinator.stopped()
                    if (coordinator.finished) break
                    // One reusable waiter; notifications never retain events or enqueue tasks.
                    val delay = coordinator.waitMillis()
                    synchronized(wake) { wake.wait(delay) }
                }
            } catch (error: Exception) {
                failure = error.javaClass.simpleName.take(80)
            } finally {
                try {
                    handle?.close()
                } finally {
                    try {
                        repository?.close()
                    } finally {
                        finished = true
                    }
                }
            }
        }

        override fun close() {
            requestedStop = true
            handle?.close()
            signal()
        }
    }
}
