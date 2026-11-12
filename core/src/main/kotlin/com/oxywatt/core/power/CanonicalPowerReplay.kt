package com.oxywatt.core.power

import com.oxywatt.core.collection.*
import java.util.UUID

/** Null emission is an explicit break, including adjacent expiry/receipt boundaries. */
class CanonicalPowerReplay(
    private val sessionId: UUID,
    private val emit: (PowerInterval?) -> Unit,
) {
    var session: SessionSnapshot? = null
        private set

    private var held: PowerInterval? = null
    private var expires: Long? = null
    private var horizon = 0L
    private var lossBoundary: Long? = null
    private var lastEnvelope: ReceiptStamp? = null

    fun accept(update: LifecycleUpdate, loss: AdmissionLoss? = null) {
        val changed = update.sessionChanges.singleOrNull { it.id == sessionId }
        val prior = session
        if (changed != null) {
            require(
                changed.policy.version == "preview-silence-v1" ||
                    (changed.policy.version == "karoo-diagnostic-silence-v1" &&
                        changed.policy.channelSilenceNanos == 5_000_000_000)
            ) {
                "Unsupported lifecycle policy"
            }
            if (prior != null)
                require(
                    changed.epochId == prior.epochId &&
                        changed.policy == prior.policy &&
                        changed.firstReceipt == prior.firstReceipt &&
                        changed.source == prior.source &&
                        changed.normalizationVersion == prior.normalizationVersion &&
                        changed.lastReceipt.ingestSequence >= prior.lastReceipt.ingestSequence &&
                        changed.lastReceipt.elapsedNanos >= prior.lastReceipt.elapsedNanos &&
                        changed.segmentIndex >= prior.segmentIndex
                )
            session = changed
        }
        val current = session ?: return
        val epoch = current.epochId
        if (
            loss?.first?.epochId == epoch &&
                loss.last.ingestSequence >= current.firstReceipt.ingestSequence
        )
            lossBoundary =
                minOf(
                    lossBoundary ?: Long.MAX_VALUE,
                    maxOf(loss.first.elapsedNanos, current.firstReceipt.elapsedNanos),
                )
        val breaks = update.coverageBreaks.filter { it.sessionId == sessionId }
        // Recovery timestamps belong to the new process epoch, not the old session.
        val sameEpoch = update.checkpoint.epochId == epoch
        val powerBreaks =
            if (sameEpoch) breaks.filter { it.metric == Metric.POWER_W } else emptyList()
        val receipt = if (sameEpoch) update.checkpoint.lastReceipt?.elapsedNanos else null
        val nextHorizon =
            maxOf(
                horizon,
                receipt ?: horizon,
                if (sameEpoch) breaks.maxOfOrNull { it.detectedAtNanos } ?: horizon else horizon,
            )
        val expiry = expires
        if (held != null && expiry != null && expiry <= nextHorizon) {
            require(
                powerBreaks.any {
                    it.reason == CoverageBreakReason.SILENCE && it.detectedAtNanos == expiry
                } || (lossBoundary ?: Long.MAX_VALUE) <= expiry
            ) {
                "Missing persisted expiry"
            }
        }
        for (b in powerBreaks) {
            require(b.detectedAtNanos >= horizon) { "Regressing coverage break" }
            advance(minOf(b.detectedAtNanos, lossBoundary ?: Long.MAX_VALUE))
            clear()
        }
        advance(minOf(nextHorizon, lossBoundary ?: Long.MAX_VALUE))
        if (lossBoundary?.let { it <= nextHorizon } == true) clear()
        horizon = nextHorizon
        if (
            changed != null &&
                (changed.phase != SessionPhase.RECORDING ||
                    prior?.segmentIndex?.let { it != changed.segmentIndex } == true)
        )
            clear()
        val envelope = update.envelope?.takeIf { it.sessionId == sessionId }
        if (envelope != null) {
            require(envelope.stamp.epochId == epoch && envelope.stamp == current.lastReceipt)
            require(
                envelope.source.adapterId == current.source.adapterId &&
                    envelope.source.adapterVersion == current.source.adapterVersion &&
                    envelope.normalizationVersion == current.normalizationVersion
            )
            lastEnvelope?.let {
                require(
                    envelope.stamp.ingestSequence > it.ingestSequence &&
                        envelope.stamp.elapsedNanos >= it.elapsedNanos
                )
            }
            lastEnvelope = envelope.stamp
            val reading = envelope.payload as? InputPayload.MetricReading
            if (reading?.metric == Metric.POWER_W && update.recordingSegment != null) {
                if (reading.value == null) clear()
                else if (lossBoundary?.let { envelope.stamp.elapsedNanos >= it } != true) {
                    require(
                        current.phase == SessionPhase.RECORDING &&
                            update.recordingSegment == current.segmentIndex
                    )
                    val t = envelope.stamp.elapsedNanos
                    held =
                        PowerInterval(
                            sessionId,
                            epoch,
                            update.recordingSegment,
                            t,
                            t,
                            reading.value,
                        )
                    expires =
                        if (t > Long.MAX_VALUE - current.policy.channelSilenceNanos) Long.MAX_VALUE
                        else t + current.policy.channelSilenceNanos
                }
            }
        }
    }

    private fun advance(to: Long) {
        val part = held ?: return
        val end = minOf(to, expires ?: to)
        if (end > part.startNanos) {
            emit(part.copy(endNanos = end))
            held = part.copy(startNanos = end, endNanos = end)
        }
    }

    private fun clear() {
        held = null
        expires = null
        emit(null)
    }
}

/** Charged range-index work; an exhausted append never publishes a partial candidate. */
class BudgetedPowerCurve(
    sessionId: UUID,
    policy: PowerCurvePolicy = PowerCurvePolicy(),
    val budget: Long = 100_000_000,
    cancelled: () -> Boolean = { false },
    coverageOrigin: String = "committed-synthetic-effects-v1",
) {
    private val engine =
        PowerCurveEngine(
            sessionId,
            PowerProvenance("canonical-power-hold-v1", coverageOrigin),
            policy,
            budget,
            cancelled,
        )
    val reserved: Long
        get() = engine.workStats.total

    val workStats: PowerWorkStats
        get() = engine.workStats

    val limited: Boolean
        get() = engine.workLimited

    val retainedPeak: Int
        get() = engine.retainedPeak

    val stagedNodeBound: Int
        get() = engine.stagedNodeBound

    val candidatePeak: Int
        get() = engine.candidatePeak

    var intervalCount = 0L
        private set

    fun accept(part: PowerInterval?) {
        if (limited) return
        if (part == null) engine.breakCoverage()
        else {
            engine.accept(part)
            if (!limited) intervalCount++
        }
    }

    fun snapshot(): PowerCurveSnapshot = engine.snapshot()
}
