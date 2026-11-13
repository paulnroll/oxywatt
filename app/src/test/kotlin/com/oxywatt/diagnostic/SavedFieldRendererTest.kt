package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.hammerhead.karooext.models.ViewConfig
import java.time.Instant
import java.util.Locale
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
class SavedFieldRendererTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun snapshot(appearance: FieldAppearance = FieldAppearance.LIGHT) =
        SavedFieldSnapshot(
            SavedFieldStatus.SAVED,
            SavedFieldBinding(UUID(0, 1), 300, appearance),
            Instant.parse("2026-11-12T23:59:59Z"),
            1,
            listOf(
                    5 to 890.0,
                    30 to 610.0,
                    60 to 490.0,
                    180 to 370.0,
                    300 to 318.5,
                    600 to 290.0,
                    1200 to 265.0,
                    1800 to 251.0,
                    3600 to 232.0,
                )
                .map { FieldPower(it.first, it.second, false) },
        )

    private fun config(width: Int, height: Int, text: Int = 60) =
        ViewConfig(60 to 30, width to height, text)

    @Test
    fun `large compact and tiny layouts preserve historical value semantics and bounded bitmaps`() {
        for (appearance in FieldAppearance.entries) {
            val state = snapshot(appearance)
            val large = SavedFieldRenderer.render(context, state, config(480, 340))
            assertEquals(FieldLayout.LARGE, large.layout)
            assertTrue(large.description.contains("319 W"))
            assertTrue(large.description.contains("12 Nov 2026"))
            assertEquals(
                FieldLayout.COMPACT,
                SavedFieldRenderer.render(context, state, config(240, 160)).layout,
            )
            assertEquals(
                FieldLayout.ENLARGE,
                SavedFieldRenderer.render(context, state, config(50, 30)).layout,
            )
            val huge = SavedFieldRenderer.render(context, state, config(4000, 3000, 400))
            assertTrue(huge.bitmap.width <= 512 && huge.bitmap.height <= 512)
            assertTrue(huge.bitmap.allocationByteCount <= 512 * 512 * 4)
            val view =
                SavedFieldRenderer.remoteViews(context, large)
                    .apply(context, android.widget.FrameLayout(context))
            assertEquals(large.description, view.contentDescription)
            // Actual synthetic native-graphics render for local visual inspection, outside Git.
            java.io
                .File("/private/tmp/oxywatt-field-${appearance.name.lowercase()}.png")
                .outputStream()
                .use {
                    large.bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
        }
    }

    @Test
    fun `missing zero limited and failure readouts never manufacture watts and remain accessible`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"))
            val base = snapshot()
            assertEquals("319", fieldValue(context, base))
            assertEquals("12 Nov 2026", fieldDate(context, base))
            assertEquals(
                "0",
                fieldValue(context, base.copy(points = listOf(FieldPower(300, 0.0, false)))),
            )
            val missing = base.copy(points = listOf(FieldPower(300, null, false)))
            assertEquals("—", fieldValue(context, missing))
            assertEquals("No covered window", fieldMessage(context, missing))
            assertEquals("Limited scan", fieldMessage(context, missing.copy(limited = true)))
            for (status in SavedFieldStatus.entries) {
                val state = base.copy(status = status)
                val render = SavedFieldRenderer.render(context, state, config(480, 340, 90))
                assertTrue(render.description.contains(fieldMessage(context, state)))
                if (status != SavedFieldStatus.SAVED) assertEquals("—", fieldValue(context, state))
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `all nine stored values format independently and large values remain bounded at large font settings`() {
        val state = snapshot()
        val expected = listOf("890", "610", "490", "370", "319", "290", "265", "251", "232")
        state.points.forEachIndexed { index, point ->
            assertEquals(
                expected[index],
                fieldValue(
                    context,
                    state.copy(binding = state.binding!!.copy(duration = point.seconds)),
                ),
            )
        }
        val hugeValue =
            state.copy(
                points = listOf(FieldPower(300, 123456789.0, false)),
                receiptDate = Instant.parse("+10000-11-12T00:00:00Z"),
            )
        for (size in listOf(20, 90, 300)) {
            val rendered = SavedFieldRenderer.render(context, hugeValue, config(240, 160, size))
            assertTrue(rendered.bitmap.width <= 512 && rendered.bitmap.height <= 512)
            assertTrue(rendered.description.contains("123456789 W"))
            assertTrue(rendered.description.contains("+10000"))
        }
    }
}
