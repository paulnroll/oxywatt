package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.diagnostic.persistence.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class CurveTimingTextTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `no runtime summary is unavailable rather than a zero measurement`() {
        assertEquals(
            "Last runtime analysis attempt\nMay differ from the selected session; not saved\nNo runtime timing available",
            curveTimingText(context, null),
        )
    }

    @Test
    fun `literal timing values retain units and distinguish page work from engine and display time`() {
        val snapshot =
            CurveTimingSnapshot(
                work = CurveWork.entries.associateWith { 1_250_000L },
                counts = CurveCount.entries.associateWith { 7L },
                marks =
                    mapOf(CurveMark.FIRST_WORKER to 500_000L, CurveMark.PUBLISHED to 20_000_000L),
                elapsedNanos = 20_000_000L,
                residualNanos = 6_250_000L,
                outcome = CurveOutcome.SAVED,
                incomplete = false,
                pacingPolicy = "diagnostic-analysis-pacing-v1",
                maximumTurnsBetweenWaits = 16,
            )
        val text = curveTimingText(context, snapshot)
        assertEquals(
            """
            Last runtime analysis attempt
            May differ from the selected session; not saved
            Operation elapsed time; not CPU or display latency
            Outcome: saved
            Timing accounting complete through last boundary
            Pacing: diagnostic-analysis-pacing-v1; maximum turns between waits: 16
            Elapsed through last accounted boundary: 20.000 ms
            Residual (unattributed): 6.250 ms
            Control/validation: 1.250 ms
            Hash page SQL/decode/validation: 1.250 ms
            Hash/provenance (excluding pages): 1.250 ms
            Generation request transaction: 1.250 ms
            Replay page SQL/decode/validation: 1.250 ms
            Replay/preprocessing/engine (excluding pages): 1.250 ms
            Completion transaction: 1.250 ms
            Readback/runtime publication (excluding display): 1.250 ms
            Canonical work: 1.250 ms
            Measured wait: 1.250 ms
            Measured retry backoff: 1.250 ms
            Job steps started: 7
            Job steps completed: 7
            Hash effects scanned: 7
            Hash effects selected: 7
            Hash page fetches: 7
            Replay effects scanned: 7
            Replay effects selected: 7
            Replay page fetches: 7
            Wait calls: 7
            Storage retry exceptions: 7
            First worker offset: 0.500 ms
            Hashed offset: unavailable
            Requested offset: unavailable
            Replayed offset: unavailable
            Published offset (runtime): 20.000 ms
            """
                .trimIndent(),
            text,
        )
        assertEquals(34, text.lines().size)
    }

    @Test
    fun `every outcome remains distinct and incomplete timing cannot appear complete under another locale`() {
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            for ((outcome, label) in
                listOf(
                    CurveOutcome.RUNNING to "in progress; last accounted boundary",
                    CurveOutcome.SAVED to "saved",
                    CurveOutcome.FAILED to "failed",
                    CurveOutcome.CANCELLED to "cancelled",
                    CurveOutcome.DISCARDED to "discarded",
                    CurveOutcome.STOPPED to "stopped",
                )) {
                val snapshot =
                    CurveTimingSnapshot(
                        CurveWork.entries.associateWith { 0L },
                        CurveCount.entries.associateWith { 0L },
                        emptyMap(),
                        1_250_000L,
                        1_250_000L,
                        outcome,
                        true,
                        "diagnostic-analysis-pacing-v1",
                        16,
                    )
                val text = curveTimingText(context, snapshot)
                assertTrue(text.contains("Outcome: $label\n"))
                assertTrue(text.contains("Incomplete execution timing"))
                assertFalse(text.contains("Timing accounting complete"))
                assertTrue(text.contains("Elapsed through last accounted boundary: 1.250 ms"))
                assertEquals(5, text.lines().count { it.endsWith(": unavailable") })
                assertEquals(34, text.lines().size)
            }
        } finally {
            java.util.Locale.setDefault(original)
        }
    }
}
