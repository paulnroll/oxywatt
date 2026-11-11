package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.util.UUID
import kotlin.math.*

/** Descriptive synthetic experiment only; no physiological estimator. */
enum class PowerHrCandidate {
    BAND,
    INTERPOLATION,
}

data class PowerHrSegment(
    val startSeconds: Double,
    val durationSeconds: Double,
    val power: Double,
    val hr: Double,
)

data class PowerHrValue(val watts: Double?, val reason: String?, val slope: Double? = null)

fun powerHrValue(
    segments: List<PowerHrSegment>,
    candidate: PowerHrCandidate,
    anchor: Double,
    band: Double,
    minimumVariance: Double,
    maximumResidual: Double,
): PowerHrValue {
    require(anchor.isFinite() && anchor > 0 && band.isFinite() && band >= 0)
    require(
        minimumVariance.isFinite() &&
            minimumVariance > 0 &&
            maximumResidual.isFinite() &&
            maximumResidual >= 0
    )
    segments.forEach {
        require(
            it.durationSeconds.isFinite() &&
                it.durationSeconds > 0 &&
                it.power.isFinite() &&
                it.power >= 0 &&
                it.hr.isFinite() &&
                it.hr > 0
        )
    }
    val selected =
        if (candidate == PowerHrCandidate.BAND)
            segments.filter { kotlin.math.abs(it.hr - anchor) <= band }
        else segments
    if (selected.isEmpty()) return PowerHrValue(null, "insufficient_anchor_support")
    val duration = selected.sumOf { it.durationSeconds }
    val p = selected.sumOf { it.durationSeconds * it.power } / duration
    if (!p.isFinite() || !duration.isFinite()) return PowerHrValue(null, "invalid_numeric_result")
    if (candidate == PowerHrCandidate.BAND) return PowerHrValue(p, null)
    if (selected.none { it.hr < anchor } || selected.none { it.hr > anchor })
        return PowerHrValue(null, "unbracketed_anchor")
    val h = selected.sumOf { it.durationSeconds * it.hr } / duration
    val variance = selected.sumOf { it.durationSeconds * (it.hr - h) * (it.hr - h) } / duration
    if (variance < minimumVariance) return PowerHrValue(null, "ill_conditioned")
    val slope =
        selected.sumOf { it.durationSeconds * (it.hr - h) * (it.power - p) } / (duration * variance)
    if (slope <= 0) return PowerHrValue(null, "nonpositive_slope")
    val intercept = p + slope * (anchor - h)
    val residual =
        kotlin.math.sqrt(
            selected.sumOf {
                it.durationSeconds *
                    (it.power - intercept - slope * (it.hr - anchor)).let { e -> e * e }
            } / duration
        )
    if (residual > maximumResidual) return PowerHrValue(null, "excessive_residual")
    if (!intercept.isFinite() || intercept < 0 || !slope.isFinite())
        return PowerHrValue(null, "invalid_numeric_result")
    return PowerHrValue(intercept, null, slope)
}

/** All numbers are explicit recipe parameters, not physiological defaults. */
data class PowerHrPolicy(
    val version: String,
    val windowNanos: Long,
    val settlingNanos: Long,
    val hrTtlNanos: Long,
    val powerTtlNanos: Long,
    val anchorBpm: Double,
    val bandBpm: Double,
    val maxHrSd: Double,
    val maxPowerSd: Double,
    val maxHrSlope: Double,
    val maxPowerSlope: Double,
    val transitionWatts: Double,
    val minimumHrVariance: Double,
    val maximumResidualWatts: Double,
    val maximumUpdates: Int,
    val maximumWindows: Int,
    val earliestWindowNanos: Long,
    val latestWindowNanos: Long,
) {
    init {
        require(windowNanos > 0 && settlingNanos >= 0 && hrTtlNanos > 0 && powerTtlNanos > 0)
        require(maximumUpdates > 0 && maximumWindows > 0)
        require(earliestWindowNanos >= 0 && latestWindowNanos > earliestWindowNanos)
        require(
            listOf(
                    anchorBpm,
                    bandBpm,
                    maxHrSd,
                    maxPowerSd,
                    maxHrSlope,
                    maxPowerSlope,
                    transitionWatts,
                    minimumHrVariance,
                    maximumResidualWatts,
                )
                .all { it.isFinite() && it >= 0 }
        )
        require(anchorBpm > 0 && minimumHrVariance > 0)
    }

    fun digest(): String =
        semanticDigest(
            "power-hr-policy-v1",
            listOf(
                version,
                windowNanos.toString(),
                settlingNanos.toString(),
                hrTtlNanos.toString(),
                powerTtlNanos.toString(),
                anchorBpm.toString(),
                bandBpm.toString(),
                maxHrSd.toString(),
                maxPowerSd.toString(),
                maxHrSlope.toString(),
                maxPowerSlope.toString(),
                transitionWatts.toString(),
                minimumHrVariance.toString(),
                maximumResidualWatts.toString(),
                maximumUpdates.toString(),
                maximumWindows.toString(),
                earliestWindowNanos.toString(),
                latestWindowNanos.toString(),
            ),
        )
}

