package com.oxywatt.diagnostic

import android.content.Context
import com.oxywatt.core.power.PowerCurveEngine
import java.util.UUID
import org.json.JSONObject

internal enum class FieldAppearance {
    LIGHT,
    DARK,
}

internal data class SavedFieldBinding(
    val session: UUID,
    val duration: Int = 300,
    val appearance: FieldAppearance = FieldAppearance.LIGHT,
) {
    init {
        require(duration in PowerCurveEngine.DURATIONS)
    }
}

/** Access only on the existing canonical worker, never from a field view. */
internal class SavedFieldSettings(context: Context) {
    private val preferences by lazy {
        context.getSharedPreferences("saved-curve-field", Context.MODE_PRIVATE)
    }

    fun read(): SavedFieldBinding? {
        val value = preferences.getString("binding", null) ?: return null
        require(value.length <= 512)
        val json = JSONObject(value)
        require(json.length() == 4 && json.get("version") == 1)
        require(json.get("duration") is Int)
        return SavedFieldBinding(
            UUID.fromString(json.getString("session")),
            json.getInt("duration"),
            FieldAppearance.valueOf(json.getString("appearance")),
        )
    }

    fun write(binding: SavedFieldBinding?) {
        val edit = preferences.edit()
        if (binding == null) edit.remove("binding")
        else
            edit.putString(
                "binding",
                JSONObject()
                    .put("version", 1)
                    .put("session", binding.session.toString())
                    .put("duration", binding.duration)
                    .put("appearance", binding.appearance.name)
                    .toString(),
            )
        check(edit.commit()) { "Field settings could not be saved" }
    }
}
