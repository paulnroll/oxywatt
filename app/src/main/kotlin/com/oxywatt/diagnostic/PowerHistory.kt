package com.oxywatt.diagnostic

import android.content.Context
import com.oxywatt.core.power.PowerCurveEngine
import com.oxywatt.core.power.PowerCurvePolicy
import com.oxywatt.diagnostic.persistence.*
import java.time.Instant
import java.util.UUID
import org.json.JSONObject

internal data class HistoryConfig(
    val duration: Int = 300,
    val appearance: FieldAppearance = FieldAppearance.LIGHT,
) {
    init {
        require(duration in PowerCurveEngine.DURATIONS)
    }
}

internal class HistorySettings(context: Context) {
    private val prefs = context.getSharedPreferences("power-history", Context.MODE_PRIVATE)

    fun read(): HistoryConfig {
        val raw = prefs.getString("config", null) ?: return HistoryConfig()
        require(raw.length <= 256)
        val j = JSONObject(raw)
        require(j.length() == 3 && j.get("version") == 1 && j.get("duration") is Int)
        return HistoryConfig(
            j.getInt("duration"),
            FieldAppearance.valueOf(j.getString("appearance")),
        )
    }

    fun write(config: HistoryConfig) {
        check(
            prefs
                .edit()
                .putString(
                    "config",
                    JSONObject()
                        .put("version", 1)
                        .put("duration", config.duration)
                        .put("appearance", config.appearance.name)
                        .toString(),
                )
                .commit()
        )
    }
}

internal enum class HistoryStatus {
    NO_WORKER,
    LOADING,
    READY,
    READ_ERROR,
    SETTINGS_ERROR,
}

internal enum class HistoryReason {
    VALUE,
    INCOMPLETE,
    INCOMPATIBLE,
    NO_RESULT,
    PENDING,
    CALCULATING,
    FAILED,
    UNAVAILABLE,
    REMOVED,
    LIMITED,
    NO_WINDOW,
}

internal data class HistoryEntry(
    val session: UUID,
    val date: Instant,
    val reason: HistoryReason,
    val watts: Double? = null,
    val generation: Long? = null,
    val provenance: String? = null,
)

internal data class HistorySnapshot(
    val status: HistoryStatus = HistoryStatus.NO_WORKER,
    val config: HistoryConfig = HistoryConfig(),
    val entries: List<HistoryEntry> = emptyList(),
    val truncated: Boolean = false,
    val refreshedAt: Instant? = null,
)

