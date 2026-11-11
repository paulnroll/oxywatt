package com.oxywatt.diagnostic.preview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class PreviewActivityTest {
    @Test
    fun recreationAndNavigationDoNotOwnOrDuplicateTheSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(PreviewRun.start(context, false))
        val controller = Robolectric.buildActivity(PreviewActivity::class.java).setup()
        controller.configurationChange()
        assertFalse(PreviewRun.start(context, false))
        controller.pause().stop().destroy()
        assertFalse(PreviewRun.start(context, false))
        PreviewRun.stop()
        val deadline = System.nanoTime() + 5_000_000_000
        while (
            PreviewRun.snapshot?.contains("EOF / stopped") != true && System.nanoTime() < deadline
        ) Thread.sleep(20)
        assertTrue(
            PreviewRun.snapshot.orEmpty(),
            PreviewRun.snapshot?.contains("EOF / stopped") == true,
        )
    }
}
