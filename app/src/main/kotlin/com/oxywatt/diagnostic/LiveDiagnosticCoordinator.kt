package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
import java.time.Instant
import java.util.UUID

internal enum class LiveStorageStatus {
    WAITING,
    COLLECTING,
    SAVED,
    RETRYING,
    FATAL,
    DRAINED,
}

internal data class LiveStorageSnapshot(
    val received: Long = 0,
    val committed: Long = 0,
    val pending: Int = 0,
    val bytes: Int = 0,
    val loss: Long = 0,
    val rejected: Long = 0,
    val revision: Long = 0,
    val receipt: ReceiptStamp? = null,
    val session: SessionSnapshot? = null,
    val status: LiveStorageStatus = LiveStorageStatus.WAITING,
    val error: String? = null,
    val restartBlocked: Boolean = false,
)

/** Short gate for admission/cuts; repository and lifecycle belong exclusively to the worker. */
internal class LiveDiagnosticCoordinator(
    private val repository: InputRepository,
    val epoch: UUID = UUID.randomUUID(),
    private val elapsed: () -> Long,
    private val utc: () -> Instant = Instant::now,
    private val policy: AdmissionPolicy = AdmissionPolicy(),
) {
    val gate = Any()
    private val admission = BoundedAdmission(policy)
    private val lifecycle =
        CollectorLifecycle(
            epoch,
            LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
            recovered = repository.checkpoint(),
        )
    private var receivedState = RecordingState.UNKNOWN
    private var receivedControl: ReceiptStamp? = null
    private var analysisCancelled = true

    fun canAnalyze(): Boolean =
        synchronized(gate) {
            accepting &&
                terminal == null &&
                !admission.stopped &&
                receivedState == RecordingState.IDLE &&
                receivedControl?.let { control ->
                    view.receipt?.let { committed ->
                        committed.epochId == control.epochId &&
                            committed.ingestSequence >= control.ingestSequence
                    }
                } == true &&
                view.status in setOf(LiveStorageStatus.SAVED, LiveStorageStatus.COLLECTING)
        }

    fun armAnalysis(): Boolean =
        synchronized(gate) {
            if (!canAnalyze()) false
            else {
                analysisCancelled = false
                true
            }
        }

    fun analysisMustCancel(): Boolean = synchronized(gate) { analysisCancelled || !canAnalyze() }

    private var sequence = 0L
    private var oldestQueued: Long? = null
    @Volatile
    private var view =
        LiveStorageSnapshot(
            revision = repository.state().revision,
            receipt = repository.checkpoint()?.lastReceipt,
            session = repository.checkpoint()?.latestSession,
        )

    private class Work(val batch: AdmissionBatch?, val cut: Long) {
        var batchDone = batch == null
        var cutDone = false
    }

    private var work: Work? = null
    private var write: PreparedInputWrite? = null
    private var terminal: InterruptionReason? = null
    private var terminalAt: Long? = null
    private var terminalPrepared = false
    private var terminalDone = false
    private var accepting = true
    private var retryAt = 0L
    private var retryFailed = false
    internal var afterCommitForTest: () -> Unit = {}
    val ready
        get() = synchronized(gate) { write == null && view.status != LiveStorageStatus.FATAL }

    val finished
        get() = synchronized(gate) { terminalDone || view.status == LiveStorageStatus.FATAL }

    val shouldStopSource
        get() = synchronized(gate) { !accepting || admission.stopped }

    private fun fatal(error: Exception) =
        synchronized(gate) {
            accepting = false
            view =
                view.copy(
                    status = LiveStorageStatus.FATAL,
                    error = error.javaClass.simpleName.take(80),
                )
        }

    init {
        if (repository.checkpoint() != null) {
            write = repository.prepareEffect(lifecycle.advanceTime(0), policy, view.revision)
        }
    }

    /** The adapter must hold gate across stamp AND accept, so timers cannot overtake a receipt. */
    fun stamp(): ReceiptStamp =
        synchronized(gate) {
            ReceiptStamp(utc(), epoch, elapsed(), sequence).also {
                sequence = Math.incrementExact(sequence)
            }
        }

    fun accept(input: SourceObservation) =
        synchronized(gate) {
            (input.payload as? InputPayload.Recording)?.let {
                receivedState = it.state
                receivedControl = input.stamp
                if (it.state != RecordingState.IDLE) analysisCancelled = true
            }
            view = view.copy(received = increment(view.received))
            if (!accepting || admission.stopped) {
                view = view.copy(rejected = increment(view.rejected))
                return@synchronized
            }
            claim(input.stamp.elapsedNanos, false)
            if (!admission.offer(input)) view = view.copy(loss = increment(view.loss))
            else {
                if (oldestQueued == null) oldestQueued = input.stamp.elapsedNanos
                if (
                    view.status == LiveStorageStatus.WAITING ||
                        view.status == LiveStorageStatus.SAVED
                )
                    view = view.copy(status = LiveStorageStatus.COLLECTING)
            }
        }

    private fun claim(time: Long, force: Boolean) {
        if (work != null || write != null) return
        if (force || admission.flushDue(time)) {
            val batch = admission.takeBatch()
            if (batch != null) {
                work = Work(batch, time)
                oldestQueued = null
            }
        }
    }

    /** One worker only; never invoked by SDK/UI callbacks. */
    fun pump(force: Boolean = false) {
        if (elapsed() < retryAt) return
        repeat(8) {
            if (view.status == LiveStorageStatus.FATAL || terminalDone) return
            val task =
                synchronized(gate) {
                    val time = terminalAt ?: elapsed()
                    claim(time, force || terminal != null)
                    if (work == null && write == null && admission.pendingCount == 0)
                        work = Work(null, time)
                    work
                }
            try {
                if (task != null && write == null) {
                    if (!task.batchDone) {
                        write = repository.prepare(task.batch!!, lifecycle, view.revision)
                    } else if (!task.cutDone) {
                        val update = lifecycle.advanceTime(task.cut)
                        task.cutDone = true
                        if (
                            update.sessionChanges.isNotEmpty() || update.coverageBreaks.isNotEmpty()
                        )
                            write = repository.prepareEffect(update, policy, view.revision)
                    }
                }
            } catch (failure: Exception) {
                fatal(failure)
                return
            }
            if (write != null) {
                try {
                    val revision = repository.commit(write!!)
                    afterCommitForTest()
                    val checkpoint = repository.checkpoint()
                    synchronized(gate) {
                        view =
                            view.copy(
                                revision = revision,
                                receipt = checkpoint?.lastReceipt,
                                session = checkpoint?.latestSession,
                                status = LiveStorageStatus.SAVED,
                                error = null,
                            )
                        if (task != null && !task.batchDone) {
                            admission.committed(task.batch!!)
                            view =
                                view.copy(
                                    committed =
                                        saturatingAdd(
                                            view.committed,
                                            task.batch.observations.size.toLong(),
                                        )
                                )
                            task.batchDone = true
                        }
                        write = null
                        retryFailed = false
                        retryAt = 0
                    }
                } catch (failure: android.database.sqlite.SQLiteException) {
                    val delay = if (retryFailed) 5_000_000_000L else 1_000_000_000L
                    retryFailed = true
                    retryAt = elapsed() + delay
                    synchronized(gate) {
                        view =
                            view.copy(
                                status = LiveStorageStatus.RETRYING,
                                error = failure.javaClass.simpleName.take(80),
                            )
                    }
                    return
                } catch (failure: Exception) {
                    fatal(failure)
                    return
                }
            }
            if (task != null && task.batchDone && task.cutDone) synchronized(gate) { work = null }
            synchronized(gate) {
                // Loss/terminal metadata can remain even when pendingCount is zero.
                if (write == null && (force || terminal != null))
                    claim(terminalAt ?: elapsed(), true)
            }
            if (
                synchronized(gate) {
                    work == null && admission.pendingCount == 0 && terminal != null
                }
            ) {
                if (!terminalPrepared) {
                    try {
                        write =
                            repository.prepareEffect(
                                lifecycle.interrupt(terminal!!),
                                policy,
                                view.revision,
                            )
                    } catch (failure: Exception) {
                        fatal(failure)
                        return
                    }
                    terminalPrepared = true
                } else {
                    terminalDone = true
                    synchronized(gate) { view = view.copy(status = LiveStorageStatus.DRAINED) }
                    return
                }
            }
            if (write == null && synchronized(gate) { work == null }) return
        }
    }

    fun stopped(reason: InterruptionReason = InterruptionReason.SOURCE_STOPPED) =
        synchronized(gate) {
            accepting = false
            analysisCancelled = true
            if (terminal == null) {
                terminal = reason
                terminalAt = elapsed()
            }
        }

    fun snapshot(): LiveStorageSnapshot =
        synchronized(gate) {
            view.copy(pending = admission.pendingCount, bytes = admission.pendingBytes)
        }

    fun waitMillis(): Long =
        synchronized(gate) {
            val now = elapsed()
            val deadline =
                if (retryAt > now) retryAt
                else oldestQueued?.let { it + policy.flushNanos } ?: (now + 250_000_000)
            ((deadline - now).coerceIn(1_000_000, 250_000_000) + 999_999) / 1_000_000
        }

    private fun saturatingAdd(a: Long, b: Long) =
        if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b

    private fun increment(value: Long) = if (value == Long.MAX_VALUE) value else value + 1
}
