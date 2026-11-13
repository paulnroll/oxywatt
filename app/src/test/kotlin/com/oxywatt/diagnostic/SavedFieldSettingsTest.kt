package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class SavedFieldSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `explicit older UUID duration and appearance survive a new settings owner and clear`() {
        val binding = SavedFieldBinding(UUID(0, 42), 1200, FieldAppearance.DARK)
        SavedFieldSettings(context).write(binding)
        assertEquals(binding, SavedFieldSettings(context).read())
        SavedFieldSettings(context).write(null)
        assertNull(SavedFieldSettings(context).read())
    }

    @Test
    fun `unknown corrupt oversize and unsupported settings cannot restore a default numeric binding`() {
        val prefs = context.getSharedPreferences("saved-curve-field", Context.MODE_PRIVATE)
        for (value in
            listOf(
                "{}",
                "broken",
                "x".repeat(513),
                """{"version":2,"session":"${UUID(0,42)}","duration":300,"appearance":"LIGHT"}""",
                """{"version":1,"session":"${UUID(0,42)}","duration":42,"appearance":"LIGHT"}""",
            )) {
            prefs.edit().putString("binding", value).commit()
            assertThrows(Exception::class.java) { SavedFieldSettings(context).read() }
        }
        SavedFieldSettings(context).write(null)
        assertNull(SavedFieldSettings(context).read())
    }
}