data class PowerHrWindow(
    val segment: PowerHrSegment,
    val hrSd: Double,
    val powerSd: Double,
    val hrSlope: Double,
    val powerSlope: Double,
    val reason: String?,
    val firstPower: Double,
    val lastPower: Double,
    val firstHr: Double,
    val lastHr: Double,
)

data class PowerHrSession(
    val session: java.util.UUID,
    val cohort: String,
    val candidate: PowerHrCandidate,
    val value: PowerHrValue,
    val windows: List<PowerHrWindow>,
    val flags: Set<String>,
    val updates: Int,
    val peakWindows: Int,
)

/** Streaming two-channel hold over committed lifecycle effects; development use only. */
class PowerHrReplay(
    val sessionId: UUID,
    val policy: PowerHrPolicy,
    val profile: ExperimentProfile,
    val sensorCalibration: String?,
) {
    private var session: SessionSnapshot? = null
    private var now = 0L
    private var recording = false
    private var power: Double? = null
    private var hr: Double? = null
    private var powerExpiry = Long.MAX_VALUE
    private var hrExpiry = Long.MAX_VALUE
    private var start = 0L
    private var ready = 0L
    private var moments = Moments()
    private val windows = mutableListOf<PowerHrWindow>()
    private val flags = linkedSetOf<String>()
    private val timeMeanings = sortedSetOf<String>()
    private var updates = 0
    private var lastSequence: Long? = null
    private var stopped = false

    private fun plus(t: Long, d: Long) = if (t > Long.MAX_VALUE - d) Long.MAX_VALUE else t + d

    private fun reset(t: Long) {
        if (moments.duration > 0) flags.add("incomplete_window")
        moments = Moments()
        start = t
        ready = plus(t, policy.settlingNanos)
    }

    private fun clear(t: Long) {
        power = null
        hr = null
        reset(t)
    }

    fun accept(update: LifecycleUpdate, loss: AdmissionLoss? = null) {
        if (stopped) return
        if (updates == policy.maximumUpdates) {
            flags.add("resource_limit")
            stopped = true
            return
        }
        updates++
        if (policy.version != "power-hr-synthetic-v1") {
            flags.add("unsupported_policy")
            stopped = true
            return
        }
        val change = update.sessionChanges.singleOrNull { it.id == sessionId }
        val prior = session
        if (change != null) {
            if (prior != null)
                require(
                    change.epochId == prior.epochId &&
                        change.policy == prior.policy &&
                        change.source.adapterId == prior.source.adapterId &&
                        change.source.adapterVersion == prior.source.adapterVersion &&
                        change.normalizationVersion == prior.normalizationVersion
                )
            session = change
        }
        val current = session ?: return
        if (
            current.policy.version != "preview-silence-v1" ||
                current.normalizationVersion !in setOf("canonical-v1", "fit-normalization-v1")
        ) {
            flags.add("unsupported_policy")
            stopped = true
            return
        }
        if (update.rejection != null) flags.add("unsupported_timing")
        if (
            loss != null &&
                loss.first.epochId == current.epochId &&
                loss.last.ingestSequence >= current.firstReceipt.ingestSequence
        ) {
            flags.add("admission_loss")
            stopped = true
            return
        }
        val sameEpoch = update.checkpoint.epochId == current.epochId
        val breaks =
            if (sameEpoch)
                update.coverageBreaks.filter {
                    it.sessionId == sessionId && it.metric != Metric.CADENCE_RPM
                }
            else emptyList()
        val envelope = update.envelope?.takeIf { it.sessionId == sessionId }
        val horizon =
            maxOf(
                now,
                if (sameEpoch) update.checkpoint.lastReceipt?.elapsedNanos ?: now else now,
                breaks.maxOfOrNull { it.detectedAtNanos } ?: now,
            )
        for (group in breaks.groupBy { it.detectedAtNanos }.toSortedMap()) {
            require(group.key >= now)
            advance(group.key)
            group.value.forEach { if (it.metric == Metric.POWER_W) power = null else hr = null }
            flags.add("coverage_break")
            reset(group.key)
        }
        advance(horizon)
        if (change != null) {
            if (
                prior == null ||
                    change.phase != prior.phase ||
                    change.segmentIndex != prior.segmentIndex
            ) {
                clear(horizon)
                recording = change.phase == SessionPhase.RECORDING
            }
        }
        if (envelope != null) {
            require(
                envelope.stamp.epochId == current.epochId && envelope.stamp.elapsedNanos == horizon
            )
            require(lastSequence?.let { envelope.stamp.ingestSequence > it } ?: true)
            lastSequence = envelope.stamp.ingestSequence
            if (envelope.payload is InputPayload.Profile) {
                if (envelope.payload.snapshot != profile.snapshot)
                    flags.add("configuration_changed")
                clear(horizon)
            }
            val reading = envelope.payload as? InputPayload.MetricReading
            if (
                reading != null &&
                    reading.metric != Metric.CADENCE_RPM &&
                    update.recordingSegment != null
            ) {
                timeMeanings.add(reading.sourceTime?.meaning?.name ?: "RECEIPT_ONLY")
                val pairedBefore = power != null && hr != null
                if (reading.metric == Metric.POWER_W) {
                    if (
                        reading.value != null &&
                            power != null &&
                            abs(reading.value - power!!) > policy.transitionWatts
                    ) {
                        flags.add("workload_transition")
                        reset(horizon)
                    }
                    power = reading.value
                    powerExpiry = plus(horizon, policy.powerTtlNanos)
                } else {
                    hr = reading.value
                    hrExpiry = plus(horizon, policy.hrTtlNanos)
                }
                if (reading.value == null) {
                    flags.add("missing_channel")
                    reset(horizon)
                } else if (!pairedBefore && power != null && hr != null) reset(horizon)
            }
        }
    }

    private fun advance(to: Long) {
        require(to >= now)
        while (now < to && !stopped) {
            val end =
                if (recording && power != null && hr != null)
                    minOf(to, powerExpiry, hrExpiry, plus(start, policy.windowNanos))
                else
                    minOf(
                        to,
                        if (power != null) powerExpiry else Long.MAX_VALUE,
                        if (hr != null) hrExpiry else Long.MAX_VALUE,
                    )
            if (recording && power != null && hr != null && end > now)
                moments.add(
                    (now - start) / 1e9,
                    (end - start) / 1e9,
                    power!!,
                    hr!!,
                    policy.windowNanos / 1e9,
                )
            now = end
            if (
                recording && power != null && hr != null && now == plus(start, policy.windowNanos)
            ) {
                if (windows.size == policy.maximumWindows) {
                    flags.add("resource_limit")
                    stopped = true
                    return
                }
                val window =
                    moments.window(
                        start / 1e9,
                        policy.windowNanos / 1e9,
                        when {
                            start < ready -> "unsettled_response"
                            start - session!!.firstReceipt.elapsedNanos <
                                policy.earliestWindowNanos ||
                                now - session!!.firstReceipt.elapsedNanos >
                                    policy.latestWindowNanos -> "time_stratum"
                            else -> null
                        },
                        policy,
                    )
                windows.add(window)
                window.reason?.let(flags::add)
                moments = Moments()
                start = now
            }
            if (power != null && powerExpiry <= now) {
                power = null
                flags.add("channel_expired")
                reset(now)
            }
            if (hr != null && hrExpiry <= now) {
                hr = null
                flags.add("channel_expired")
                reset(now)
            }
        }
    }

    fun result(candidate: PowerHrCandidate): PowerHrSession {
        val current = session
        val cohort =
            semanticDigest(
                "power-hr-cohort-v1",
                listOf(
                    policy.digest(),
                    profile.digest(),
                    sensorCalibration,
                    current?.source?.adapterId,
                    current?.source?.adapterVersion,
                    current?.normalizationVersion,
                ) + timeMeanings,
            )
        val reason =
            when {
                "resource_limit" in flags -> "resource_limit"
                "unsupported_policy" in flags -> "unsupported_policy"
                "unsupported_timing" in flags -> "unsupported_timing"
                "admission_loss" in flags -> "admission_loss"
                current?.trendEligible != true -> "ambiguous_session"
                "configuration_changed" in flags -> "configuration_changed"
                profile.context == "unknown" ||
                    profile.calibration == null ||
                    sensorCalibration.isNullOrBlank() -> "context_unverified"
                windows.isEmpty() -> "no_paired_coverage"
                windows.none { it.reason == null } -> windows.first().reason
                else -> null
            }
        val value =
            if (reason != null) PowerHrValue(null, reason)
            else
                powerHrValue(
                    windows.filter { it.reason == null }.map { it.segment },
                    candidate,
                    policy.anchorBpm,
                    policy.bandBpm,
                    policy.minimumHrVariance,
                    policy.maximumResidualWatts,
                )
        return PowerHrSession(
            sessionId,
            cohort,
            candidate,
            value,
            windows.toList(),
            flags.toSet(),
            updates,
            windows.size,
        )
    }
}

