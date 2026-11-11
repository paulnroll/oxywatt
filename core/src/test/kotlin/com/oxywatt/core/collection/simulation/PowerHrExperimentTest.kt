package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID
import kotlin.test.*
import org.junit.Test

class PowerHrExperimentTest {
    @Test
    fun independentDurationAndInterpolationAnchors() {
        val band =
            powerHrValue(
                listOf(
                    PowerHrSegment(0.0, 10.0, 200.0, 150.0),
                    PowerHrSegment(10.0, 20.0, 230.0, 150.0),
                ),
                PowerHrCandidate.BAND,
                150.0,
                15.0,
                1.0,
                1.0,
            )
        assertEquals(220.0, band.watts)
        val line =
            powerHrValue(
                listOf(
                    PowerHrSegment(0.0, 10.0, 200.0, 140.0),
                    PowerHrSegment(10.0, 10.0, 220.0, 160.0),
                ),
                PowerHrCandidate.INTERPOLATION,
                150.0,
                15.0,
                1.0,
                1.0,
            )
        assertEquals(210.0, line.watts)
        assertEquals(1.0, line.slope)
    }

    @Test
    fun jointCoverageUsesTimeAndNeverFillsMissingHr() {
        val session = UUID(0, 700)
        val policy = powerHrSyntheticPolicy()
        fun run(missing: Boolean): PowerHrSession {
            val engine =
                PowerHrReplay(
                    session,
                    policy,
                    powerHrSyntheticProfile(),
                    "invented-sensor/calibration-1",
                )
            val collector =
                CollectorLifecycle(
                    UUID(0, 701),
                    LifecyclePolicy("preview-silence-v1", 6_000_000_000),
                    { session },
                )
            val events =
                mutableListOf<Pair<Long, InputPayload>>(
                    0L to InputPayload.Recording(RecordingState.IDLE),
                    0L to InputPayload.Recording(RecordingState.RECORDING),
                )
            for (t in 0L until 20) {
                events.add(t * 1_000_000_000 to InputPayload.MetricReading(Metric.POWER_W, 200.0))
                events.add(
                    t * 1_000_000_000 to
                        InputPayload.MetricReading(
                            Metric.HEART_RATE_BPM,
                            if (missing) null else 150.0,
                            if (missing) MissingReason.DROPOUT else null,
                        )
                )
            }
            events.add(20_000_000_000 to InputPayload.Recording(RecordingState.IDLE))
            events.forEachIndexed { i, (t, p) ->
                engine.accept(
                    collector.accept(
                        SourceObservation(
                            ReceiptStamp(Instant.EPOCH.plusNanos(t), UUID(0, 701), t, i.toLong()),
                            SourceRef("synthetic", "1"),
                            "canonical-v1",
                            p,
                        )
                    )
                )
            }
            return engine.result(PowerHrCandidate.BAND)
        }
        assertEquals(200.0, run(false).value.watts)
        assertNull(run(true).value.watts)
        assertEquals("no_paired_coverage", run(true).value.reason)
    }

    private fun replay(
        kind: String,
        policy: PowerHrPolicy = powerHrSyntheticPolicy(),
        offset: Double = 0.0,
    ): List<PowerHrSession> {
        val f = powerHrFixture(kind, offset)
        f.validate(true)
        val id = UUID(0, 700)
        val collector =
            CollectorLifecycle(
                f.manifest.epoch,
                LifecyclePolicy("preview-silence-v1", 6_000_000_000),
                { id },
            )
        val engine = PowerHrReplay(id, policy, f.profiles[1], "invented-sensor/calibration-1")
        f.observations().forEachIndexed { i, e ->
            engine.accept(
                collector.accept(
                    SourceObservation(
                        ReceiptStamp(
                            Instant.EPOCH.plusNanos(e.offsetNanos),
                            f.manifest.epoch,
                            e.offsetNanos,
                            i.toLong(),
                        ),
                        SourceRef("synthetic", "1"),
                        "canonical-v1",
                        e.payload,
                    )
                )
            )
        }
        return PowerHrCandidate.entries.map { engine.result(it) }
    }

