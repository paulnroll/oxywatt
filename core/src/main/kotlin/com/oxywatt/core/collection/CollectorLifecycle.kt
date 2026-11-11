package com.oxywatt.core.collection

import java.util.UUID

data class LifecyclePolicy(val version: String, val channelSilenceNanos: Long) {
    init {
        require(version.isNotBlank())
        require(channelSilenceNanos > 0)
    }
}

enum class SessionPhase {
    RECORDING,
    PAUSED,
    ENDED_LOCALLY,
    INTERRUPTED,
}

enum class SessionOrigin {
    OBSERVED_START,
    COLD_ATTACH,
    RECOVERY_FRAGMENT,
}

enum class InterruptionReason {
    PROCESS_RESTART,
    SOURCE_STOPPED,
    SOURCE_FAILED,
    UNKNOWN_STATE,
    TRANSPORT_LOSS,
    INVALID_ORDER,
    SOURCE_CHANGED,
    CONTROL_OVERFLOW,
    STORAGE_FAILURE,
}

enum class InputRejection {
    WRONG_EPOCH,
    NON_INCREASING_SEQUENCE,
    REGRESSING_TIME,
    SOURCE_CHANGED,
}

enum class CoverageBreakReason {
    MISSING_VALUE,
    SILENCE,
    PAUSE,
    SESSION_ENDED,
    INTERRUPTED,
}

data class SessionSnapshot(
    val id: UUID,
    val epochId: UUID,
    val phase: SessionPhase,
    val origin: SessionOrigin,
    val firstReceipt: ReceiptStamp,
    val lastReceipt: ReceiptStamp,
    val segmentIndex: Long,
    val policy: LifecyclePolicy,
    val source: SourceRef,
    val normalizationVersion: String,
    val ambiguityGroupId: UUID? = null,
    val interruptionReason: InterruptionReason? = null,
) {
    init {
        require(firstReceipt.epochId == epochId && lastReceipt.epochId == epochId)
        require(lastReceipt.elapsedNanos >= firstReceipt.elapsedNanos)
        require(lastReceipt.ingestSequence >= firstReceipt.ingestSequence)
        require(segmentIndex >= 0)
        require(normalizationVersion.isNotBlank())
        require(phase != SessionPhase.INTERRUPTED || ambiguityGroupId != null)
        require((phase == SessionPhase.INTERRUPTED) == (interruptionReason != null))
        require(origin == SessionOrigin.OBSERVED_START || ambiguityGroupId != null)
    }

    val trendEligible: Boolean
        get() =
            phase == SessionPhase.ENDED_LOCALLY &&
                origin == SessionOrigin.OBSERVED_START &&
                ambiguityGroupId == null
}

data class ChannelCoverageBreak(
    val sessionId: UUID,
    val metric: Metric,
    val reason: CoverageBreakReason,
    val detectedAtNanos: Long,
)

data class CollectorCheckpoint(
    val epochId: UUID,
    val policy: LifecyclePolicy,
    val lastReceipt: ReceiptStamp?,
    val latestSession: SessionSnapshot?,
    val profileObservation: SourceObservation?,
    val schemaVersion: String = "collector-checkpoint-v1",
) {
    init {
        require(lastReceipt == null || lastReceipt.epochId == epochId)
        require(profileObservation == null || profileObservation.payload is InputPayload.Profile)
        latestSession
            ?.takeIf { it.phase == SessionPhase.RECORDING || it.phase == SessionPhase.PAUSED }
            ?.let {
                require(it.epochId == epochId && it.lastReceipt == lastReceipt)
            }
    }
}

data class LifecycleUpdate(
    val checkpoint: CollectorCheckpoint,
    val envelope: InputEnvelope? = null,
    val recordingSegment: Long? = null,
    val sessionChanges: List<SessionSnapshot> = emptyList(),
    val coverageBreaks: List<ChannelCoverageBreak> = emptyList(),
    val rejection: InputRejection? = null,
)

