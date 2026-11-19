package com.oxywatt.diagnostic

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.oxywatt.diagnostic.persistence.*
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import io.hammerhead.karooext.models.ViewConfig
import java.io.File
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
class FitImportRuntimeTest {
    @get:Rule val karoo = FakeKarooRule()

    private fun views(v: View): List<View> =
        listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) }
            else emptyList()

    @Test
    fun `one staged archive is consumed by the existing worker before live startup and shown as FIT history`() {
        val parent = File(karoo.app.filesDir, "fit-history")
        val pending = File(parent, "pending")
        fitArchiveFixture(karoo.app).copyRecursively(pending)
        HistorySettings(karoo.app).write(HistoryConfig(5))
        val service = Robolectric.buildService(DiagnosticExtension::class.java).create()
        val screen = Robolectric.buildActivity(MainActivity::class.java)
        try {
            karoo.awaitValue {
                true.takeIf {
                    FitImportMaintenance.statusResource(karoo.app) == R.string.fit_import_saved &&
                        karoo.system.consumerCount == 5
                }
            }
            assertFalse(pending.exists())
            assertTrue(parent.listFiles()!!.any { it.name.startsWith("completed-") })
            assertEquals(
                HistoryConfig(5, source = HistorySource.FIT),
                HistorySettings(karoo.app).read(),
            )
            ShadowSystemClock.advanceBy(Duration.ofSeconds(6))
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.storage().committed > 0 } }
            screen.setup()
            karoo.awaitValue {
                true.takeIf { DiagnosticRuntime.history().status == HistoryStatus.READY }
            }
            val snapshot = DiagnosticRuntime.history()
            assertEquals(HistorySource.FIT, snapshot.config.source)
            assertEquals(100.0, snapshot.entries.single().watts!!, 0.0)
            for (appearance in FieldAppearance.entries) {
                val rendered =
                    HistoryRenderer.render(
                        karoo.app,
                        snapshot.copy(config = snapshot.config.copy(appearance = appearance)),
                        ViewConfig(1 to 1, 480 to 340, 20),
                    )
                assertTrue(rendered.description.contains("Imported FIT"))
                assertTrue(rendered.description.contains("100 W"))
            }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertTrue(
                views(screen.get().window.decorView).filterIsInstance<TextView>().any {
                    it.text.contains("FIT import: saved")
                }
            )
            assertNull(DiagnosticRuntime.curveTiming())
            screen.recreate()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            assertEquals(5, karoo.system.consumerCount)
            assertEquals(
                1L,
                AnalysisRepository(karoo.app, "oxywatt-live-diagnostic.db").use { results ->
                    InputRepository(karoo.app, "oxywatt-live-diagnostic.db").use { inputs ->
                        results
                            .newest(FitSource.slot(inputs.recentSessions().sessions.single().id))!!
                            .generation
                    }
                },
            )
        } finally {
            screen.pause().stop().destroy()
            service.destroy()
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
            parent.deleteRecursively()
            karoo.app.deleteDatabase("oxywatt-live-diagnostic.db")
        }
    }

    @Test
    fun `failed staged archive retains input and still initializes live consumers without retrying`() {
        val parent = File(karoo.app.filesDir, "fit-history")
        val pending = File(parent, "pending")
        fitArchiveFixture(karoo.app).copyRecursively(pending)
        File(pending, "observations.tsv").appendText("corrupt\n")
        val service = Robolectric.buildService(DiagnosticExtension::class.java).create()
        try {
            karoo.awaitValue {
                true.takeIf {
                    FitImportMaintenance.statusResource(karoo.app) == R.string.fit_import_failed &&
                        karoo.system.consumerCount == 5
                }
            }
            assertTrue(File(parent, "working/observations.tsv").exists())
            assertEquals(HistorySource.LIVE, HistorySettings(karoo.app).read().source)
            InputRepository(karoo.app, "oxywatt-live-diagnostic.db").use {
                assertTrue(it.recentSessions().sessions.isEmpty())
            }
            FitImportMaintenance.run(karoo.app)
            assertTrue(File(parent, "working").isDirectory)
            assertFalse(File(parent, "pending").exists())
        } finally {
            service.destroy()
            karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
            parent.deleteRecursively()
            karoo.app.deleteDatabase("oxywatt-live-diagnostic.db")
        }
    }

    @Test
    fun `legacy history preferences default to live and source selection survives round trip`() {
        karoo.app
            .getSharedPreferences("power-history", 0)
            .edit()
            .putString("config", """{"version":1,"duration":60,"appearance":"DARK"}""")
            .commit()
        val settings = HistorySettings(karoo.app)
        assertEquals(HistoryConfig(60, FieldAppearance.DARK), settings.read())
        settings.write(settings.read().copy(source = HistorySource.FIT))
        assertEquals(
            HistoryConfig(60, FieldAppearance.DARK, HistorySource.FIT),
            HistorySettings(karoo.app).read(),
        )
    }
}
