package com.oxywatt.core.collection

/** Versioned engineering ceilings, not measured heap or physiological limits. */
data class AdmissionPolicy(
    val version: String = "admission-v1",
    val events: Int = 4096,
    val bytes: Int = 4 * 1024 * 1024,
    val reservedEvents: Int = 64,
    val reservedBytes: Int = 64 * 1024,
    val envelopeBytes: Int = 1024,
    val flushNanos: Long = 5_000_000_000,
) {
    init {
        require(version.isNotBlank())
        require(events in 1..4096 && bytes in 1..4 * 1024 * 1024)
        require(reservedEvents in 1..events && reservedBytes in 1..bytes)
        require(envelopeBytes in 1..1024 && flushNanos in 1..5_000_000_000)
    }
}

enum class AdmissionLossReason {
    CAPACITY,
    OVERSIZE,
    CONTROL_OVERFLOW,
    INVALID_ORDER,
}

data class AdmissionLoss(
    val first: ReceiptStamp,
    val last: ReceiptStamp,
    val count: Long,
    val reasons: Set<AdmissionLossReason>,
)

class AdmissionBatch
internal constructor(
    val policy: AdmissionPolicy,
    val observations: List<SourceObservation>,
    val loss: AdmissionLoss?,
    val terminal: InterruptionReason?,
) {
    private var applied = false

    /** Apply once; retry persistence with these effects, never replay into the same lifecycle. */
    fun applyTo(lifecycle: CollectorLifecycle): List<LifecycleUpdate> {
        check(!applied) {
            "Lifecycle effects must be computed once, then retained for persistence retries"
        }
        applied = true
        val result = mutableListOf<LifecycleUpdate>()
        var lossPending = loss != null
        for (input in observations) {
            if (lossPending && input.stamp.ingestSequence > loss!!.first.ingestSequence) {
                result += lifecycle.interrupt(InterruptionReason.TRANSPORT_LOSS)
                lossPending = input.stamp.ingestSequence <= loss.last.ingestSequence
            }
            result += lifecycle.accept(input)
        }
        if (lossPending) result += lifecycle.interrupt(InterruptionReason.TRANSPORT_LOSS)
        terminal?.let { result += lifecycle.interrupt(it) }
        return result
    }
}

/** Serialized owner only. No timer, writer, upstream channel or automatic retry is created. */
class BoundedAdmission(private val policy: AdmissionPolicy = AdmissionPolicy()) {
    private data class Pending(val input: SourceObservation, val bytes: Int, val telemetry: Boolean)

    private val queued = ArrayDeque<Pending>()
    private var inFlight: AdmissionBatch? = null
    private var flightItems: List<Pending> = emptyList()
    private val lossReasons = java.util.EnumSet.noneOf(AdmissionLossReason::class.java)
    private var lossFirst: ReceiptStamp? = null
    private var lossLast: ReceiptStamp? = null
    private var lossCount = 0L
    private var terminal: InterruptionReason? = null
    private var last: ReceiptStamp? = null
    private var now = 0L
    var stopped: Boolean = false
        private set

    var pendingCount: Int = 0
        private set

    var pendingBytes: Int = 0
        private set

    private var telemetryCount = 0
    private var telemetryBytes = 0