/** Single-owner state machine; outputs are not durable until the repository commits them. */
class CollectorLifecycle(
    private val epochId: UUID,
    private val policy: LifecyclePolicy,
    private val newSessionId: () -> UUID = UUID::randomUUID,
    recovered: CollectorCheckpoint? = null,
) {
    private var lastReceipt: ReceiptStamp? = null
    private var latestSession: SessionSnapshot? = null
    private var profileObservation: SourceObservation? = null
    private var hostState = RecordingState.UNKNOWN
    private var idleObservation: SourceObservation? = null
    private var now = 0L
    private var recoveryGroup: UUID? = null
    private var pendingRecovery: SessionSnapshot? = null
    private val lastPresent = arrayOfNulls<Long>(Metric.entries.size)
    private val broken = BooleanArray(Metric.entries.size)

    init {
        recovered?.let { saved ->
            require(saved.schemaVersion == "collector-checkpoint-v1") { "Unsupported checkpoint" }
            require(saved.epochId != epochId) { "Recovery requires a new acquisition epoch" }
            latestSession = saved.latestSession
            profileObservation = saved.profileObservation
            val prior = active()
            if (prior != null) {
                recoveryGroup = prior.ambiguityGroupId ?: prior.id
                latestSession =
                    prior.copy(
                        phase = SessionPhase.INTERRUPTED,
                        ambiguityGroupId = recoveryGroup,
                        interruptionReason = InterruptionReason.PROCESS_RESTART,
                    )
                pendingRecovery = latestSession
            } else if (latestSession?.phase == SessionPhase.INTERRUPTED) {
                recoveryGroup = latestSession?.ambiguityGroupId
            }
        }
    }

    fun checkpoint(): CollectorCheckpoint =
        CollectorCheckpoint(epochId, policy, lastReceipt, latestSession, profileObservation)

    fun accept(input: SourceObservation): LifecycleUpdate {
        val changes = mutableListOf<SessionSnapshot>()
        val breaks = mutableListOf<ChannelCoverageBreak>()
        val previous = active()
        val rejection =
            when {
                input.stamp.epochId != epochId -> InputRejection.WRONG_EPOCH
                lastReceipt?.let { input.stamp.ingestSequence <= it.ingestSequence } == true ->
                    InputRejection.NON_INCREASING_SEQUENCE
                input.stamp.elapsedNanos < now -> InputRejection.REGRESSING_TIME
                previous != null &&
                    (previous.source.adapterId != input.source.adapterId ||
                        previous.source.adapterVersion != input.source.adapterVersion ||
                        previous.normalizationVersion != input.normalizationVersion) ->
                    InputRejection.SOURCE_CHANGED
                else -> null
            }
        if (rejection != null) {
            drainRecovery(changes)
            val reason =
                if (rejection == InputRejection.SOURCE_CHANGED) InterruptionReason.SOURCE_CHANGED
                else InterruptionReason.INVALID_ORDER
            interruptSession(reason, breaks)?.let(changes::add)
            return LifecycleUpdate(
                checkpoint(),
                sessionChanges = changes,
                coverageBreaks = breaks,
                rejection = rejection,
            )
        }
        // Allocate before consuming receipt/recovery state so allocation failure is retryable.
        val state = (input.payload as? InputPayload.Recording)?.state
        val allocatedId =
            if (
                previous == null &&
                    (state == RecordingState.RECORDING || state == RecordingState.PAUSED)
            ) {
                newSessionId().also {
                    require(it != latestSession?.id && it != latestSession?.ambiguityGroupId) {
                        "Session IDs must be unique"
                    }
                }
            } else null
        drainRecovery(changes)
        breaks += expire(input.stamp.elapsedNanos)
        lastReceipt = input.stamp
        now = input.stamp.elapsedNanos
        var envelopeSession: UUID? = active()?.id
        if (active() != null) latestSession = latestSession!!.copy(lastReceipt = input.stamp)
        when (val payload = input.payload) {
            is InputPayload.Recording -> {
                when (payload.state) {
                    RecordingState.IDLE -> {
                        breakAcquired(CoverageBreakReason.SESSION_ENDED, breaks)
                        active()?.let {
                            latestSession = it.copy(phase = SessionPhase.ENDED_LOCALLY)
                        }
                        resetCoverage()
                        recoveryGroup = null
                    }
                    RecordingState.RECORDING,
                    RecordingState.PAUSED -> {
                        val phase =
                            if (payload.state == RecordingState.RECORDING) SessionPhase.RECORDING
                            else SessionPhase.PAUSED
                        val prior = active()
                        if (prior == null) {
                            resetCoverage()
                            val id = checkNotNull(allocatedId)
                            val origin =
                                when {
                                    hostState == RecordingState.IDLE &&
                                        idleObservation?.let { sameSource(it, input) } == true &&
                                        payload.state == RecordingState.RECORDING ->
                                        SessionOrigin.OBSERVED_START
                                    recoveryGroup != null -> SessionOrigin.RECOVERY_FRAGMENT
                                    else -> SessionOrigin.COLD_ATTACH
                                }
                            val group =
                                if (origin == SessionOrigin.OBSERVED_START) null
                                else recoveryGroup ?: id
                            latestSession =
                                SessionSnapshot(
                                    id,
                                    epochId,
                                    phase,
                                    origin,
                                    input.stamp,
                                    input.stamp,
                                    0,
                                    policy,
                                    input.source,
                                    input.normalizationVersion,
                                    group,
                                )
                        } else {
                            val segment =
                                if (
                                    prior.phase == SessionPhase.PAUSED &&
                                        phase == SessionPhase.RECORDING
                                )
                                    Math.incrementExact(prior.segmentIndex)
                                else prior.segmentIndex
                            if (prior.phase != phase) {
                                if (phase == SessionPhase.PAUSED)
                                    breakAcquired(CoverageBreakReason.PAUSE, breaks)
                                resetCoverage()
                            }
                            latestSession = prior.copy(phase = phase, segmentIndex = segment)
                        }
                        envelopeSession = latestSession!!.id
                    }
                    RecordingState.UNKNOWN ->
                        interruptSession(InterruptionReason.UNKNOWN_STATE, breaks)
                }
                hostState = payload.state
                idleObservation = input.takeIf { payload.state == RecordingState.IDLE }
            }
            is InputPayload.Profile -> profileObservation = input
            is InputPayload.MetricReading ->
                if (active()?.phase == SessionPhase.RECORDING) {
                    val index = payload.metric.ordinal
                    if (payload.value == null) {
                        breakChannel(payload.metric, CoverageBreakReason.MISSING_VALUE, now, breaks)
                    } else {
                        lastPresent[index] = now
                        broken[index] = false
                    }
                }
        }
        if (envelopeSession != null) changes += checkNotNull(latestSession)
        val envelope = envelopeSession?.let {
            InputEnvelope(it, input.stamp, input.source, input.normalizationVersion, input.payload)
        }
        val segment = active()?.takeIf { it.phase == SessionPhase.RECORDING }?.segmentIndex
        return LifecycleUpdate(checkpoint(), envelope, segment, changes, breaks)
    }

    fun advanceTime(elapsedNanos: Long): LifecycleUpdate {
        require(elapsedNanos >= now)
        val changes = mutableListOf<SessionSnapshot>()
        drainRecovery(changes)
        val breaks = expire(elapsedNanos)
        now = elapsedNanos
        return LifecycleUpdate(checkpoint(), sessionChanges = changes, coverageBreaks = breaks)
    }

    fun interrupt(reason: InterruptionReason): LifecycleUpdate {
        val changes = mutableListOf<SessionSnapshot>()
        val breaks = mutableListOf<ChannelCoverageBreak>()
        drainRecovery(changes)
        interruptSession(reason, breaks)?.let(changes::add)
        return LifecycleUpdate(checkpoint(), sessionChanges = changes, coverageBreaks = breaks)
    }

    private fun interruptSession(
        reason: InterruptionReason,
        breaks: MutableList<ChannelCoverageBreak>,
    ): SessionSnapshot? {
        val prior = active()
        breakAcquired(CoverageBreakReason.INTERRUPTED, breaks)
        resetCoverage()
        hostState = RecordingState.UNKNOWN
        idleObservation = null
        if (prior == null) return null
        recoveryGroup = prior.ambiguityGroupId ?: prior.id
        val changed =
            prior.copy(
                phase = SessionPhase.INTERRUPTED,
                ambiguityGroupId = recoveryGroup,
                interruptionReason = reason,
            )
        latestSession = changed
        return changed
    }

    fun sourceStopped(failed: Boolean = false): LifecycleUpdate =
        interrupt(
            if (failed) InterruptionReason.SOURCE_FAILED else InterruptionReason.SOURCE_STOPPED
        )

    private fun drainRecovery(changes: MutableList<SessionSnapshot>) {
        pendingRecovery?.let(changes::add)
        pendingRecovery = null
    }

    private fun expire(time: Long): List<ChannelCoverageBreak> {
        val breaks = mutableListOf<ChannelCoverageBreak>()
        if (active()?.phase != SessionPhase.RECORDING) return breaks
        for (metric in Metric.entries) {
            val last = lastPresent[metric.ordinal] ?: continue
            if (!broken[metric.ordinal] && time - last >= policy.channelSilenceNanos) {
                breakChannel(
                    metric,
                    CoverageBreakReason.SILENCE,
                    Math.addExact(last, policy.channelSilenceNanos),
                    breaks,
                )
            }
        }
        return breaks.sortedBy { it.detectedAtNanos }
    }

    private fun breakAcquired(
        reason: CoverageBreakReason,
        breaks: MutableList<ChannelCoverageBreak>,
    ) {
        for (metric in Metric.entries) {
            if (lastPresent[metric.ordinal] != null) breakChannel(metric, reason, now, breaks)
        }
    }

    private fun breakChannel(
        metric: Metric,
        reason: CoverageBreakReason,
        time: Long,
        breaks: MutableList<ChannelCoverageBreak>,
    ) {
        val session = active() ?: return
        if (!broken[metric.ordinal]) {
            breaks += ChannelCoverageBreak(session.id, metric, reason, time)
            broken[metric.ordinal] = true
        }
    }

    private fun resetCoverage() {
        lastPresent.fill(null)
        broken.fill(false)
    }

    private fun sameSource(a: SourceObservation, b: SourceObservation): Boolean =
        a.source.adapterId == b.source.adapterId &&
            a.source.adapterVersion == b.source.adapterVersion &&
            a.normalizationVersion == b.normalizationVersion

    private fun active(): SessionSnapshot? = latestSession?.takeIf {
        it.phase == SessionPhase.RECORDING || it.phase == SessionPhase.PAUSED
    }
}
