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
    private data class FrozenRequest(
        val session: UUID,
        val inputHash: String,
        val requestId: UUID,
        val provenance: AnalysisProvenance,
    )

    private var request: FrozenRequest? = null
    val hasPreparedRequest: Boolean
        get() = request != null

    companion object {
        fun slot(id: UUID) =
            AnalysisSlot(id, "observed-power-curve", "synthetic-canonical-power-hold-v1-budget-v1")
    }

    fun prepare(
        id: UUID,
        manifest: String,
        codeVersion: String,
        cancelled: () -> Boolean = { false },
    ): PreparedPowerCompletion {
        require(manifest.startsWith("SYNTHETIC:"))
        val revision = inputs.state().revision
        var terminalSession: SessionSnapshot? = null
        var first = 0L
        var last = 0L
        var count = 0L
        val hash = PowerReplayHash()
        val profile = PowerReplayHash()
        hash.token(id.toString())
        hash.token(
            "canonical-power-hold-v1:power-range-means-v2:power-replay-work-v2:16384:100000000"
        )
        hash.token(manifest)
        scan(revision, cancelled) { row ->
            if (relevant(row.update, id)) {
                row.update.sessionChanges.singleOrNull { it.id == id }?.let { terminalSession = it }
                if (first == 0L) first = row.revision
                last = row.revision
                count++
                hash.document(selectedDocument(row, id))
                row.update.checkpoint.profileObservation?.let {
                    profile.document(CollectorCodec.observation(it))
                }
            }
        }
        val session = checkNotNull(terminalSession) { "No committed session" }
        require(session.phase in setOf(SessionPhase.ENDED_LOCALLY, SessionPhase.INTERRUPTED)) {
            "Session is still active"
        }
        val inputHash = hash.finish()
        val provenance =
            AnalysisProvenance(
                inputHash,
                "${session.source.adapterId}:${session.source.adapterVersion}:${session.normalizationVersion}",
                profile.finish(),
                "power-range-means-v2",
                "canonical-power-hold-v1",
                "power-replay-work-v2:16384:100000000:${session.policy.version}:${session.policy.channelSilenceNanos}",
                "not-applicable-v1",
                "not-applicable-v1",
                Instant.now(),
                codeVersion,
            )
        val frozen =
            request?.also {
                check(
                    it.session == id &&
                        it.inputHash == inputHash &&
                        it.provenance.codeVersion == codeVersion
                ) {
                    "Prepared request input changed"
                }
            } ?: FrozenRequest(id, inputHash, UUID.randomUUID(), provenance).also { request = it }
        val generation = analyses.request(slot(id), frozen.requestId, frozen.provenance)
        val terminal =
            try {
                val engine = BudgetedPowerCurve(id, cancelled = cancelled)
                val replay =
                    CanonicalPowerReplay(id) { part ->
                        if (cancelled()) throw CancellationException()
                        engine.accept(part)
                    }
                scan(revision, cancelled) { row ->
                    // Metadata applies before every affected update; min boundary is idempotent.
                    if (relevant(row.update, id)) {
                        require(row.metadata.policy.version == "admission-v1") {
                            "Unsupported admission policy"
                        }
                        replay.accept(row.update, row.metadata.loss)
                    }
                }
                check(replay.session == session)
                AnalysisTerminal(
                    AnalysisStatus.AVAILABLE,
                    Instant.now(),
                    curve =
                        StoredPowerCurve(
                            session,
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
                        ),
                )
            } catch (failure: Exception) {
                AnalysisTerminal(
                    AnalysisStatus.FAILED,
                    Instant.now(),
                    reason =
                        if (failure is CancellationException) "CANCELLED"
                        else "INVALID_OR_UNSUPPORTED_POWER_REPLAY",
                )
            }
        request = null
        return PreparedPowerCompletion(generation, terminal)
    }

    private fun scan(revision: Long, cancelled: () -> Boolean, consume: (CommittedEffect) -> Unit) {
        var afterRevision = 0L
        var index = -1
        while (true) {
            if (cancelled()) throw CancellationException()
            val page = inputs.replay(revision, afterRevision, index)
            if (page.isEmpty()) return
            page.forEach { consume(it) }
            afterRevision = page.last().revision
            index = page.last().index
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