private class Moments {
    var duration = 0.0
    var p = 0.0
    var h = 0.0
    var pp = 0.0
    var hh = 0.0
    var pt = 0.0
    var ht = 0.0
    var firstP = 0.0
    var firstH = 0.0
    var lastP = 0.0
    var lastH = 0.0

    fun add(a: Double, b: Double, power: Double, hr: Double, length: Double) {
        val firstDuration = max(0.0, min(b, length / 2) - a)
        val lastDuration = (b - a) - firstDuration
        firstP += firstDuration * power
        firstH += firstDuration * hr
        lastP += lastDuration * power
        lastH += lastDuration * hr
        val d = b - a
        val t = (b * b - a * a) / 2
        duration += d
        p += d * power
        h += d * hr
        pp += d * power * power
        hh += d * hr * hr
        pt += t * power
        ht += t * hr
    }

    fun window(
        start: Double,
        length: Double,
        initialReason: String?,
        policy: PowerHrPolicy,
    ): PowerHrWindow {
        val pm = p / duration
        val hm = h / duration
        val psd = sqrt(max(0.0, pp / duration - pm * pm))
        val hsd = sqrt(max(0.0, hh / duration - hm * hm))
        val ps = (pt / duration - pm * length / 2) / (length * length / 12)
        val hs = (ht / duration - hm * length / 2) / (length * length / 12)
        val reason =
            initialReason
                ?: when {
                    abs(hs) > policy.maxHrSlope || abs(ps) > policy.maxPowerSlope ->
                        "excessive_drift"
                    hsd > policy.maxHrSd || psd > policy.maxPowerSd -> "unstable_window"
                    pm <= 0 -> "nonqualifying_work"
                    else -> null
                }
        return PowerHrWindow(
            PowerHrSegment(start, duration, pm, hm),
            hsd,
            psd,
            hs,
            ps,
            reason,
            firstP / (length / 2),
            lastP / (length / 2),
            firstH / (length / 2),
            lastH / (length / 2),
        )
    }
}

