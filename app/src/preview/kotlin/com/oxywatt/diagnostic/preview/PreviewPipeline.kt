package com.oxywatt.diagnostic.preview

import android.content.Context
import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
import java.util.UUID

/** Worker-confined real pipeline. UI reads only the immutable owner snapshot. */
class PreviewPipeline(
    context: Context,
    val databaseName: String,
    val epoch: UUID = UUID.randomUUID(),
    policy: AdmissionPolicy = AdmissionPolicy(),
) : AutoCloseable {
    init {
        require(
            databaseName.startsWith("oxywatt-synthetic-") &&
                databaseName.endsWith(".db") &&
                !databaseName.contains("/") &&
                !databaseName.contains("..")
        )
    }

    private val repository = InputRepository(context, databaseName)
    private val admission = BoundedAdmission(policy)
    private val lifecycle =
        CollectorLifecycle(
            epoch,
            LifecyclePolicy("preview-silence-v1", 2_000_000_000),
            recovered = repository.checkpoint(),
        )
    private val admissionPolicy = policy
    private var flight: AdmissionBatch? = null
    private var write: PreparedInputWrite? = null
    var received: SourceObservation? = null
        private set

    val committedReadings = mutableMapOf<Metric, InputPayload.MetricReading>()
    var gaps = 0L
        private set

    val readings = mutableMapOf<Metric, SourceObservation>()
    var saved = 0L
        private set

    var loss = 0L
        private set

    var error: String? = null
        private set

    var lastCommit: Long? = null
        private set

    val pending
        get() = admission.pendingCount

    val checkpoint
        get() = lifecycle.checkpoint()

    val committedCheckpoint
        get() = repository.checkpoint()

    init {
        if (repository.checkpoint() != null) persist(lifecycle.advanceTime(0))
        refreshCommitted()
    }

    private fun refreshCommitted() {
        committedReadings.clear()
        var position = 0L
        var count = 0L
        val id = repository.checkpoint()?.latestSession?.id
        if (id != null)
            while (true) {
                val page = repository.observations(position, 256, id)
                count += page.size
                for (stored in page) (stored.observation.payload as? InputPayload.MetricReading)
                    ?.let { committedReadings[it.metric] = it }
                if (page.isEmpty()) break
                position = page.last().position
            }
        saved = count
    }

    internal fun failWrites(action: () -> Unit) {
        repository.beforeCheckpoint = action
    }

    fun accept(input: SourceObservation) {
        received = input
        (input.payload as? InputPayload.MetricReading)?.let { readings[it.metric] = input }
        if (!admission.offer(input)) loss++
    }

    fun flush() {
        try {
            if (write == null) {
                flight = admission.takeBatch() ?: return
                write = repository.prepare(flight!!, lifecycle, repository.state().revision)
            }
            repository.commit(write!!)
            gaps += repository.effects(write!!.batchId).sumOf { it.coverageBreaks.size }.toLong()
            flight?.let(admission::committed)
            write = null
            flight = null
            refreshCommitted()
            lastCommit = repository.checkpoint()?.lastReceipt?.elapsedNanos
            error = null
        } catch (failure: Exception) {
            error = failure.javaClass.simpleName.take(80)
        }
    }

    fun advance(time: Long) {
        if (admission.flushDue(time)) flush()
        if (pending == 0 && error == null) persist(lifecycle.advanceTime(time))
    }

    fun stopped(failed: Boolean = false) {
        flush()
        if (pending > 0 && error == null) flush()
        if (pending == 0 && error == null) persist(lifecycle.sourceStopped(failed))
    }

    private fun persist(update: LifecycleUpdate) {
        check(write == null)
        try {
            write = repository.prepareEffect(update, admissionPolicy, repository.state().revision)
            flush()
        } catch (failure: Exception) {
            error = failure.javaClass.simpleName.take(80)
        }
    }

    override fun close() = repository.close()
}
