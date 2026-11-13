package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.hammerhead.karooext.models.ViewConfig
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryRendererTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun state(appearance: FieldAppearance) =
        HistorySnapshot(
            HistoryStatus.READY,
            HistoryConfig(300, appearance),
            listOf(
                HistoryEntry(
                    UUID(0, 1),
                    Instant.parse("2026-10-01T00:00:00Z"),
                    HistoryReason.VALUE,
                    200.0,
                ),
                HistoryEntry(
                    UUID(0, 2),
                    Instant.parse("2026-10-03T00:00:00Z"),
                    HistoryReason.NO_WINDOW,
                ),
                HistoryEntry(
                    UUID(0, 3),
                    Instant.parse("2026-10-10T00:00:00Z"),
                    HistoryReason.VALUE,
                    240.0,
                ),
                HistoryEntry(
                    UUID(0, 4),
                    Instant.parse("2026-10-10T00:00:00Z"),
                    HistoryReason.VALUE,
                    240.0,
                ),
                HistoryEntry(
                    UUID(0, 5),
                    Instant.parse("2026-10-21T00:00:00Z"),
                    HistoryReason.VALUE,
                    0.0,
                ),
            ),
            true,
            Instant.EPOCH,
        )

    @Test
    fun `native layouts bound bitmaps and expose calendar provenance raw values and missing reasons`() {
        for (appearance in FieldAppearance.entries) {
            val state = state(appearance)
            val large = HistoryRenderer.render(context, state, ViewConfig(1 to 1, 480 to 340, 60))
            assertEquals(FieldLayout.LARGE, large.layout)
            assertTrue(large.description.contains("200 W"))
            assertTrue(large.description.contains("No covered window"))
            assertTrue(large.description.contains("0 W"))
            assertTrue(large.description.contains("More history exists"))
            assertTrue(large.description.contains("UTC"))
            val small = HistoryRenderer.render(context, state, ViewConfig(1 to 1, 300 to 150, 40))
            assertEquals(FieldLayout.COMPACT, small.layout)
            assertTrue(small.description.contains("5 min"))
            val tiny = HistoryRenderer.render(context, state, ViewConfig(1 to 1, 60 to 30, 20))
            assertEquals(FieldLayout.ENLARGE, tiny.layout)
            val huge = HistoryRenderer.render(context, state, ViewConfig(1 to 1, 4000 to 3000, 300))
            assertTrue(huge.bitmap.allocationByteCount <= 512 * 512 * 4)
            val view =
                SavedFieldRenderer.remoteViews(context, large)
                    .apply(context, android.widget.FrameLayout(context))
            assertEquals(large.description, view.contentDescription)
            java.io
                .File("/private/tmp/oxywatt-history-${appearance.name.lowercase()}.png")
                .outputStream()
                .use { large.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun `errors and empty history are accessible without exposing cached numeric success`() {
        for (status in HistoryStatus.entries) {
            val state = state(FieldAppearance.LIGHT).copy(status = status)
            val rendered =
                HistoryRenderer.render(context, state, ViewConfig(1 to 1, 480 to 340, 60))
            assertTrue(rendered.description.contains(historyMessage(context, state)))
            if (status != HistoryStatus.READY) assertFalse(rendered.description.contains("240 W"))
        }
        val empty = HistorySnapshot(HistoryStatus.READY)
        assertTrue(
            HistoryRenderer.render(context, empty, ViewConfig(1 to 1, 480 to 340, 60))
                .description
                .contains("No covered saved results")
        )
        for (reason in HistoryReason.entries) assertTrue(
            historyReason(context, reason).isNotBlank()
        )
    }
}
