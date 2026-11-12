package com.oxywatt.diagnostic.persistence

import com.oxywatt.core.collection.*
import com.oxywatt.core.power.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CancellationException
import org.json.JSONObject

/** Frozen terminal for an exact storage retry; no second result store or background scheduler. */
data class PreparedPowerCompletion(val generation: Long, val terminal: AnalysisTerminal)

class PowerCurveCalculation(
    private val inputs: InputRepository,
    private val analyses: AnalysisRepository,
) {
    private var pending: Job? = null
    val hasPreparedRequest
        get() = pending?.awaitingRequest == true

    companion object {
        const val REAL = "REAL_KAROO_DIAGNOSTIC:v1"

        fun slot(id: UUID) =
            AnalysisSlot(id, "observed-power-curve", "synthetic-canonical-power-hold-v1-budget-v1")

        fun realSlot(id: UUID) =
            AnalysisSlot(id, "observed-power-curve", "real-karoo-diagnostic-power-hold-v1-work-v2")
    }

    fun beginReal(id: UUID, codeVersion: String, cancelled: () -> Boolean = { false }): Job =
        Job(id, REAL, codeVersion, true, cancelled)

    fun prepareReal(id: UUID, codeVersion: String): PreparedPowerCompletion {
        val job = beginReal(id, codeVersion)
        while (job.completion == null) job.step()
        return job.completion!!
    }

    fun prepare(
        id: UUID,
        manifest: String,
        codeVersion: String,
        cancelled: () -> Boolean = { false },
    ): PreparedPowerCompletion {
        require(manifest.startsWith("SYNTHETIC:"))
        val job =
            pending?.also {
                require(it.id == id && it.manifest == manifest && it.codeVersion == codeVersion)
            } ?: Job(id, manifest, codeVersion, false, cancelled).also { pending = it }
        try {
            while (job.completion == null) job.step()
        } catch (failure: Exception) {
            // Only an allocated-or-uncertain request needs its frozen identity on retry.
            // A cancelled hash pass must not retain the previous cancellation callback.
            if (!job.awaitingRequest) pending = null
            throw failure
        }
        pending = null
        return job.completion!!
    }

    /**
     * One effect per step; at most one 256-effect page retained across both frozen-revision passes.
     */
    inner class Job
    internal constructor(
        val id: UUID,
        val manifest: String,
        val codeVersion: String,
        val live: Boolean,
        private val cancelled: () -> Boolean,
    ) {
        val revision = inputs.state().revision
        private var session: SessionSnapshot? = null
        private var first = 0L
        private var last = 0L
        private var count = 0L
        private val hash = PowerReplayHash()
        private val profile = PowerReplayHash()
        private var inputHash = ""
        private var acquisition: String? = null
        private var acquisitionHash: String? = null
        private var provenance: AnalysisProvenance? = null
        private val requestId = UUID.randomUUID()
        private var generation: Long? = null
        private var phase = 0
        private var cursor = Cursor()
        private val engine =
            BudgetedPowerCurve(
                id,
                cancelled = cancelled,
                coverageOrigin =
                    if (live) "committed-real-karoo-diagnostic-effects-v1"
                    else "committed-synthetic-effects-v1",
            )
        private val replay =
            CanonicalPowerReplay(id) { part ->
                if (cancelled()) throw CancellationException()
                engine.accept(part)
            }
        var completion: PreparedPowerCompletion? = null
            private set

        val awaitingRequest
            get() = phase == 1

        val allocatedGeneration
            get() = generation

        internal val retainedPageSize
            get() = cursor.page.size

        init {
            hash.token(id.toString())
            hash.token(
                "canonical-power-hold-v1:power-range-means-v2:power-replay-work-v2:16384:100000000"
            )
            hash.token(manifest)
        }

        private inner class Cursor {
            var page = emptyList<CommittedEffect>()
            private var offset = 0
            private var afterRevision = 0L
            private var index = -1

            fun next(): CommittedEffect? {
                if (offset == page.size) {
                    if (cancelled()) throw CancellationException()
                    page = inputs.replay(revision, afterRevision, index)
                    offset = 0
                    if (page.isEmpty()) return null
                }
                return page[offset++].also {
                    afterRevision = it.revision
                    index = it.index
                }
            }
        }

        fun step() {
            if (completion != null) return
            when (phase) {
                0 -> {
                    if (live && cancelled()) throw CancellationException()
                    val row = cursor.next()
                    if (row != null) {
                        if (relevant(row.update, id)) {
                            row.update.sessionChanges
                                .singleOrNull { it.id == id }
                                ?.let { session = it }
                            if (first == 0L) first = row.revision
                            last = row.revision
                            count++
                            hash.document(selectedDocument(row, id))
                            if (live) {
                                val text =
                                    requireNotNull(row.metadata.acquisition) {
                                        "Missing acquisition provenance"
                                    }
                                val j = JSONObject(text)
                                CollectorCodec.validateLivePowerAcquisition(j)
                                require(j.getString("origin") == "real-karoo-diagnostic")
                                require(row.metadata.policy == AdmissionPolicy())
                                require(
                                    j.getString("epoch") ==
                                        (session ?: row.update.checkpoint.latestSession)!!
                                            .epochId
                                            .toString()
                                )
                                val digest = PowerReplayHash().also { it.document(j) }.finish()
                                require(acquisitionHash == null || acquisitionHash == digest) {
                                    "Mixed acquisition provenance"
                                }
                                acquisition = text
                                acquisitionHash = digest
                                hash.document(j)
                            }
                            if (!live) {
                                require(row.metadata.acquisition == null) {
                                    "Real acquisition cannot use a synthetic calculation"
                                }
                            }
                            row.update.checkpoint.profileObservation?.let {
                                profile.document(CollectorCodec.observation(it))
                            }
                        }
                    } else {
                        val s = checkNotNull(session) { "No committed session" }
                        require(
                            s.phase in setOf(SessionPhase.ENDED_LOCALLY, SessionPhase.INTERRUPTED)
                        ) {
                            "Session is still active"
                        }
                        if (live) {
                            require(
                                s.phase == SessionPhase.ENDED_LOCALLY &&
                                    s.origin == SessionOrigin.OBSERVED_START &&
                                    s.ambiguityGroupId == null
                            )
                            require(
                                s.policy ==
                                    LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000)
                            )
                            require(
                                s.source.adapterId == "karoo" &&
                                    s.source.adapterVersion == "1.1.9" &&
                                    s.normalizationVersion == "karoo-normalization-v1"
                            )
                        }
                        inputHash = hash.finish()
                        provenance =
                            AnalysisProvenance(
                                inputHash,
                                "${s.source.adapterId}:${s.source.adapterVersion}:${s.normalizationVersion}",
                                profile.finish(),
                                "power-range-means-v2",
                                "canonical-power-hold-v1",
                                "power-replay-work-v2:16384:100000000:${s.policy.version}:${s.policy.channelSilenceNanos}",
                                "not-applicable-v1",
                                "not-applicable-v1",
                                Instant.now(),
                                codeVersion,
                            )
                        cursor = Cursor()
                        phase = 1
                    }
                }
                1 -> {
                    if (live && cancelled()) throw CancellationException()
                    generation =
                        analyses.request(
                            if (live) realSlot(id) else slot(id),
                            requestId,
                            provenance!!,
                        )
                    phase = 2
                }
                2 ->
                    try {
                        if (live && cancelled()) throw CancellationException()
                        val row = cursor.next()
                        if (row != null) {
                            if (relevant(row.update, id)) {
                                require(row.metadata.policy.version == "admission-v1") {
                                    "Unsupported admission policy"
                                }
                                if (!live)
                                    require(session!!.policy.version == "preview-silence-v1") {
                                        "Real policy cannot use a synthetic calculation"
                                    }
                                replay.accept(row.update, row.metadata.loss)
                            }
                        } else {
                            check(replay.session == session)
                            finish(
                                AnalysisTerminal(
                                    AnalysisStatus.AVAILABLE,
                                    Instant.now(),
                                    curve =
                                        StoredPowerCurve(
                                            session!!,
                                            engine.snapshot(),
                                            revision,
                                            first,
                                            last,
                                            inputHash,
                                            manifest,
                                            engine.reserved,
                                            engine.limited,
                                            count,
                                            engine.intervalCount,
                                            acquisition,
                                        ),
                                )
                            )
                        }
                    } catch (failure: Exception) {
                        fail(
                            if (failure is CancellationException) "CANCELLED"
                            else "INVALID_OR_UNSUPPORTED_POWER_REPLAY"
                        )
                    }
            }
        }

        fun cancel(replacePrepared: Boolean = false): PreparedPowerCompletion? {
            if (
                (completion == null || replacePrepared) &&
                    generation != null &&
                    completion?.terminal?.reason != "CANCELLED"
            )
                fail("CANCELLED")
            return completion
        }

        private fun fail(reason: String) =
            finish(AnalysisTerminal(AnalysisStatus.FAILED, Instant.now(), reason = reason))

        private fun finish(terminal: AnalysisTerminal) {
            completion = PreparedPowerCompletion(checkNotNull(generation), terminal)
            cursor = Cursor()
            phase = 3
        }
    }

    private fun relevant(u: LifecycleUpdate, id: UUID) =
        u.envelope?.sessionId == id ||
            u.sessionChanges.any { it.id == id } ||
            u.coverageBreaks.any { it.sessionId == id }

    private fun selectedDocument(row: CommittedEffect, id: UUID): JSONObject {
        val u = row.update
        val selected =
            u.copy(
                envelope = u.envelope?.takeIf { it.sessionId == id },
                sessionChanges = u.sessionChanges.filter { it.id == id },
                coverageBreaks = u.coverageBreaks.filter { it.sessionId == id },
            )
        // Full checkpoint can name another session during recovery. Hash only selected facts.
        val j = CollectorCodec.update(selected)
        j.remove("checkpoint")
        j.put("checkpointSchema", u.checkpoint.schemaVersion)
        j.put(
            "profile",
            u.checkpoint.profileObservation?.let(CollectorCodec::observation) ?: JSONObject.NULL,
        )
        if (
            u.checkpoint.epochId ==
                (u.sessionChanges.firstOrNull { it.id == id }?.epochId
                    ?: u.envelope?.stamp?.epochId)
        ) {
            j.put("horizonReceipt", u.checkpoint.lastReceipt?.elapsedNanos ?: JSONObject.NULL)
        }
        val session =
            u.sessionChanges.firstOrNull { it.id == id }
                ?: u.checkpoint.latestSession?.takeIf { it.id == id }
        val loss =
            row.metadata.loss?.takeIf { loss ->
                session != null &&
                    loss.first.epochId == session.epochId &&
                    loss.last.ingestSequence >= session.firstReceipt.ingestSequence &&
                    (loss.first.ingestSequence <= session.lastReceipt.ingestSequence ||
                        session.interruptionReason == InterruptionReason.TRANSPORT_LOSS)
            }
        val terminal = row.metadata.terminal?.takeIf { session?.interruptionReason == it }
        j.put("metadata", CollectorCodec.metadata(row.metadata.policy, loss, terminal))
        return j
    }
}
