package com.oxywatt.diagnostic

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.oxywatt.core.collection.SessionPhase
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import java.time.Duration
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class LiveCurveRuntimeTest {
    @get:Rule val karoo = FakeKarooRule()

    @Test
    fun `bounded pacing matches independent wait counts and never skips ordinary work waits`() {
        for ((turns, baseline, bounded) in listOf(Triple(44, 43, 2), Triple(2092, 2091, 130))) {
            for ((quantum, expected) in listOf(1 to baseline, 16 to bounded)) {
                val pacing = AnalysisPacing(quantum)
                var waits = 0
                repeat(turns) { turn ->
                    val mustWait = pacing.shouldWait(turn < turns - 1)
                    if (mustWait && turn < turns - 1) waits++
                }
                assertEquals(expected, waits)
                repeat(20) { assertTrue(pacing.shouldWait(false)) }
            }
        }
    }

    @Test
    fun `pacing resets partial allowance at every non progressing turn`() {
        val pacing = AnalysisPacing()
        repeat(10) { assertFalse(pacing.shouldWait(true)) }
        assertTrue(pacing.shouldWait(false))
        repeat(15) { assertFalse(pacing.shouldWait(true)) }
        assertTrue(pacing.shouldWait(true))
        assertFalse(pacing.shouldWait(true))
        assertTrue(pacing.shouldWait(false))
    }

    private fun views(v: View): List<View> =
        listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) }
            else emptyList()

    @Test
    fun `live runtime opens data beyond the historical cap and publishes exact storage v2 metadata`() {
        val name = "oxywatt-live-diagnostic.db"
        val path = karoo.app.getDatabasePath(name)
        com.oxywatt.diagnostic.persistence.InputRepository(karoo.app, name).use { it.state() }
        android.database.sqlite.SQLiteDatabase.openDatabase(path.path, null, 0).use { db ->
            db.execSQL("CREATE TABLE capacity_fixture (data BLOB NOT NULL)")
            db.execSQL(
                "INSERT INTO capacity_fixture VALUES (zeroblob(?))",
                arrayOf(33L * 1024 * 1024),
            )
        }
        assertTrue(path.length() > 33554432L)
        val service = Robolectric.buildService(DiagnosticExtension::class.java).create()
        try {
            karoo.awaitValue {
                true.takeIf {
                    karoo.system.consumerCount == 5 && DiagnosticRuntime.storage().received > 0
                }
            }
            ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
            karoo.awaitValue {
                true.takeIf {
                    DiagnosticRuntime.storage().committed > 0 &&
                        !DiagnosticRuntime.sessions().unavailable
                }
            }
            com.oxywatt.diagnostic.persistence.InputRepository(karoo.app, name).use { inputs ->
                val rows = inputs.replay(inputs.state().revision)
                assertTrue(rows.isNotEmpty())
                rows.forEach { row ->
                    val metadata = org.json.JSONObject(row.metadata.acquisition!!)
                    assertEquals("karoo-diagnostic-storage-v2", metadata.getString("storage"))
                    assertEquals(67108864L, metadata.getLong("maximumBytes"))
                }
            }
            assertFalse(DiagnosticRuntime.curve().canCalculate)
            assertNull(DiagnosticRuntime.curveTiming())
        } finally {
            service.destroy()
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
            karoo.app.deleteDatabase(name)
        }
    }

    @Test
    fun `screen button computes real adapter inputs on the existing runtime and recreation reloads without a new generation`() {
        val service = Robolectric.buildService(DiagnosticExtension::class.java).create()
        val screen = Robolectric.buildActivity(MainActivity::class.java)
        try {
            karoo.awaitValue {
                true.takeIf {
                    karoo.system.consumerCount == 5 &&
                        DiagnosticRuntime.capture(karoo.app).snapshot().connected == true
                }
            }
            fun send(action: () -> Unit) {
                val before = DiagnosticRuntime.storage().received
                action()
                karoo.awaitValue { true.takeIf { DiagnosticRuntime.storage().received > before } }
            }
            send { karoo.system.setRideState(RideState.Recording) }
            repeat(6) {
                send {
                    karoo.system.setDataPoint(
                        DataPoint(DataType.Type.POWER, mapOf(DataType.Field.POWER to 200.0))
                    )
                }
                ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
            }
            send { karoo.system.setRideState(RideState.Idle) }
            ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
            karoo.awaitValue {
                true.takeIf {
                    DiagnosticRuntime.storage().session?.phase == SessionPhase.ENDED_LOCALLY
                }
            }
            assertNull(DiagnosticRuntime.curve().saved)
            screen.setup()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertFalse(DiagnosticRuntime.curve().canCalculate)
            assertNull(DiagnosticRuntime.curveTiming())
            assertTrue(
                views(screen.get().window.decorView).filterIsInstance<TextView>().any {
                    it.text.contains("No runtime timing available")
                }
            )
            views(screen.get().window.decorView)
                .filterIsInstance<Button>()
                .single { it.text == screen.get().getString(R.string.local_sessions_refresh) }
                .performClick()
            karoo.awaitValue {
                true.takeIf {
                    DiagnosticRuntime.sessions().cards.isNotEmpty() &&
                        !DiagnosticRuntime.sessions().loading
                }
            }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            val target = DiagnosticRuntime.storage().session!!.id
            views(screen.get().window.decorView)
                .filterIsInstance<Button>()
                .single { it.tag == target }
                .performClick()
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.curve().canCalculate } }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            val button =
                views(screen.get().window.decorView).filterIsInstance<Button>().single {
                    it.text == screen.get().getString(R.string.live_curve_calculate)
                }
            assertTrue(button.isEnabled)
            button.performClick()
            karoo.awaitValue {
                true.takeIf { DiagnosticRuntime.curve().status == LiveCurveStatus.SAVED }
            }
            val saved = DiagnosticRuntime.curve().saved!!
            val timing = DiagnosticRuntime.curveTiming()!!
            assertEquals(com.oxywatt.diagnostic.persistence.CurveOutcome.SAVED, timing.outcome)
            assertFalse(timing.incomplete)
            assertEquals("diagnostic-analysis-pacing-v1", timing.pacingPolicy)
            assertEquals(16, timing.maximumTurnsBetweenWaits)
            assertTrue(timing.work.getValue(com.oxywatt.diagnostic.persistence.CurveWork.WAIT) > 0)
            assertEquals(timing.elapsedNanos, timing.work.values.sum() + timing.residualNanos)
            assertEquals(
                200.0,
                saved.terminal!!.curve!!.snapshot.durations.first().record!!.meanWatts,
                1e-8,
            )
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertTrue(
                views(screen.get().window.decorView).filterIsInstance<TextView>().any {
                    it.text.contains("5 s: 200.0 W")
                }
            )
            val expectedTiming = curveTimingText(screen.get(), timing)
            assertTrue(
                views(screen.get().window.decorView).filterIsInstance<TextView>().any {
                    it.text.toString() == expectedTiming
                }
            )
            screen.recreate()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertEquals(saved.generation, DiagnosticRuntime.curve().saved!!.generation)
            assertEquals(target, DiagnosticRuntime.curve().session!!.id)
            assertEquals(5, karoo.system.consumerCount)
            assertEquals(timing, DiagnosticRuntime.curveTiming())
            assertTrue(
                views(screen.get().window.decorView).filterIsInstance<TextView>().any {
                    it.text.toString() == expectedTiming
                }
            )
        } finally {
            screen.pause().stop().destroy()
            service.destroy()
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
        }
    }
}
