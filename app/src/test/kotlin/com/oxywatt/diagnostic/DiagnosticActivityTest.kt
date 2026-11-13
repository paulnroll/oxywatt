package com.oxywatt.diagnostic

import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class DiagnosticActivityTest {
    private fun text(view: View): String =
        when (view) {
            is TextView -> view.text.toString()
            is ViewGroup ->
                (0 until view.childCount).joinToString("\n") { text(view.getChildAt(it)) }
            else -> ""
        }

    @Test
    fun `recreating the screen retains the source model and renders actual received values`() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        val capture = DiagnosticRuntime.capture(app)
        capture.sourceStarted()
        val count = capture.snapshot().metrics[Metric.POWER_W]?.callbacks ?: 0
        capture.accept(
            SourceObservation(
                ReceiptStamp(Instant.EPOCH, UUID(0, 2), SystemClock.elapsedRealtimeNanos(), 1),
                SourceRef("karoo", "1.1.9"),
                "karoo-normalization-v1",
                InputPayload.MetricReading(Metric.POWER_W, 321.0),
            )
        )
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertTrue(text(activity.get().window.decorView).contains("Power: 321 W"))
        assertTrue(text(activity.get().window.decorView).contains("Local sessions unavailable"))
        assertFalse(DiagnosticRuntime.curve().canCalculate)
        assertTrue(
            text(activity.get().window.decorView)
                .contains("Committed canonical events (current epoch): 0")
        )
        assertTrue(
            text(activity.get().window.decorView).contains("Pending events / payload bytes:")
        )
        activity.recreate()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        assertSame(capture, DiagnosticRuntime.capture(app))
        assertEquals(count + 1, capture.snapshot().metrics[Metric.POWER_W]!!.callbacks)
        assertTrue(text(activity.get().window.decorView).contains("Power: 321 W"))
        activity.pause().stop().destroy()
        assertTrue(capture.snapshot().sourceActive)
        capture.sourceStopped(false)
    }
}