/** Invented stress configuration; no real-use eligibility inference. */
fun powerHrSyntheticPolicy() =
    PowerHrPolicy(
        "power-hr-synthetic-v1",
        10_000_000_000,
        0,
        3_000_000_000,
        3_000_000_000,
        150.0,
        15.0,
        2.0,
        20.0,
        0.5,
        5.0,
        50.0,
        1.0,
        1.0,
        4096,
        256,
        0,
        60_000_000_000,
    )

fun powerHrSyntheticProfile() =
    experimentProfiles()[1].copy(context = "indoor", calibration = "invented-calibration-1")

/** Explicit mutation recipes on the existing descriptor, never a second manifest/registry. */
fun powerHrFixture(kind: String, offset: Double = 0.0): ExperimentFixture {
    require(
        kind in
            setOf(
                "standard",
                "density",
                "distribution",
                "hr-offset",
                "drift",
                "lag",
                "missing",
                "gap",
                "pause",
                "ambiguous",
                "profile",
                "unknown-context",
                "calibration",
                "expiry",
                "zero",
                "unstable",
                "transition",
                "irregular",
            )
    )
    val base = experimentFixture("constant")
    val profile =
        powerHrSyntheticProfile().let {
            when (kind) {
                "unknown-context" -> it.copy(context = "unknown")
                "calibration" -> it.copy(calibration = "invented-calibration-2")
                else -> it
            }
        }
    val profiles =
        listOf(profile.copy(schema = "ftp-input-profile-v1"), profile) +
            if (kind == "profile")
                listOf(
                    profile.copy(snapshot = profile.snapshot.copy(version = "invented-profile-2"))
                )
            else emptyList()
    val end = if (kind == "distribution") 30L else if (kind == "irregular") 10L else 20L
    val events = mutableListOf<ScheduledObservation>()
    fun event(t: Long, p: InputPayload) {
        events.add(ScheduledObservation(t, p))
    }
    if (kind != "ambiguous") event(0, InputPayload.Recording(RecordingState.IDLE))
    event(0, InputPayload.Recording(RecordingState.RECORDING))
    event(0, InputPayload.Profile(profile.snapshot))
    val ticks =
        if (kind == "irregular") listOf(0L, 2_000_000_000, 7_000_000_000, 8_000_000_000)
        else
            (0L until end * (if (kind == "density") 2 else 1)).map {
                it * (if (kind == "density") 500_000_000 else 1_000_000_000)
            }
    for (t in ticks) {
        val second = t / 1_000_000_000
        if (kind == "pause" && second == 9L) event(t, InputPayload.Recording(RecordingState.PAUSED))
        if (kind == "pause" && second == 11L)
            event(t, InputPayload.Recording(RecordingState.RECORDING))
        if (kind == "profile" && second == 10L)
            event(t, InputPayload.Profile(profile.snapshot.copy(version = "invented-profile-2")))
        if (kind == "gap" && second in 4L..8L) continue
        val h =
            when {
                kind == "irregular" -> 150.0
                kind == "distribution" && second < 10 -> 145.0
                kind == "lag" -> 160.0
                else -> if (second < 10) 140.0 else 160.0
            }
        val power =
            when (kind) {
                "irregular" ->
                    when (second) {
                        0L -> 100.0
                        2L -> 200.0
                        else -> 0.0
                    }
                "lag" -> if (second < 10) 200.0 else 220.0
                "zero" -> 0.0
                "unstable" -> if (second % 2 == 0L) 180.0 else 240.0
                "transition" -> if (second < 5) 100.0 else 300.0
                else -> h + 60.0 + offset
            }
        event(t, InputPayload.MetricReading(Metric.POWER_W, power))
        if (kind != "expiry" || second == 0L || second == 10L)
            event(
                t,
                InputPayload.MetricReading(
                    Metric.HEART_RATE_BPM,
                    if (kind == "missing") null
                    else
                        h +
                            (if (kind == "hr-offset") -5.0
                            else if (kind == "drift") (second % 10).toDouble() else 0.0),
                    if (kind == "missing") MissingReason.DROPOUT else null,
                ),
            )
    }
    event(end * 1_000_000_000, InputPayload.Recording(RecordingState.IDLE))
    return base.copy(
        id = "power-hr-$kind-$offset",
        manifest =
            base.manifest.copy(scenario = "power-hr-$kind-$offset", endNanos = end * 1_000_000_000),
        events = events,
        profiles = profiles,
        expectedCount = events.size,
        expectedEnd = end * 1_000_000_000,
        expectation = "T08a1 pinned H01-H07 tables; synthetic only",
        sourceRevision = "c2f5a81",
        recipe = "explicit power/HR stages and named mutation; no RNG; synthetic stress only",
    )
}

