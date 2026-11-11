package com.oxywatt.diagnostic.preview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import com.oxywatt.core.collection.simulation.*
import com.oxywatt.diagnostic.persistence.*
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ExperimentReplayTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = UUID(0, 501)

    @Test
    fun descriptorFixturesTraverseVirtualTimersPersistAndReopenAtEverySpeed() {
        for (kind in listOf("constant", "irregular", "break-profile", "expiry")) {
            val f = experimentFixture(kind)
            f.validate(publication = true)
            var baseline: StoredPowerCurve? = null
            var baselineEvents: List<SourceObservation>? = null
            var baselineEffects: List<LifecycleUpdate>? = null
            var baselineProvenance: String? = null
            for (speed in listOf(1, 10, null)) {
                val name = "oxywatt-synthetic-experiment-${UUID.randomUUID()}.db"
                try {
                    val lifecycle =
                        CollectorLifecycle(
                            f.manifest.epoch,
                            LifecyclePolicy("preview-silence-v1", 6_000_000_000),
                            { session },
                        )
                    val admission = BoundedAdmission()
                    val clock = VirtualTimeline()
                    val observed = mutableListOf<SourceObservation>()
                    InputRepository(context, name).use { inputs ->
                        fun flush() {
                            val batch = admission.takeBatch() ?: return
                            inputs.commit(inputs.prepare(batch, lifecycle, inputs.state().revision))
                            admission.committed(batch)
                        }
                        // Real Collector time advancement in deterministic timer-before-input
                        // order.
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
                            .start({ observation ->
                                observed.add(observation)
                                check(admission.offer(observation))
                                flush()
                            }) {
                                it?.let { throw it }
                            }
                            .use { while (source.pump()) {} }
                        assertEquals(f.expectedCount, observed.size)
                        assertEquals(f.expectedEnd, clock.now)
                        assertEquals(observed, inputs.observations().map { it.observation })
                        val effects = inputs.replay(inputs.state().revision).map { it.update }
                        if (baselineEvents == null) {
                            baselineEvents = observed
                            baselineEffects = effects
                        } else {
                            assertEquals(baselineEvents, observed)
                            assertEquals(baselineEffects, effects)
                        }
                        AnalysisRepository(context, name).use { analyses ->
                            val completion =
                                PowerCurveCalculation(inputs, analyses)
                                    .prepare(
                                        session,
                                        "SYNTHETIC:${f.linkedManifest().hash()}:${f.digest()}",
                                        "t05a-fixtures-v1",
                                    )
                            assertEquals(AnalysisStatus.AVAILABLE, completion.terminal.status)
                            val curve = completion.terminal.curve!!
                            assertFalse(curve.workLimited)
                            assertFalse(curve.snapshot.resourceLimited)
                            assertEquals(9, curve.snapshot.durations.size)
                            assertEquals(
                                when (kind) {
                                    "constant" -> 60L
                                    "expiry" -> 1L
                                    else -> 3L
                                },
                                curve.intervalCount,
                            )
                            curve.snapshot.durations.forEach { d ->
                                val expected =
                                    when {
                                        kind == "constant" && d.durationSeconds <= 60 -> 200.0
                                        kind in setOf("irregular", "expiry") &&
                                            d.durationSeconds == 5 -> 200.0
                                        else -> null
                                    }
                                if (expected == null) {
                                    assertNull(d.record)
                                    assertEquals(
                                        com.oxywatt.core.power.PowerUnavailableReason
                                            .INSUFFICIENT_CONTIGUOUS_COVERAGE,
                                        d.unavailableReason,
                                    )
                                } else {
                                    assertNull(d.unavailableReason)
                                    assertEquals(expected, d.record!!.meanWatts, 1e-8)
                                    assertEquals(
                                        if (kind == "irregular") 2_000_000_000 else 0,
                                        d.record!!.startNanos,
                                    )
                                    assertEquals(
                                        d.record!!.startNanos + d.durationSeconds * 1_000_000_000L,
                                        d.record!!.endNanos,
                                    )
                                }
                            }
                            if (baseline == null) baseline = curve
                            else assertEquals(baseline, curve)
                            analyses.complete(completion.generation, completion.terminal)
                            val provenance =
                                JSONObject(
                                    analyses
                                        .current(PowerCurveCalculation.slot(session))!!
                                        .provenance
                                )
                            provenance.remove("requestedAt")
                            val semanticProvenance =
                                PowerReplayHash().also { it.document(provenance) }.finish()
                            if (baselineProvenance == null) baselineProvenance = semanticProvenance
                            else assertEquals(baselineProvenance, semanticProvenance)
                            assertThrows(IllegalStateException::class.java) {
                                analyses.replaceMembership(
                                    "forbidden",
                                    setOf(completion.generation),
                                )
                            }
                        }
                    }
                    PreviewPipeline(context, name).use { preview ->
                        assertEquals(baseline, preview.storedCurve()!!.terminal!!.curve)
                        assertTrue(curveText(context, preview).contains(baseline!!.inputHash))
                    }
                    println(
                        "T05a kind=$kind speed=$speed descriptor=${f.digest()} input=${baseline!!.inputHash} events=${observed.size}"
                    )
                } finally {
                    context.deleteDatabase(name)
                }
            }
        }
    }

    @Test
    fun projectionUsesExistingTypedHashAndIgnoresExecutionMetadata() {
        fun hash(doc: JSONObject) = PowerReplayHash().also { it.document(doc) }.finish()
        val f = experimentFixture("constant")
        val semantic = JSONObject().put("descriptor", f.digest()).put("events", f.expectedCount)
        val reordered = JSONObject().put("events", f.expectedCount).put("descriptor", f.digest())
        assertEquals(hash(semantic), hash(reordered))
        assertNotEquals(
            hash(semantic),
            hash(
                JSONObject().put("events", f.expectedCount.toString()).put("descriptor", f.digest())
            ),
        )
        val reports =
            listOf(1, 10, null).map { speed ->
                JSONObject()
                    .put("semantic", semantic)
                    .put("speed", speed)
                    .put("request", UUID.randomUUID().toString())
                    .put("path", UUID.randomUUID().toString())
                    .put("wallTime", System.nanoTime())
            }
        assertEquals(1, reports.map { hash(it.getJSONObject("semantic")) }.distinct().size)
    }
}
