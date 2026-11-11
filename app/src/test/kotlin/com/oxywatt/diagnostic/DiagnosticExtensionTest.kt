package com.oxywatt.diagnostic

import android.content.Intent
import fi.nikosavola.karooext.testing.FakeKarooHost
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import fi.nikosavola.karooext.testing.robolectric.RobolectricPump
import io.hammerhead.karooext.aidl.IKarooExtension
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class DiagnosticExtensionTest {
    @get:Rule val karoo = FakeKarooRule()

    @Test
    fun `host starts probe and destruction releases consumers`() {
        val controller = Robolectric.buildService(DiagnosticExtension::class.java).create()
        val host =
            FakeKarooHost(
                IKarooExtension.Stub.asInterface(controller.get().onBind(Intent())),
                RobolectricPump.invoke(),
            )
        karoo.awaitValue { true.takeIf { karoo.system.consumerCount == 5 } }
        val fit = host.startFit()
        karoo.awaitValue {
            true.takeIf {
                ShadowLog.getLogsForTag("OxyWattProbe").any { it.msg == "fit binding started" }
            }
        }
        host.stopFit(fit)
        karoo.awaitValue {
            true.takeIf {
                ShadowLog.getLogsForTag("OxyWattProbe").any { it.msg == "fit binding stopped" }
            }
        }
        // FIT cancellation remains an observation; service-owned acquisition survives it.
        assertEquals(5, karoo.system.consumerCount)
        assertTrue(fit.items.isEmpty())
        host.close()
        // Host close cancels FIT; Android destruction releases the service subscriptions.
        controller.destroy()
        assertEquals(0, karoo.system.consumerCount)
        assertTrue(fit.items.isEmpty())
    }
}