data class PowerHrPoint(
    val session: UUID,
    val generation: Long,
    val atSeconds: Long,
    val cohort: String,
    val watts: Double?,
    val reason: String?,
)

data class PowerHrChange(
    val point: PowerHrPoint,
    val deltaWatts: Double?,
    val percent: Double?,
    val direction: Int?,
)

data class PowerHrReport(
    val asOfSeconds: Long?,
    val thresholdWatts: Double,
    val baseline: Double?,
    val baselineReason: String?,
    val contributors: Map<UUID, Long>,
    val rows: List<PowerHrChange>,
    val screened: Int,
    val numeric: Int,
    val refusals: Map<String, Int>,
)

/** Explicit fixed baseline; no rolling replacement, smoothing or physiological interpretation. */
fun powerHrReport(
    points: List<PowerHrPoint>,
    baselineIds: List<UUID>,
    cohort: String,
    thresholdWatts: Double,
): PowerHrReport {
    require(points.size <= 256 && points.map { it.session }.distinct().size == points.size)
    require(
        baselineIds.isNotEmpty() &&
            baselineIds.size <= 256 &&
            baselineIds.distinct().size == baselineIds.size
    )
    require(thresholdWatts.isFinite() && thresholdWatts >= 0)
    points.forEach {
        require(
            it.generation > 0 &&
                ((it.watts != null) == (it.reason == null)) &&
                (it.watts == null || it.watts.isFinite())
        )
    }
    val base = baselineIds.map { id -> points.singleOrNull { it.session == id } }
    val reason =
        when {
            base.any { it == null || it.watts == null } -> "baseline_unavailable"
            base.any { it!!.cohort != cohort } -> "baseline_incompatible"
            base.any { it!!.watts!! <= 0 } -> "baseline_nonpositive"
            else -> null
        }
    val b = if (reason == null) base.sumOf { it!!.watts!! / base.size } else null
    val rows = points.map { p ->
        val d = if (b != null && p.cohort == cohort) p.watts?.minus(b) else null
        PowerHrChange(
            p,
            d,
            d?.let { 100 * (it / b!!) },
            d?.let { if (it > thresholdWatts) 1 else if (it < -thresholdWatts) -1 else 0 },
        )
    }
    return PowerHrReport(
        points.maxOfOrNull { it.atSeconds },
        thresholdWatts,
        b,
        reason,
        if (b == null) emptyMap() else base.associate { it!!.session to it.generation },
        rows,
        points.size,
        points.count { it.watts != null },
        points.filter { it.watts == null }.groupingBy { it.reason!! }.eachCount(),
    )
}

