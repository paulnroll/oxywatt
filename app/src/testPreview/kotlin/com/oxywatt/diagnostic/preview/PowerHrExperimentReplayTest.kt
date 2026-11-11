package com.oxywatt.diagnostic.preview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import com.oxywatt.core.collection.simulation.*
import com.oxywatt.diagnostic.persistence.*
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class PowerHrExperimentReplayTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = UUID(0, 900)

    private fun policy(kind: String) =
        when (kind) {
            "irregular" ->
                powerHrSyntheticPolicy()
                    .copy(
                        hrTtlNanos = 20_000_000_000,
                        powerTtlNanos = 20_000_000_000,
                        transitionWatts = 1000.0,
                        maxPowerSd = 1000.0,
                        maxPowerSlope = 1000.0,
                    )
            "unstable" -> powerHrSyntheticPolicy().copy(transitionWatts = 100.0)
            else -> powerHrSyntheticPolicy()
        }

    private fun acquire(name: String, f: ExperimentFixture, speed: Int?, id: UUID = session) {
        f.validate(true)
        val lifecycle =
            CollectorLifecycle(
                f.manifest.epoch,
                LifecyclePolicy("preview-silence-v1", 6_000_000_000),
                { id },
            )
        val admission = BoundedAdmission()
        val clock = VirtualTimeline()
        InputRepository(context, name).use { inputs ->
            fun flush() {
                val batch = admission.takeBatch() ?: return
                inputs.commit(inputs.prepare(batch, lifecycle, inputs.state().revision))
                admission.committed(batch)
            }
            for (t in 1L..f.expectedEnd / 1_000_000_000) clock.at(t * 1_000_000_000) {
                flush()
                inputs.commit(
                    inputs.prepareEffect(
                        lifecycle.advanceTime(clock.now),
                        AdmissionPolicy(),
                        inputs.state().revision,
                    )
                )
            }
            val source =
                SimulatedRideDataSource(
                    f.observations(),
                    clock,
                    f.manifest.epoch,
                    f.manifest.origin,
                    f.expectedEnd,
                    speed = speed,
                )
            source
                .start({
                    check(admission.offer(it))
                    flush()
                }) {
                    it?.let { e -> throw e }
                }
                .use { while (source.pump()) {} }
        }
    }

    private data class Calculation(
        val results: List<PowerHrSession>,
        val slots: List<AnalysisSlot>,
        val hash: String,
        val revision: Long,
    )

    private fun calculate(
        name: String,
        f: ExperimentFixture,
        p: PowerHrPolicy = powerHrSyntheticPolicy(),
        id: UUID = session,
    ): Calculation {
        require(name.startsWith("oxywatt-synthetic-power-hr-"))
        f.validate(true)
        InputRepository(context, name).use { inputs ->
            val revision = inputs.state().revision
            check(revision > 0) { "Replay inputs unavailable/deleted" }
            val replay = PowerHrReplay(id, p, f.profiles[1], "invented-sensor/calibration-1")
            val hash = PowerReplayHash()
            hash.token("SYNTHETIC:${f.linkedManifest().hash()}:${f.digest()}")
            var after = 0L
            var index = -1
            var count = 0
            while (true) {
                val page = inputs.replay(revision, after, index, 7)
                if (page.isEmpty()) break
                for (row in page) {
                    check(row.metadata.policy.version == "admission-v1")
                    val u = row.update
                    if (
                        u.envelope?.sessionId == id ||
                            u.sessionChanges.any { it.id == id } ||
                            u.coverageBreaks.any { it.sessionId == id }
                    ) {
                        check(count++ < 4096) { "Bounded replay work exceeded" }
                        hash.document(CollectorCodec.update(u))
                        hash.document(
                            CollectorCodec.metadata(
                                row.metadata.policy,
                                row.metadata.loss,
                                row.metadata.terminal,
                            )
                        )
                        replay.accept(u, row.metadata.loss)
                    }
                    // Timer updates with no effect have no additional committed horizon.
                }
                after = page.last().revision
                index = page.last().index
            }
            val digest = hash.finish()
            val results = PowerHrCandidate.entries.map { replay.result(it) }
            val slots = results.map {
                AnalysisSlot(id, "experimental-power-at-hr:${it.candidate}", it.cohort)
            }
            AnalysisRepository(context, name).use { analyses ->
                for ((r, slot) in results.zip(slots)) {
                    val provenance = provenance(digest, p, f, slot.metric, revision)
                    val request = UUID.randomUUID()
                    val generation = analyses.request(slot, request, provenance)
                    check(generation == analyses.request(slot, request, provenance))
                    val terminal =
                        if (r.value.watts != null)
                            AnalysisTerminal(
                                AnalysisStatus.AVAILABLE,
                                Instant.EPOCH,
                                r.value.watts,
                                "W",
                                "SYNTHETIC_EXPERIMENTAL_NOT_PHYSIOLOGICAL",
                            )
                        else
                            AnalysisTerminal(
                                AnalysisStatus.UNAVAILABLE,
                                Instant.EPOCH,
                                reason = r.value.reason,
                            )
                    analyses.complete(generation, terminal)
                    analyses.complete(generation, terminal)
                    assertEquals(terminal, analyses.newest(slot)!!.terminal)
                    assertTrue(analyses.membership("real-history").isEmpty())
                }
            }
            return Calculation(results, slots, digest, revision)
        }
    }

    private fun provenance(
        hash: String,
        p: PowerHrPolicy,
        f: ExperimentFixture,
        candidate: String,
        revision: Long = 0,
    ) =
        AnalysisProvenance(
            hash,
            "SYNTHETIC:${f.manifest.normalizationVersion}:revision=$revision",
            f.profiles[1].digest(),
            "power-hr-synthetic-v1:$candidate",
            "joint-hold-grid-v1",
            p.digest(),
            f.profiles[1].digest(),
            "fixed-recipe-baseline-v1",
            Instant.EPOCH,
            com.oxywatt.diagnostic.BuildConfig.CODE_REVISION,
        )

    private fun guardedReport(
        analyses: AnalysisRepository,
        slot: AnalysisSlot,
        beforeRecheck: () -> Unit = {},
    ): PowerHrReport {
        val newest = checkNotNull(analyses.newest(slot))
        val current = analyses.current(slot)
        val point =
            PowerHrPoint(
                slot.session,
                newest.generation,
                0,
                slot.cohort,
                current?.terminal?.value,
                if (current == null) newest.terminal?.reason ?: newest.status.name else null,
            )
        val report = powerHrReport(listOf(point), listOf(slot.session), slot.cohort, 5.0)
        beforeRecheck()
        check(
            analyses.newest(slot)?.generation == newest.generation &&
                analyses.newest(slot)?.status == newest.status
        ) {
            "Report contributors changed"
        }
        return report
    }

    @Test
    fun policyBoundariesPersistAtAllSpeedsWithoutNewStorage() {
        for (p in
            listOf(
                powerHrSyntheticPolicy().copy(settlingNanos = 10_000_000_000),
                powerHrSyntheticPolicy().copy(maximumWindows = 1),
                powerHrSyntheticPolicy().copy(maximumUpdates = 5),
                powerHrSyntheticPolicy().copy(version = "unknown"),
            )) {
            var reference: Calculation? = null
            for (speed in listOf(1, 10, null)) {
                val name = "oxywatt-synthetic-power-hr-${UUID.randomUUID()}.db"
                try {
                    val f = powerHrFixture("standard")
                    acquire(name, f, speed)
                    val c = calculate(name, f, p)
                    if (reference == null) reference = c else assertEquals(reference, c)
                    AnalysisRepository(context, name).use { analyses ->
                        c.slots.forEach { slot ->
                            assertEquals(
                                analyses.newest(slot)?.terminal,
                                analyses.run(analyses.newest(slot)!!.generation)?.terminal,
                            )
                        }
                    }
                    val expected =
                        when {
                            p.version == "unknown" -> "unsupported_policy"
                            p.maximumWindows == 1 || p.maximumUpdates == 5 -> "resource_limit"
                            else -> null
                        }
                    assertEquals(expected, c.results[0].value.reason)
                    if (expected == null) {
                        assertEquals(220.0, c.results[0].value.watts)
                        assertEquals("unbracketed_anchor", c.results[1].value.reason)
                    }
                } finally {
                    context.deleteDatabase(name)
                }
            }
        }
    }

    @Test
    fun longitudinalSyntheticSeriesReportCoverageFalseDirectionAndDetectionDelay() {
        val series =
            mapOf(
                "null" to listOf(0.0, 0.0, 0.0, 0.0, 0.0),
                "step-up" to listOf(0.0, 0.0, 10.0, 10.0, 10.0),
                "step-down" to listOf(0.0, 0.0, -10.0, -10.0, -10.0),
                "ramp" to listOf(0.0, 0.0, 5.0, 10.0, 15.0),
                "reversal" to listOf(0.0, 0.0, 10.0, -10.0, 0.0),
                "power-confound" to listOf(0.0, 0.0, 10.0, 10.0, 10.0),
                "hr-confound" to List(5) { 0.0 },
                "distribution-confound" to List(5) { 0.0 },
            )
        for ((label, offsets) in series) {
            var baselineReports: List<PowerHrReport>? = null
            for (speed in listOf(1, 10, null)) {
                val name = "oxywatt-synthetic-power-hr-${UUID.randomUUID()}.db"
                try {
                    val calculations = offsets.mapIndexed { i, offset ->
                        val id = UUID(0, 1000L + i)
                        val kind =
                            if (i < 2) "standard"
                            else
                                when (label) {
                                    "hr-confound" -> "hr-offset"
                                    "distribution-confound" -> "distribution"
                                    else -> "standard"
                                }
                        val original = powerHrFixture(kind, offset)
                        val f =
                            original.copy(
                                manifest =
                                    original.manifest.copy(
                                        epoch = UUID(0, 1100L + i),
                                        origin = Instant.EPOCH.plusSeconds(i * 100L),
                                    )
                            )
                        acquire(name, f, speed, id)
                        id to calculate(name, f, policy(kind), id)
                    }
                    val reports =
                        AnalysisRepository(context, name).use { analyses ->
                            PowerHrCandidate.entries.mapIndexed { candidateIndex, candidate ->
                                val runs = calculations.map { (_, c) ->
                                    analyses.newest(c.slots[candidateIndex])!!
                                }
                                val points = runs.mapIndexed { i, run ->
                                    PowerHrPoint(
                                        run.slot.session,
                                        run.generation,
                                        i * 100L,
                                        run.slot.cohort,
                                        analyses.current(run.slot)?.terminal?.value,
                                        if (analyses.current(run.slot) == null) run.status.name
                                        else null,
                                    )
                                }
                                val report =
                                    powerHrReport(
                                        points,
                                        calculations.take(2).map { it.first },
                                        points.first().cohort,
                                        5.0,
                                    )
                                runs.forEach {
                                    assertEquals(
                                        it.generation,
                                        analyses.newest(it.slot)!!.generation,
                                    )
                                }
                                assertEquals(5, report.screened)
                                assertEquals(5, report.numeric)
                                assertEquals(210.0, report.baseline)
                                val expected =
                                    when (label) {
                                        "distribution-confound" ->
                                            if (candidate == PowerHrCandidate.BAND)
                                                listOf(210.0, 210.0, 215.0, 215.0, 215.0)
                                            else List(5) { 210.0 }
                                        "hr-confound" ->
                                            if (candidate == PowerHrCandidate.INTERPOLATION)
                                                listOf(210.0, 210.0, 215.0, 215.0, 215.0)
                                            else List(5) { 210.0 }
                                        else -> offsets.map { 210 + it }
                                    }
                                assertEquals(expected, points.map { it.watts })
                                val truth =
                                    when {
                                        label.endsWith("confound") || label == "null" ->
                                            List(5) { 0 }
                                        else ->
                                            offsets.map { if (it > 0) 1 else if (it < 0) -1 else 0 }
                                    }
                                val changeAt = if (truth.any { it != 0 }) 200L else null
                                val direction = truth.firstOrNull { it != 0 } ?: 0
                                for (q in listOf(2.0, 5.0)) {
                                    val evaluated =
                                        powerHrReport(
                                            points,
                                            calculations.take(2).map { it.first },
                                            points.first().cohort,
                                            q,
                                        )
                                    val detection =
                                        powerHrDetection(evaluated, truth, changeAt, direction)
                                    if (label == "null")
                                        assertEquals(
                                            0,
                                            detection.falseIncreases + detection.falseDecreases,
                                        )
                                    if (label == "power-confound")
                                        assertEquals(3, detection.falseIncreases)
                                    if (label == "ramp" && q == 5.0) {
                                        assertEquals(100L, detection.delaySeconds)
                                        assertEquals(2, detection.eligibleSessionDelay)
                                    }
                                    if (speed == null)
                                        println(
                                            "T08a1 series=$label candidate=$candidate q=$q raw=${points.map {it.watts}} coverage=${report.numeric}/${report.screened} detection=$detection"
                                        )
                                }
                                report
                            }
                        }
                    // IDs/generations are fixed; request UUIDs/database paths are execution-only.
                    if (baselineReports == null) baselineReports = reports
                    else assertEquals(baselineReports, reports)
                } finally {
                    context.deleteDatabase(name)
                }
            }
        }
    }

    @Test
    fun everyMutationTraversesActualTimersAndReopensWithoutAnotherRequest() {
        val kinds =
            listOf(
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
        for (kind in kinds) {
            var reference: Calculation? = null
            for (speed in listOf(1, 10, null)) {
                val name = "oxywatt-synthetic-power-hr-${UUID.randomUUID()}.db"
                try {
                    val f = powerHrFixture(kind)
                    acquire(name, f, speed)
                    val c = calculate(name, f, policy(kind))
                    if (reference == null) reference = c else assertEquals(reference, c)
                    val expected =
                        when (kind) {
                            "standard",
                            "density",
                            "calibration" -> listOf(210.0, 210.0)
                            "distribution" -> listOf(215.0, 210.0)
                            "hr-offset" -> listOf(210.0, 215.0)
                            "lag" -> listOf(210.0, null)
                            "irregular" -> listOf(120.0, null)
                            else -> listOf(null, null)
                        }
                    assertEquals(kind, expected, c.results.map { it.value.watts })
                    AnalysisRepository(context, name).use { analyses ->
                        for ((r, slot) in c.results.zip(c.slots)) {
                            val saved = analyses.newest(slot)!!
                            assertEquals(r.value.watts, saved.terminal!!.value)
                            assertEquals(r.value.reason, saved.terminal!!.reason)
                            val report = guardedReport(analyses, slot)
                            assertEquals(if (r.value.watts == null) 0 else 1, report.numeric)
                            assertEquals(saved.generation, analyses.newest(slot)!!.generation)
                        }
                    }
                    if (speed == null)
                        println(
                            "T08a1 $kind values=${c.results.map { it.value }} flags=${c.results[0].flags} hash=${c.hash} windows=${c.results[0].windows.size}"
                        )
                } finally {
                    context.deleteDatabase(name)
                }
            }
        }
    }

    @Test
    fun newestAuthorityRaceRetryAndDeletedInputsCannotProduceFallback() {
        val name = "oxywatt-synthetic-power-hr-${UUID.randomUUID()}.db"
        try {
            val f = powerHrFixture("standard")
            acquire(name, f, 1)
            val c = calculate(name, f)
            val slot = c.slots[0]
            AnalysisRepository(context, name).use { analyses ->
                val first = analyses.newest(slot)!!
                val p = provenance(c.hash, policy("standard"), f, slot.metric)
                val request = UUID.randomUUID()
                analyses.beforeCommit = { throw IllegalStateException("injected_request_rollback") }
                assertThrows(IllegalStateException::class.java) {
                    analyses.request(slot, request, p)
                }
                analyses.beforeCommit = {}
                assertEquals(first.generation, analyses.newest(slot)!!.generation)
                val next = analyses.request(slot, request, p)
                assertEquals(next, analyses.request(slot, request, p))
                assertNull(analyses.current(slot))
                assertNull(guardedReport(analyses, slot).baseline)
                analyses.complete(first.generation, first.terminal!!)
                assertEquals(next, analyses.newest(slot)!!.generation)
                analyses.complete(
                    next,
                    AnalysisTerminal(
                        AnalysisStatus.FAILED,
                        Instant.EPOCH,
                        reason = "injected_failure",
                    ),
                )
                assertNull(guardedReport(analyses, slot).baseline)
                assertEquals(mapOf("injected_failure" to 1), guardedReport(analyses, slot).refusals)
                val refused = analyses.request(slot, UUID.randomUUID(), p)
                analyses.complete(
                    refused,
                    AnalysisTerminal(
                        AnalysisStatus.UNAVAILABLE,
                        Instant.EPOCH,
                        reason = "insufficient_anchor_support",
                    ),
                )
                assertNull(guardedReport(analyses, slot).baseline)
                val good = analyses.request(slot, UUID.randomUUID(), p)
                analyses.complete(good, first.terminal!!)
                assertEquals(210.0, guardedReport(analyses, slot).baseline)
                assertThrows(IllegalStateException::class.java) {
                    guardedReport(analyses, slot) { analyses.request(slot, UUID.randomUUID(), p) }
                }
                val late = analyses.request(slot, UUID.randomUUID(), p)
                val authoritative = analyses.request(slot, UUID.randomUUID(), p)
                analyses.complete(late, first.terminal!!)
                assertEquals(authoritative, analyses.newest(slot)!!.generation)
                assertNull(analyses.current(slot))
                assertTrue(analyses.membership("real-history").isEmpty())
            }
            context.deleteDatabase(name)
            assertThrows(IllegalStateException::class.java) { calculate(name, f) }
            assertThrows(IllegalArgumentException::class.java) { calculate("real-history.db", f) }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun persistedThreeSpeedExperimentHasIndependentRawValues() {
        var reference: List<PowerHrSession>? = null
        for (speed in listOf(1, 10, null)) {
            val name = "oxywatt-synthetic-power-hr-${UUID.randomUUID()}.db"
            try {
                val f = powerHrFixture("standard")
                acquire(name, f, speed)
                val c = calculate(name, f)
                assertEquals(2, c.results.size)
                assertEquals(listOf(210.0, 210.0), c.results.map { it.value.watts })
                if (reference == null) reference = c.results else assertEquals(reference, c.results)
            } finally {
                context.deleteDatabase(name)
            }
        }
    }
}