    @Test
    fun pinnedMutationTablePreservesRefusalsAndObservedUnits() {
        for (kind in listOf("standard", "density")) replay(kind).forEach {
            assertEquals(210.0, it.value.watts, kind)
        }
        val expected =
            mapOf(
                "missing" to "no_paired_coverage",
                "expiry" to "no_paired_coverage",
                "pause" to "no_paired_coverage",
                "ambiguous" to "ambiguous_session",
                "profile" to "configuration_changed",
                "unknown-context" to "context_unverified",
                "drift" to "excessive_drift",
                "zero" to "nonqualifying_work",
            )
        expected.forEach { (kind, reason) ->
            replay(kind).forEach { assertEquals(reason, it.value.reason, kind) }
        }
        assertEquals(220.0, replay("standard", offset = 10.0)[0].value.watts)
        assertEquals(200.0, replay("standard", offset = -10.0)[1].value.watts)
        assertEquals(215.0, replay("distribution")[0].value.watts)
        assertEquals(210.0, replay("distribution")[1].value.watts)
        assertEquals(215.0, replay("hr-offset")[1].value.watts)
        assertEquals(
            120.0,
            replay(
                    "irregular",
                    powerHrSyntheticPolicy()
                        .copy(
                            hrTtlNanos = 20_000_000_000,
                            powerTtlNanos = 20_000_000_000,
                            transitionWatts = 1000.0,
                            maxPowerSd = 1000.0,
                            maxPowerSlope = 1000.0,
                        ),
                )[0]
                .value
                .watts,
        )
        val warm = replay("standard", powerHrSyntheticPolicy().copy(settlingNanos = 10_000_000_000))
        assertEquals(220.0, warm[0].value.watts)
        assertEquals("unbracketed_anchor", warm[1].value.reason)
        assertTrue(warm[0].windows.any { it.reason == "unsettled_response" })
        assertEquals(
            "unstable_window",
            replay("unstable", powerHrSyntheticPolicy().copy(transitionWatts = 100.0))[0]
                .value
                .reason,
        )
    }

    @Test
    fun supportConditioningResourceAndCohortBoundariesAreExplicit() {
        assertEquals("unbracketed_anchor", replay("lag")[1].value.reason)
        assertEquals(
            "resource_limit",
            replay("standard", powerHrSyntheticPolicy().copy(maximumWindows = 1))[0].value.reason,
        )
        assertEquals(
            "resource_limit",
            replay("standard", powerHrSyntheticPolicy().copy(maximumUpdates = 5))[0].value.reason,
        )
        assertEquals(
            "unsupported_policy",
            replay("standard", powerHrSyntheticPolicy().copy(version = "unknown"))[0].value.reason,
        )
        assertEquals(
            "ill_conditioned",
            replay("standard", powerHrSyntheticPolicy().copy(minimumHrVariance = 101.0))[1]
                .value
                .reason,
        )
        assertNotEquals(replay("standard")[0].cohort, replay("calibration")[0].cohort)
        assertTrue(replay("transition")[0].flags.contains("workload_transition"))
        assertTrue(replay("gap")[0].flags.contains("channel_expired"))
        assertNotEquals(powerHrFixture("standard").digest(), powerHrFixture("density").digest())
        assertNotEquals(
            powerHrSyntheticPolicy().digest(),
            powerHrSyntheticPolicy().copy(hrTtlNanos = 4).digest(),
        )
    }

