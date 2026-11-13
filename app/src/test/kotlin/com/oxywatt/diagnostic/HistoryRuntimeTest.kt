package com.oxywatt.diagnostic

import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import fi.nikosavola.karooext.testing.FakeKarooHost
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import io.hammerhead.karooext.aidl.IKarooExtension
import io.hammerhead.karooext.models.ViewConfig
import java.time.Duration
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class HistoryRuntimeTest {
    @get:Rule val karoo = FakeKarooRule()

    private fun views(v: View): List<View> =
        listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) }
            else emptyList()

    @Test
    fun `app and registered SDK history share projection without extra acquisition or calculation`() {
        HistorySettings(karoo.app).write(HistoryConfig(5, FieldAppearance.DARK))
        FieldFixture(karoo.app, "oxywatt-live-diagnostic.db").use { f ->
            val result = f.calculate()
            f.calculate(f.ids.last())
            val oldBinding = SavedFieldBinding(f.ids.first(), 5)
            SavedFieldSettings(karoo.app).write(oldBinding)
            val service = Robolectric.buildService(DiagnosticExtension::class.java).create()
            val screen = Robolectric.buildActivity(MainActivity::class.java)
            val host =
                FakeKarooHost(
                    IKarooExtension.Stub.asInterface(service.get().onBind(Intent())),
                    RobolectricPump.invoke(),
                )
            try {
                karoo.awaitValue { true.takeIf { karoo.system.consumerCount == 5 } }
                screen.setup()
                val field = host.startView(HistoryDataType.TYPE, ViewConfig(1 to 1, 480 to 340, 60))
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.history().status == HistoryStatus.READY }
                }
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
                karoo.awaitValue {
                    true.takeIf {
                        field.frames.any {
                            it.apply(karoo.app, android.widget.FrameLayout(karoo.app))
                                .contentDescription
                                .contains("100 W")
                        }
                    }
                }
                assertEquals(
                    listOf(100.0, 150.0),
                    DiagnosticRuntime.history().entries.map { it.watts },
                )
                val all = views(screen.get().window.decorView)
                val duration =
                    all.filterIsInstance<Spinner>().single {
                        it.contentDescription == karoo.app.getString(R.string.history_duration)
                    }
                assertEquals(0, duration.selectedItemPosition)
                val refresh =
                    all.filterIsInstance<Button>().single {
                        it.text == karoo.app.getString(R.string.history_refresh)
                    }
                assertTrue(refresh.isEnabled)
                refresh.performClick()
                screen.recreate()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
                assertEquals(
                    HistoryConfig(5, FieldAppearance.DARK),
                    HistorySettings(karoo.app).read(),
                )
                assertEquals(oldBinding, SavedFieldSettings(karoo.app).read())
                assertEquals(5, karoo.system.consumerCount)
                assertEquals(result.generation, f.results.newest(result.slot)!!.generation)
                assertNull(DiagnosticRuntime.curveTiming())
                val xml = karoo.app.resources.getXml(R.xml.extension_info)
                val types = mutableSetOf<String>()
                while (xml.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (
                        xml.eventType == org.xmlpull.v1.XmlPullParser.START_TAG &&
                            xml.name == "DataType"
                    ) {
                        types.add(xml.getAttributeValue(null, "typeId"))
                        assertEquals("true", xml.getAttributeValue(null, "graphical"))
                    }
                    xml.next()
                }
                xml.close()
                assertEquals(setOf("saved-power-curve", "power-history"), types)
                // Canonical progress must continue while both history consumers refresh.
                val before = DiagnosticRuntime.storage()
                DiagnosticRuntime.refreshHistory()
                karoo.system.setRideState(io.hammerhead.karooext.models.RideState.Recording)
                karoo.system.setDataPoint(
                    io.hammerhead.karooext.models.DataPoint(
                        io.hammerhead.karooext.models.DataType.Type.POWER,
                        mapOf(io.hammerhead.karooext.models.DataType.Field.POWER to 250.0),
                    )
                )
                karoo.system.setRideState(io.hammerhead.karooext.models.RideState.Idle)
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.storage().received > before.received }
                }
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.storage().committed > before.committed }
                }
                assertEquals(0L, DiagnosticRuntime.storage().loss)
                assertEquals(result.generation, f.results.newest(result.slot)!!.generation)
                host.stopView(field)
                val frames = field.frames.size
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
                assertEquals(frames, field.frames.size)
            } finally {
                screen.pause().stop().destroy()
                host.close()
                service.destroy()
                karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
            }
        }
    }

    @Test
    fun `dead history binder cancels only the view without creating a source`() {
        var attempts = 0
        val handler =
            object : io.hammerhead.karooext.aidl.IHandler.Stub() {
                override fun onNext(value: android.os.Bundle) {
                    attempts++
                    throw android.os.RemoteException("synthetic dead view")
                }

                override fun onError(message: String) {
                    throw android.os.RemoteException("synthetic dead view")
                }

                override fun onComplete() {}
            }
        val type = HistoryDataType()
        type.startView(
            karoo.app,
            ViewConfig(1 to 1, 480 to 340, 60),
            io.hammerhead.karooext.internal.ViewEmitter(karoo.app.packageName, handler),
        )
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(1, attempts)
        type.close()
        assertTrue(DiagnosticRuntime.readyForSource())
        assertEquals(0, karoo.system.consumerCount)
    }
}
