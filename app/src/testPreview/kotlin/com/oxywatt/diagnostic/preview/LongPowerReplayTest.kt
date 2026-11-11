package com.oxywatt.diagnostic.preview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import com.oxywatt.core.collection.simulation.VirtualTimeline
import com.oxywatt.core.collection.simulation.playbackDeadline
import com.oxywatt.core.power.*
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
@Config(sdk = [32])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LongPowerReplayTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val session = UUID(0, 701)
    private val epoch = UUID(0, 702)
    private val ttl = 2_000_000_000L

    private data class Event(val time: Long, val payload: InputPayload)

    private fun state(s: RecordingState) = InputPayload.Recording(s)

    private fun power(w: Double?) =
        InputPayload.MetricReading(
            Metric.POWER_W,
            w,
            if (w == null) MissingReason.DROPOUT else null,
        )

    private fun events(mode: Int): List<Event> {
        val events =
            arrayListOf(
                Event(0, state(RecordingState.IDLE)),
                Event(0, state(RecordingState.RECORDING)),
            )
        if (mode == 2) {
            repeat(16000) {
                events.add(Event(it * 1_000_000L, power(if (it % 2 == 0) 100.0 else 200.0)))
            }
            // Silence is not a confirmed hour. Next receipt must apply the stored expiry.
            for (s in 3600 until 14400) events.add(
                Event(s * 1_000_000_000L, power(if (s % 2 == 0) 100.0 else 200.0))
            )
        } else {
            var t = 0L
            repeat(14400) { i ->
                when {
                    mode == 3 && i == 1000 -> events.add(Event(t, state(RecordingState.PAUSED)))
                    mode == 3 && i in 1001..1009 -> Unit
                    mode == 3 && i == 1010 -> {
                        events.add(Event(t, state(RecordingState.RECORDING)))
                        events.add(Event(t, power(100.0)))
                    }
                    mode == 3 && i == 2000 -> events.add(Event(t, power(null)))
                    mode == 3 && i in 3000..3009 -> Unit
                    else -> events.add(Event(t, power(if (i % 2 == 0) 100.0 else 200.0)))
                }
                t +=
                    if (mode == 1) longArrayOf(250000001, 749999999, 1500000000, 1500000000)[i % 4]
                    else 1_000_000_000L
            }
        }
        events.add(
            Event(14400_000_000_000, if (mode >= 4) power(null) else state(RecordingState.IDLE))
        )
        return events
    }

    /** Independent event-table hold oracle, with explicit pause/missing/timeout partitions. */
    private fun spans(events: List<Event>): List<List<PowerInterval>> {
        val spans = mutableListOf<MutableList<PowerInterval>>()
        var active = false
        var segment = 0L
        var held: Event? = null
        var current = mutableListOf<PowerInterval>().also(spans::add)
        for (event in events) {
            val old = held
            if (old != null) {
                val expiry = old.time + ttl
                val end = minOf(event.time, expiry)
                if (end > old.time)
                    current.add(
                        PowerInterval(
                            session,
                            epoch,
                            segment,
                            old.time,
                            end,
                            (old.payload as InputPayload.MetricReading).value!!,
                        )
                    )
                if (event.time >= expiry) {
                    held = null
                    current = mutableListOf<PowerInterval>().also(spans::add)
                }
            }
            when (val payload = event.payload) {
                is InputPayload.Recording -> {
                    held = null
                    current = mutableListOf<PowerInterval>().also(spans::add)
                    if (payload.state == RecordingState.RECORDING) {
                        if (active) error("duplicate recording")
                        segment++
                    }
                    active = payload.state == RecordingState.RECORDING
                }
                is InputPayload.MetricReading -> {
                    if (active && payload.value != null) held = event
                    else {
                        held = null
                        current = mutableListOf<PowerInterval>().also(spans::add)
                    }
                }
                else -> error("unsupported fixture event")
            }
        }
        // Collector's first segment is zero.
        return spans
            .filter { it.isNotEmpty() }
            .map { span -> span.map { it.copy(segmentIndex = it.segmentIndex - 1) } }
    }

    @Test
    fun longFixturesPersistReopenAndRenderIdenticallyAtAllDeliverySpeeds() {
        for (mode in 0..5) {
            val events = events(mode)
            val spans = spans(events)
            val expected = ExactPowerOracle.records(spans)
            var baseline: StoredPowerCurve? = null
            for (speed in listOf(1, 10, null)) {
                val name = "oxywatt-synthetic-long-${UUID.randomUUID()}.db"
                try {
                    val lifecycle =
                        CollectorLifecycle(
                            epoch,
                            LifecyclePolicy("preview-silence-v1", ttl),
                            { session },
                        )
                    val admission = BoundedAdmission()
                    val delivery = VirtualTimeline()
                    InputRepository(context, name).use { inputs ->
                        fun flush() {
                            val batch = admission.takeBatch() ?: return
                            inputs.commit(inputs.prepare(batch, lifecycle, inputs.state().revision))
                            admission.committed(batch)
                        }
                        events.forEachIndexed { i, event ->
                            delivery.advanceTo(playbackDeadline(event.time, speed))
                            val observation =
                                SourceObservation(
                                    ReceiptStamp(Instant.EPOCH, epoch, event.time, i.toLong()),
                                    SourceRef("simulated", "1"),
                                    "canonical-v1",
                                    event.payload,
                                )
                            assertTrue(admission.offer(observation))
                            if (i % 256 == 255) flush()
                        }
                        flush()
                        if (mode == 4) {
                            val recovered =
                                CollectorLifecycle(
                                    UUID(0, 703),
                                    LifecyclePolicy("preview-silence-v1", ttl),
                                    recovered = lifecycle.checkpoint(),
                                )
                            inputs.commit(
                                inputs.prepareEffect(
                                    recovered.advanceTime(0),
                                    AdmissionPolicy(),
                                    inputs.state().revision,
                                )
                            )
                        }
                        if (mode == 5) {
                            // Zero telemetry capacity creates actual persisted admission-loss
                            // metadata.
                            val loss =
                                BoundedAdmission(AdmissionPolicy(events = 1, reservedEvents = 1))
                            assertFalse(
                                loss.offer(
                                    SourceObservation(
                                        ReceiptStamp(
                                            Instant.EPOCH,
                                            epoch,
                                            14401_000_000_000,
                                            events.size.toLong(),
                                        ),
                                        SourceRef("simulated", "1"),
                                        "canonical-v1",
                                        power(999.0),
                                    )
                                )
                            )
                            val batch = loss.takeBatch()!!
                            inputs.commit(inputs.prepare(batch, lifecycle, inputs.state().revision))
                            loss.committed(batch)
                        }
                        AnalysisRepository(context, name).use { results ->
                            val completed =
                                PowerCurveCalculation(inputs, results)
                                    .prepare(
                                        session,
                                        "SYNTHETIC:long-v2:mode=$mode:seed=0",
                                        "t04b2-test",
                                    )
                            assertEquals(AnalysisStatus.AVAILABLE, completed.terminal.status)
                            val curve = completed.terminal.curve!!
                            assertFalse(curve.workLimited)
                            assertFalse(curve.snapshot.resourceLimited)
                            assertEquals(
                                (events.size - 1 + if (mode >= 4) 1 else 0).toLong(),
                                curve.effectCount,
                            )
                            assertEquals(
                                if (mode >= 4) SessionPhase.INTERRUPTED
                                else SessionPhase.ENDED_LOCALLY,
                                curve.session.phase,
                            )
                            assertEquals(spans.sumOf { it.size }.toLong(), curve.intervalCount)
                            assertTrue(curve.reservedWork <= 100_000_000)
                            expected.forEachIndexed { i, ref ->
                                val actual = curve.snapshot.durations[i].record
                                if (ref == null) assertNull(actual)
                                else {
                                    assertNotNull(actual)
                                    assertEquals(
                                        ref.meanWatts,
                                        actual!!.meanWatts,
                                        maxOf(1e-8, 1e-12 * ref.meanWatts),
                                    )
                                    assertEquals(ref.copy(meanWatts = actual.meanWatts), actual)
                                }
                            }
                            val old = baseline
                            if (old == null) baseline = curve else assertEquals(old, curve)
                            results.complete(completed.generation, completed.terminal)
                            assertThrows(IllegalStateException::class.java) {
                                results.replaceMembership(
                                    "synthetic-forbidden",
                                    setOf(completed.generation),
                                )
                            }
                        }
                    }
                    PreviewPipeline(context, name).use { preview ->
                        assertEquals(baseline, preview.storedCurve()!!.terminal!!.curve)
                        val text = curveText(context, preview)
                        assertTrue(text.contains("Input scan complete"))
                        assertTrue(text.contains(baseline!!.inputHash))
                        assertTrue(text.contains("3600 s:"))
                        assertTrue(text.contains("W"))
                    }
                    println(
                        "T04b2 persisted mode=$mode speed=$speed events=${events.size} effects=${baseline!!.effectCount} intervals=${baseline!!.intervalCount} work=${baseline!!.reservedWork}"
                    )
                } finally {
                    context.deleteDatabase(name)
                }
            }
        }
    }
}
