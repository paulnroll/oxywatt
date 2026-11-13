package com.oxywatt.diagnostic

import android.content.Context
import android.os.SystemClock
import com.oxywatt.diagnostic.persistence.AnalysisRepository
import com.oxywatt.diagnostic.persistence.CurveTimingSnapshot
import com.oxywatt.diagnostic.persistence.CurveWork
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

    @Synchronized
    fun curve(): LiveCurveSnapshot =
        run?.let { live ->
            val snapshot = live.curveSnapshot()
            if (live.finished) snapshot.copy(canCalculate = false) else snapshot
        } ?: LiveCurveSnapshot()

    @Synchronized fun curveTiming(): CurveTimingSnapshot? = run?.timingSnapshot()

    @Synchronized
    fun calculateCurve() {
        run?.requestCurve()
    }

    @Synchronized
    fun sessions(): LocalSessionsSnapshot =
        run?.let { live ->
            live.sessionSnapshot().let {
                if (live.finished) it.copy(unavailable = true, canChange = false) else it
            }
        } ?: LocalSessionsSnapshot()

    @Synchronized
    fun selectCurve(id: UUID) {
        run?.selectCurve(id)
    }

    @Synchronized
    fun refreshSessions() {
        run?.refreshSessions()
    }

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
        @Volatile private var curveController: LiveCurveController? = null

        fun timingSnapshot() = curveController?.timingSnapshot()

        fun curveSnapshot() = curveController?.snapshot() ?: LiveCurveSnapshot()

        fun requestCurve() {
            curveController?.request()
            signal()
        }

        fun sessionSnapshot() = curveController?.sessions() ?: LocalSessionsSnapshot()

        fun selectCurve(id: UUID) {
            curveController?.select(id)
            signal()
        }

        fun refreshSessions() {
            curveController?.refreshSessions()
            signal()
        }

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
            var analyses: AnalysisRepository? = null
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
                // Result storage failure must not prevent canonical acquisition.
                try {
                    analyses =
                        AnalysisRepository(context, "oxywatt-live-diagnostic.db", 32L * 1024 * 1024)
                    curveController =
                        LiveCurveController(
                            repository,
                            analyses,
                            coordinator,
                            BuildConfig.CODE_REVISION +
                                if (BuildConfig.CODE_DIRTY) "+local" else "",
                            SystemClock::elapsedRealtimeNanos,
                        )
                } catch (_: Exception) {
                    analyses?.close()
                    analyses = null
                }
                var started = false
                while (true) {
                    if (requestedStop || coordinator.shouldStopSource) handle?.close()
                    val controller = curveController
                    if (controller == null) coordinator.pump()
                    else controller.measureWorker(CurveWork.CANONICAL) { coordinator.pump() }
                    if (!started && coordinator.ready && !requestedStop) {
                        started = true
                        handle = source(coordinator, capture, origin, ::signal)
                        if (requestedStop) handle?.close()
                    }
                    if (requestedStop && !started) coordinator.stopped()
                    if (coordinator.finished) break
                    curveController?.pump()
                    // Continue through the outer canonical/stop checks, never a Job-internal loop.
                    if (controller?.shouldWaitAfterTurn() == false) continue
                    // One reusable waiter; notifications never retain events or enqueue tasks.
                    val delay =
                        if (curveController?.hasWork == true) 1L else coordinator.waitMillis()
                    if (controller == null) synchronized(wake) { wake.wait(delay) }
                    else controller.waitForWorker { synchronized(wake) { wake.wait(delay) } }
                }
            } catch (error: Exception) {
                failure = error.javaClass.simpleName.take(80)
            } finally {
                curveController?.stopped()
                try {
                    handle?.close()
                } finally {
                    try {
                        try {
                            analyses?.close()
                        } finally {
                            repository?.close()
                        }
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

/** Fixed operation-count pacing; canonical/stop checks still run before each analysis step. */
internal class AnalysisPacing(val maximumTurns: Int = 16) {
    init {
        require(maximumTurns == 1 || maximumTurns == 16)
    }

    val policy = if (maximumTurns == 1) "single-effect-wait-v1" else "diagnostic-analysis-pacing-v1"
    private var turns = 0

    fun shouldWait(immediateAnalysis: Boolean): Boolean {
        if (!immediateAnalysis) {
            turns = 0
            return true
        }
        turns++
        if (turns < maximumTurns) return false
        turns = 0
        return true
    }
}
