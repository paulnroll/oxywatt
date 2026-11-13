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
class SavedFieldRuntimeTest {
    @get:Rule val karoo = FakeKarooRule()

    private fun views(v: View): List<View> =
        listOf(v) +
            if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) }
            else emptyList()

    @Test
    fun `broken view binder stops rendering without starting acquisition or crashing its process`() {
        var attempts = 0
        var errors = 0
        val handler =
            object : io.hammerhead.karooext.aidl.IHandler.Stub() {
                override fun onNext(value: android.os.Bundle) {
                    attempts++
                    throw android.os.RemoteException("test-only dead view")
                }

                override fun onError(message: String) {
                    errors++
                    throw android.os.RemoteException("test-only dead view")
                }

                override fun onComplete() {}
            }
        val field = SavedCurveDataType()
        field.startView(
            karoo.app,
            ViewConfig(60 to 30, 480 to 340, 60),
            io.hammerhead.karooext.internal.ViewEmitter(karoo.app.packageName, handler),
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, attempts)
        assertEquals(1, errors)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(1, attempts)
        field.close()
        assertEquals(0, karoo.system.consumerCount)
        assertTrue(DiagnosticRuntime.readyForSource())
    }

    @Test
    fun `app binds older curve and actual SDK field survives recreation without analysis or extra consumers`() {
        SavedFieldSettings(karoo.app).write(null)
        FieldFixture(karoo.app, "oxywatt-live-diagnostic.db").use { fixture ->
            val result = fixture.calculate()
            val service = Robolectric.buildService(DiagnosticExtension::class.java).create()
            val screen = Robolectric.buildActivity(MainActivity::class.java)
            val host =
                FakeKarooHost(
                    IKarooExtension.Stub.asInterface(service.get().onBind(Intent())),
                    RobolectricPump.invoke(),
                )
            try {
                karoo.awaitValue {
                    true.takeIf {
                        karoo.system.consumerCount == 5 && !DiagnosticRuntime.sessions().loading
                    }
                }
                screen.setup()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
                fun all() = views(screen.get().window.decorView)
                all()
                    .filterIsInstance<Button>()
                    .single { it.tag == fixture.ids.first() }
                    .performClick()
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.curve().status == LiveCurveStatus.SAVED }
                }
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
                all()
                    .filterIsInstance<Spinner>()
                    .single {
                        it.contentDescription == karoo.app.getString(R.string.field_duration_label)
                    }
                    .setSelection(0)
                all()
                    .filterIsInstance<Spinner>()
                    .single {
                        it.contentDescription ==
                            karoo.app.getString(R.string.field_appearance_label)
                    }
                    .setSelection(1)
                val apply =
                    all().filterIsInstance<Button>().single {
                        it.text == karoo.app.getString(R.string.field_bind)
                    }
                assertTrue(apply.isEnabled)
                apply.performClick()
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.savedField().status == SavedFieldStatus.SAVED }
                }
                val saved = DiagnosticRuntime.savedField()
                assertEquals(
                    SavedFieldBinding(fixture.ids.first(), 5, FieldAppearance.DARK),
                    saved.binding,
                )
                assertEquals(result.generation, saved.generation)
                val field =
                    host.startView(SavedCurveDataType.TYPE, ViewConfig(60 to 30, 480 to 340, 60))
                karoo.awaitValue { true.takeIf { field.frames.isNotEmpty() } }
                val frame =
                    field.frames.last().apply(karoo.app, android.widget.FrameLayout(karoo.app))
                assertTrue(frame.contentDescription.contains("100 W"))
                assertTrue(frame.contentDescription.contains("1 Jan 1970"))
                val frames = field.frames.size
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
                assertEquals("Unchanged snapshots emit no frames", frames, field.frames.size)
                assertEquals(5, karoo.system.consumerCount)
                screen.recreate()
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
                assertEquals(saved, DiagnosticRuntime.savedField())
                DiagnosticRuntime.selectCurve(fixture.ids.last())
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.curve().session?.id == fixture.ids.last() }
                }
                assertEquals(saved, DiagnosticRuntime.savedField())
                val canonicalBefore = DiagnosticRuntime.storage().revision
                val received = DiagnosticRuntime.storage().received
                karoo.system.setRideState(io.hammerhead.karooext.models.RideState.Recording)
                karoo.system.setDataPoint(
                    io.hammerhead.karooext.models.DataPoint(
                        io.hammerhead.karooext.models.DataType.Type.POWER,
                        mapOf(io.hammerhead.karooext.models.DataType.Field.POWER to 250.0),
                    )
                )
                karoo.awaitValue { true.takeIf { DiagnosticRuntime.storage().received > received } }
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.storage().revision > canonicalBefore }
                }
                assertEquals(
                    "New recording does not retarget or recalculate the saved field",
                    saved,
                    DiagnosticRuntime.savedField(),
                )
                val compact =
                    host.startView(SavedCurveDataType.TYPE, ViewConfig(30 to 15, 240 to 160, 40))
                karoo.awaitValue { true.takeIf { compact.frames.isNotEmpty() } }
                host.stopView(field)
                host.stopView(compact)
                val count = field.frames.size
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
                assertEquals(count, field.frames.size)
                assertEquals(5, karoo.system.consumerCount)
                assertEquals(result.generation, fixture.results.newest(result.slot)!!.generation)
                assertNull(DiagnosticRuntime.curveTiming())
                assertEquals(2, service.get().types.size)
                val xml = karoo.app.resources.getXml(R.xml.extension_info)
                while (
                    xml.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT &&
                        xml.name != "DataType"
                ) xml.next()
                assertEquals(SavedCurveDataType.TYPE, xml.getAttributeValue(null, "typeId"))
                assertEquals("true", xml.getAttributeValue(null, "graphical"))
                xml.close()
            } finally {
                screen.pause().stop().destroy()
                host.close()
                service.destroy()
                karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
            }
            assertEquals(SavedFieldStatus.NO_WORKER, DiagnosticRuntime.savedField().status)
            assertTrue(DiagnosticRuntime.savedField().points.isEmpty())
            // A fresh runtime reloads the durable UUID, independently of diagnostic selection.
            val replacement = Robolectric.buildService(DiagnosticExtension::class.java).create()
            try {
                karoo.awaitValue {
                    true.takeIf { DiagnosticRuntime.savedField().status == SavedFieldStatus.SAVED }
                }
                assertEquals(fixture.ids.first(), DiagnosticRuntime.savedField().binding!!.session)
                assertEquals(result.generation, DiagnosticRuntime.savedField().generation)
                assertNull(DiagnosticRuntime.curve().session)
            } finally {
                replacement.destroy()
                karoo.awaitValue { true.takeIf { DiagnosticRuntime.readyForSource() } }
                SavedFieldSettings(karoo.app).write(null)
            }
        }
    }
}