/** Sole canonical worker owns reads. No history operation requests an analysis. */
internal class HistoryController(
    private val inputs: InputRepository,
    private val analyses: AnalysisRepository,
    private val settings: HistorySettings,
    private val now: () -> Long,
    private val utc: () -> Instant = Instant::now,
) {
    @Volatile private var view = HistorySnapshot(HistoryStatus.LOADING)
    private val gate = Any()
    private var requested = false
    private var configuration: HistoryConfig? = null
    private var initialized = false
    private var settingsFailed = false
    private var config = HistoryConfig()
    private var nextStart = 0L
    private var page: StoredSessionPage? = null
    private val gathered = mutableListOf<HistoryEntry>()
    private var index = 0
    private var verification = -1
    private var activeSession: UUID? = null
    private var lastResult: Pair<Long, AnalysisStatus>? = null
    val hasWork
        get() = page != null

    fun snapshot() = view

    fun refresh(config: HistoryConfig? = null) =
        synchronized(gate) {
            requested = true
            if (config != null) configuration = config
        }

    private fun reset() {
        page = null
        gathered.clear()
        index = 0
        verification = -1
    }

    private fun loading() {
        view = view.copy(status = HistoryStatus.LOADING, config = config, entries = emptyList())
    }

    fun pump(subscribed: Boolean, result: StoredAnalysis?, invalidating: UUID? = null) {
        try {
            if (!initialized) {
                initialized = true
                try {
                    config = settings.read()
                    view = view.copy(config = config)
                } catch (_: Exception) {
                    settingsFailed = true
                    view = HistorySnapshot(HistoryStatus.SETTINGS_ERROR)
                }
            }
            val action =
                synchronized(gate) {
                    (requested to configuration).also {
                        requested = false
                        configuration = null
                    }
                }
            if (action.first) {
                action.second?.let {
                    try {
                        settings.write(it)
                        config = it
                        settingsFailed = false
                    } catch (_: Exception) {
                        reset()
                        settingsFailed = true
                        view = HistorySnapshot(HistoryStatus.SETTINGS_ERROR, config)
                        return
                    }
                }
                reset()
                loading()
            }
            if (settingsFailed) {
                reset()
                view = HistorySnapshot(HistoryStatus.SETTINGS_ERROR, config)
                return
            }
            val key = result?.let { it.generation to it.status }
            val changed = key != null && key != lastResult
            if (key != null) lastResult = key
            // The only writer runs on this same worker before history. Suppress immediately,
            // including allocation/calculation states that have no terminal StoredAnalysis yet.
            val activeChanged = activeSession != invalidating
            activeSession = invalidating
            if (changed || activeChanged) {
                reset()
                loading()
            }
            if (!subscribed) {
                if (page != null) {
                    reset()
                    loading()
                }
                return
            }
            if (page == null) {
                if (now() < nextStart) return
                nextStart = now() + 5_000_000_000L
                loading()
                page = inputs.recentSessions()
                return
            }
            val sessions = page!!.sessions
            if (index < sessions.size) {
                gathered.add(load(sessions[index++].id, sessions[index - 1].firstReceipt.utc))
                return
            }
            if (verification < 0) verification = 0
            if (verification < gathered.size) {
                val previous = gathered[verification]
                // Reread one complete entry per turn; deleted/changed sessions and newest slots
                // replace that entry rather than reviving its earlier result.
                gathered[verification++] = load(previous.session, previous.date)
                return
            }
            view =
                HistorySnapshot(
                    HistoryStatus.READY,
                    config,
                    java.util.Collections.unmodifiableList(
                        gathered.sortedWith(
                            compareBy<HistoryEntry> { it.date }.thenBy { it.session.toString() }
                        )
                    ),
                    page!!.truncated,
                    utc(),
                )
            reset()
        } catch (_: Exception) {
            reset()
            view =
                view.copy(status = HistoryStatus.READ_ERROR, config = config, entries = emptyList())
        }
    }

    private fun load(id: UUID, originalDate: Instant): HistoryEntry {
        val session =
            inputs.session(id) ?: return HistoryEntry(id, originalDate, HistoryReason.REMOVED)
        if (!session.trendEligible)
            return HistoryEntry(id, session.firstReceipt.utc, HistoryReason.INCOMPLETE)
        if (id == activeSession)
            return HistoryEntry(id, session.firstReceipt.utc, HistoryReason.CALCULATING)
        val saved =
            try {
                loadSavedField(
                    inputs,
                    analyses,
                    SavedFieldBinding(id, config.duration, config.appearance),
                )
            } catch (_: IllegalArgumentException) {
                return HistoryEntry(id, session.firstReceipt.utc, HistoryReason.INCOMPATIBLE)
            } catch (_: IllegalStateException) {
                return HistoryEntry(id, session.firstReceipt.utc, HistoryReason.INCOMPATIBLE)
            }
        val current = analyses.newest(PowerCurveCalculation.realSlot(id))
        if (current != null) {
            val fields = JSONObject(current.provenance).getJSONArray("fields")
            val curve = current.terminal?.curve?.snapshot
            // Profile hashes identify input history, not a physiological comparability model.
            // Observed watts use no rider settings or calibration equation in this cohort.
            if (
                (curve != null &&
                    (curve.policy != PowerCurvePolicy() ||
                        curve.provenance.preprocessingVersion != "canonical-power-hold-v1")) ||
                    fields.getString(6) != "not-applicable-v1" ||
                    fields.getString(7) != "not-applicable-v1"
            )
                return HistoryEntry(
                    id,
                    session.firstReceipt.utc,
                    HistoryReason.INCOMPATIBLE,
                    generation = current.generation,
                    provenance = current.provenance,
                )
        }
        val watts = saved.points.singleOrNull { it.seconds == config.duration }?.watts
        val reason =
            when (saved.status) {
                SavedFieldStatus.SAVED ->
                    when {
                        saved.limited -> HistoryReason.LIMITED
                        watts == null -> HistoryReason.NO_WINDOW
                        else -> HistoryReason.VALUE
                    }
                SavedFieldStatus.PENDING -> HistoryReason.PENDING
                SavedFieldStatus.FAILED -> HistoryReason.FAILED
                SavedFieldStatus.UNAVAILABLE ->
                    if (current == null) HistoryReason.NO_RESULT else HistoryReason.UNAVAILABLE
                SavedFieldStatus.REMOVED -> HistoryReason.REMOVED
                else -> HistoryReason.INCOMPATIBLE
            }
        return HistoryEntry(
            id,
            session.firstReceipt.utc,
            reason,
            watts.takeIf { reason == HistoryReason.VALUE },
            saved.generation,
            current?.provenance,
        )
    }
}
