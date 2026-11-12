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

    private fun views(v: View): List<View> =
        listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) }
            else emptyList()

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
                    DiagnosticRuntime.storage().session?.phase == SessionPhase.ENDED_LOCALLY &&
                        DiagnosticRuntime.curve().canCalculate
                }
            }
            assertNull(DiagnosticRuntime.curve().saved)
            screen.setup()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            val button = views(screen.get().window.decorView).filterIsInstance<Button>().single()
            assertTrue(button.isEnabled)
            button.performClick()
            karoo.awaitValue {
                true.takeIf { DiagnosticRuntime.curve().status == LiveCurveStatus.SAVED }
            }
            val saved = DiagnosticRuntime.curve().saved!!
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
            screen.recreate()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertEquals(saved.generation, DiagnosticRuntime.curve().saved!!.generation)
            assertEquals(5, karoo.system.consumerCount)
        } finally {
            screen.pause().stop().destroy()
            service.destroy()
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
        }
    }
}