    @Test
    fun baselineChangesAndFalseDirectionDelayNeverBecomePhysiology() {
        val ids = (0L..4).map { UUID(0, 800 + it) }
        fun points(values: List<Double?>) = values.mapIndexed { i, v ->
            PowerHrPoint(
                ids[i],
                i + 1L,
                i * 100L,
                "cohort",
                v,
                if (v == null) "unavailable" else null,
            )
        }
        val r =
            powerHrReport(
                points(listOf(210.0, 210.0, 220.0, 230.0, 210.0)),
                ids.take(2),
                "cohort",
                5.0,
            )
        assertEquals(210.0, r.baseline)
        assertEquals(listOf(0, 0, 1, 1, 0), r.rows.map { it.direction })
        assertEquals(10.0, r.rows[2].deltaWatts)
        val d = powerHrDetection(r, listOf(0, 0, 1, 1, 0), 200, 1)
        assertEquals(0, d.falseIncreases)
        assertEquals(0L, d.delaySeconds)
        assertEquals(1, d.eligibleSessionDelay)
        val confounded = powerHrDetection(r, listOf(0, 0, 0, 0, 0), null, 0)
        assertEquals(2, confounded.falseIncreases)
        assertEquals(3, confounded.nullComparisons)
        val negativeConfound =
            powerHrReport(points(listOf(210.0, 210.0, 200.0)), ids.take(2), "cohort", 5.0)
        val falseDrop = powerHrDetection(negativeConfound, listOf(0, 0, 0), null, 0)
        assertEquals(1, falseDrop.falseDecreases)
        assertEquals(1, falseDrop.nullComparisons)
        val refused = powerHrReport(points(listOf(null, 210.0, 220.0)), ids.take(2), "cohort", 5.0)
        assertNull(refused.baseline)
        assertTrue(refused.rows.all { it.direction == null })
        assertEquals(mapOf("unavailable" to 1), refused.refusals)
        val missed = powerHrDetection(r, listOf(0, 0, 0, -1, -1), 300, -1)
        assertTrue(missed.missed)
        assertNull(missed.delaySeconds)
    }

    @Test
    fun profileAndContextChangesSeparateCohortsWithoutInventingAlgebraicEffects() {
        fun calculate(profile: ExperimentProfile, sensor: String?): PowerHrSession {
            val f = powerHrFixture("standard")
            val id = UUID(0, 700)
            val collector =
                CollectorLifecycle(
                    f.manifest.epoch,
                    LifecyclePolicy("preview-silence-v1", 6_000_000_000),
                    { id },
                )
            val engine = PowerHrReplay(id, powerHrSyntheticPolicy(), profile, sensor)
            f.events.forEachIndexed { i, event ->
                val payload =
                    if (event.payload is InputPayload.Profile)
                        InputPayload.Profile(profile.snapshot)
                    else event.payload
                engine.accept(
                    collector.accept(
                        SourceObservation(
                            ReceiptStamp(
                                Instant.EPOCH.plusNanos(event.offsetNanos),
                                f.manifest.epoch,
                                event.offsetNanos,
                                i.toLong(),
                            ),
                            SourceRef("synthetic", "1"),
                            "canonical-v1",
                            payload,
                        )
                    )
                )
            }
            return engine.result(PowerHrCandidate.BAND)
        }
        val profile = powerHrSyntheticProfile()
        val reference = calculate(profile, "sensor-1")
        for (changed in
            listOf(
                profile.copy(
                    snapshot =
                        profile.snapshot.copy(
                            version = "mass-2",
                            massKg = ProfileSetting(90.0, SettingOrigin.MANUAL),
                        )
                ),
                profile.copy(
                    snapshot =
                        profile.snapshot.copy(
                            version = "hrmax-2",
                            configuredMaxHeartRateBpm =
                                ProfileSetting(180.0, SettingOrigin.CONFIGURED),
                        )
                ),
                profile.copy(context = "outdoor"),
            )) {
            val result = calculate(changed, "sensor-1")
            assertEquals(reference.value.watts, result.value.watts)
            assertNotEquals(reference.cohort, result.cohort)
            val report =
                powerHrReport(
                    listOf(PowerHrPoint(reference.session, 1, 0, reference.cohort, 210.0, null)),
                    listOf(reference.session),
                    result.cohort,
                    5.0,
                )
            assertEquals("baseline_incompatible", report.baselineReason)
        }
        assertNotEquals(reference.cohort, calculate(profile, "sensor-2").cohort)
        assertEquals("context_unverified", calculate(profile, null).value.reason)
    }