    fun offer(input: SourceObservation): Boolean {
        if (stopped) return false
        val stamp = input.stamp
        if (
            last?.let {
                stamp.epochId != it.epochId || stamp.ingestSequence <= it.ingestSequence
            } == true || stamp.elapsedNanos < now
        ) {
            recordLoss(stamp, AdmissionLossReason.INVALID_ORDER)
            stop(InterruptionReason.INVALID_ORDER)
            return false
        }
        last = stamp
        now = stamp.elapsedNanos
        val telemetry = input.payload is InputPayload.MetricReading
        val size = payloadCharge(input)
        val reason =
            when {
                size > policy.envelopeBytes -> AdmissionLossReason.OVERSIZE
                pendingCount >= policy.events || size > policy.bytes - pendingBytes ->
                    AdmissionLossReason.CAPACITY
                telemetry &&
                    (telemetryCount >= policy.events - policy.reservedEvents ||
                        size > policy.bytes - policy.reservedBytes - telemetryBytes) ->
                    AdmissionLossReason.CAPACITY
                else -> null
            }
        if (reason != null) {
            recordLoss(stamp, reason)
            if (!telemetry) {
                recordReason(AdmissionLossReason.CONTROL_OVERFLOW)
                stop(InterruptionReason.CONTROL_OVERFLOW)
            }
            return false
        }
        queued.addLast(Pending(input, size, telemetry))
        pendingCount++
        pendingBytes += size
        if (telemetry) {
            telemetryCount++
            telemetryBytes += size
        }
        return true
    }

    fun flushDue(elapsedNanos: Long): Boolean {
        require(elapsedNanos >= now)
        now = elapsedNanos
        val oldest = flightItems.firstOrNull()?.input ?: queued.firstOrNull()?.input
        return inFlight != null ||
            lossFirst != null ||
            terminal != null ||
            (pendingCount > 0 && pendingCount >= policy.events - policy.reservedEvents) ||
            (pendingBytes > 0 && pendingBytes >= policy.bytes - policy.reservedBytes) ||
            (oldest != null && elapsedNanos - oldest.stamp.elapsedNanos >= policy.flushNanos)
    }

    /** Handoff retains all capacity until commit. Null means no new batch is available. */
    fun takeBatch(): AdmissionBatch? {
        if (inFlight != null || (queued.isEmpty() && lossFirst == null && terminal == null))
            return null
        flightItems = queued.toList()
        queued.clear()
        val batch =
            AdmissionBatch(
                policy,
                java.util.List.copyOf(flightItems.map { it.input }),
                snapshotLoss(),
                terminal,
            )
        lossFirst = null
        lossLast = null
        lossCount = 0
        lossReasons.clear()
        terminal = null
        inFlight = batch
        return batch
    }

    fun committed(batch: AdmissionBatch) {
        require(inFlight === batch) { "Only the outstanding batch can be acknowledged" }
        for (item in flightItems) {
            pendingCount--
            pendingBytes -= item.bytes
            if (item.telemetry) {
                telemetryCount--
                telemetryBytes -= item.bytes
            }
        }
        flightItems = emptyList()
        inFlight = null
    }

    private fun stop(reason: InterruptionReason) {
        stopped = true
        terminal = reason
    }

    private fun recordLoss(stamp: ReceiptStamp, reason: AdmissionLossReason) {
        if (lossFirst == null) lossFirst = stamp
        lossLast = stamp
        if (lossCount < Long.MAX_VALUE) lossCount++
        lossReasons.add(reason)
    }

    private fun recordReason(reason: AdmissionLossReason) {
        lossReasons.add(reason)
    }

    private fun snapshotLoss(): AdmissionLoss? = lossFirst?.let {
        AdmissionLoss(it, checkNotNull(lossLast), lossCount, java.util.Set.copyOf(lossReasons))
    }

    private fun payloadCharge(input: SourceObservation): Int {
        val strings =
            listOfNotNull(
                input.source.adapterId,
                input.source.adapterVersion,
                input.normalizationVersion,
                (input.payload as? InputPayload.Profile)?.snapshot?.version,
            )
        var charge = 160L
        for (value in strings) {
            charge += 4
            var index = 0
            while (index < value.length) {
                val c = value[index++]
                charge +=
                    when {
                        c.code < 128 -> 1
                        c.code < 2048 -> 2
                        c.isHighSurrogate() &&
                            index < value.length &&
                            value[index].isLowSurrogate() -> {
                            index++
                            4
                        }
                        c.isSurrogate() -> 1
                        else -> 3
                    }
                if (charge > policy.envelopeBytes) return policy.envelopeBytes + 1
            }
        }
        return charge.toInt()
    }
}