data class PowerHrDetection(
    val falseIncreases: Int,
    val falseDecreases: Int,
    val nullComparisons: Int,
    val delaySeconds: Long?,
    val eligibleSessionDelay: Int?,
    val missed: Boolean,
)

/** Truth is the injected observational change, never human physiological ground truth. */
fun powerHrDetection(
    report: PowerHrReport,
    truth: List<Int>,
    changeAtSeconds: Long?,
    changeDirection: Int,
): PowerHrDetection {
    require(truth.size == report.rows.size && truth.all { it in -1..1 } && changeDirection in -1..1)
    val nullRows =
        report.rows.zip(truth).filter {
            it.second == 0 && it.first.point.session !in report.contributors
        }
    val after =
        report.rows.filter {
            changeAtSeconds != null && it.point.atSeconds >= changeAtSeconds && it.direction != null
        }
    val detected = after.indexOfFirst { it.direction == changeDirection && changeDirection != 0 }
    return PowerHrDetection(
        nullRows.count { it.first.direction == 1 },
        nullRows.count { it.first.direction == -1 },
        nullRows.size,
        if (detected >= 0) after[detected].point.atSeconds - changeAtSeconds!! else null,
        if (detected >= 0) detected + 1 else null,
        changeAtSeconds != null && detected < 0,
    )
}