    @Test
    fun interpolationRefusesWrongSlopeResidualAndUnsupportedAnchor() {
        fun value(p: List<Double>, h: List<Double>, anchor: Double = 150.0) =
            powerHrValue(
                p.indices.map { PowerHrSegment(it * 10.0, 10.0, p[it], h[it]) },
                PowerHrCandidate.INTERPOLATION,
                anchor,
                15.0,
                1.0,
                1.0,
            )
        assertEquals("nonpositive_slope", value(listOf(220.0, 200.0), listOf(140.0, 160.0)).reason)
        assertEquals(
            "excessive_residual",
            value(listOf(200.0, 250.0, 220.0), listOf(140.0, 150.0, 160.0)).reason,
        )
        assertEquals(
            "unbracketed_anchor",
            value(listOf(200.0, 220.0), listOf(140.0, 160.0), 170.0).reason,
        )
        val drift = replay("drift")[0].windows.first()
        assertEquals(144.5, drift.segment.hr)
        assertEquals(0.99, drift.hrSlope, 1e-10)
        assertEquals(142.0, drift.firstHr)
        assertEquals(147.0, drift.lastHr)
        val capped = replay("standard", powerHrSyntheticPolicy().copy(maximumWindows = 1))[0]
        assertEquals(1, capped.peakWindows)
        assertEquals(
            5,
            replay("standard", powerHrSyntheticPolicy().copy(maximumUpdates = 5))[0].updates,
        )
    }

    @Test
    fun finiteInputsCannotPublishOverflowOrNonfiniteBaseline() {
        val huge =
            powerHrValue(
                listOf(PowerHrSegment(0.0, 10.0, Double.MAX_VALUE, 150.0)),
                PowerHrCandidate.BAND,
                150.0,
                1.0,
                1.0,
                1.0,
            )
        assertNull(huge.watts)
        val ids = listOf(UUID(0, 1), UUID(0, 2))
        val report =
            powerHrReport(
                ids.map { PowerHrPoint(it, 1, 0, "c", Double.MAX_VALUE, null) },
                ids,
                "c",
                5.0,
            )
        assertEquals(Double.MAX_VALUE, report.baseline)
    }

    @Test
    fun interruptedSessionsAndRecoveryFragmentsNeverContribute() {
        val f = powerHrFixture("standard")
        val id = UUID(0, 700)
        val collector =
            CollectorLifecycle(
                f.manifest.epoch,
                LifecyclePolicy("preview-silence-v1", 6_000_000_000),
                { id },
            )
        val engine = PowerHrReplay(id, powerHrSyntheticPolicy(), f.profiles[1], "sensor-1")
        f.events
            .takeWhile { it.offsetNanos < 5_000_000_000 }
            .forEachIndexed { i, e ->
                engine.accept(
                    collector.accept(
                        SourceObservation(
                            ReceiptStamp(
                                Instant.EPOCH.plusNanos(e.offsetNanos),
                                f.manifest.epoch,
                                e.offsetNanos,
                                i.toLong(),
                            ),
                            SourceRef("synthetic", "1"),
                            "canonical-v1",
                            e.payload,
                        )
                    )
                )
            }
        val checkpoint = collector.checkpoint()
        engine.accept(collector.sourceStopped(true))
        assertEquals("ambiguous_session", engine.result(PowerHrCandidate.BAND).value.reason)
        val epoch = UUID(0, 799)
        val fragmentId = UUID(0, 798)
        val recovery = CollectorLifecycle(epoch, checkpoint.policy, { fragmentId }, checkpoint)
        val fragment =
            PowerHrReplay(fragmentId, powerHrSyntheticPolicy(), f.profiles[1], "sensor-1")
        for ((i, state) in
            listOf(RecordingState.RECORDING, RecordingState.IDLE).withIndex()) fragment.accept(
            recovery.accept(
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, epoch, i.toLong(), i.toLong()),
                    SourceRef("synthetic", "1"),
                    "canonical-v1",
                    InputPayload.Recording(state),
                )
            )
        )
        assertEquals("ambiguous_session", fragment.result(PowerHrCandidate.BAND).value.reason)
    }

    @Test
    fun timeInRideStratumIsExplicitAndIncludedInPolicyIdentity() {
        val p = powerHrSyntheticPolicy().copy(latestWindowNanos = 10_000_000_000)
        val results = replay("standard", p)
        assertEquals(200.0, results[0].value.watts)
        assertEquals("unbracketed_anchor", results[1].value.reason)
        assertTrue(results[0].windows.any { it.reason == "time_stratum" })
        assertNotEquals(p.digest(), powerHrSyntheticPolicy().digest())
    }
}
