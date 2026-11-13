package com.oxywatt.diagnostic

import com.oxywatt.core.collection.*
import com.oxywatt.core.power.PowerUnavailableReason
import com.oxywatt.diagnostic.persistence.*
import java.time.Instant

internal enum class SavedFieldStatus {
    SETUP,
    LOADING,
    SAVED,
    REMOVED,
    PENDING,
    FAILED,
    UNAVAILABLE,
    INVALID,
    READ_ERROR,
    SETTINGS_ERROR,
    NO_WORKER,
}

internal data class FieldPower(val seconds: Int, val watts: Double?, val resourceLimited: Boolean)

internal data class SavedFieldSnapshot(
    val status: SavedFieldStatus = SavedFieldStatus.NO_WORKER,
    val binding: SavedFieldBinding? = null,
    val receiptDate: Instant? = null,
    val generation: Long? = null,
    val points: List<FieldPower> = emptyList(),
    val limited: Boolean = false,
    val bindingError: SavedFieldStatus? = null,
)

/** Worker-owned reads/settings, with one coalesced app command and an immutable view projection. */
internal class SavedFieldController(
    private val inputs: InputRepository,
    private val analyses: AnalysisRepository,
    private val settings: SavedFieldSettings,
    private val now: () -> Long,
) {
    @Volatile private var view = SavedFieldSnapshot(SavedFieldStatus.LOADING)
    private val gate = Any()
    private var command: Pair<Boolean, SavedFieldBinding?>? = null
    private var initialized = false
    private var binding: SavedFieldBinding? = null
    private var nextRead = 0L
    private var lastResult: Pair<Long, AnalysisStatus>? = null
    private var bindingError: SavedFieldStatus? = null
    private var reload = false

    fun snapshot() = view

    fun bind(binding: SavedFieldBinding) = synchronized(gate) { command = true to binding }

    fun clear() = synchronized(gate) { command = false to null }

    fun pump(subscribed: Boolean, result: StoredAnalysis?) {
        val action = synchronized(gate) { command.also { command = null } }
        val first = !initialized
        if (first) {
            initialized = true
            try {
                binding = settings.read()
                view =
                    SavedFieldSnapshot(
                        if (binding == null) SavedFieldStatus.SETUP else SavedFieldStatus.LOADING,
                        binding,
                    )
            } catch (_: Exception) {
                view = SavedFieldSnapshot(SavedFieldStatus.SETTINGS_ERROR)
            }
        }
        val resultKey =
            result
                ?.takeIf { it.slot.session == binding?.session }
                ?.let { it.generation to it.status }
        val completed = resultKey != null && resultKey != lastResult
        if (resultKey != null) lastResult = resultKey
        if (action == null && !first && !completed && !reload && (!subscribed || now() < nextRead))
            return
        reload = false
        nextRead = now() + 5_000_000_000L
        try {
            if (action != null) {
                val candidate = action.second
                val loaded =
                    if (action.first) load(checkNotNull(candidate))
                    else SavedFieldSnapshot(SavedFieldStatus.SETUP)
                if (action.first && loaded.status != SavedFieldStatus.SAVED) {
                    bindingError = loaded.status
                    // A refused candidate cannot relabel the old binding with its date/generation.
                    view = view.copy(bindingError = bindingError)
                    reload = view.status == SavedFieldStatus.LOADING
                    return
                }
                try {
                    settings.write(candidate)
                } catch (_: Exception) {
                    view = SavedFieldSnapshot(SavedFieldStatus.SETTINGS_ERROR, binding)
                    return
                }
                binding = candidate
                bindingError = null
                view = loaded
            } else if (view.status != SavedFieldStatus.SETTINGS_ERROR) {
                view =
                    (binding?.let(::load) ?: SavedFieldSnapshot(SavedFieldStatus.SETUP)).copy(
                        bindingError = bindingError
                    )
            }
        } catch (_: android.database.sqlite.SQLiteException) {
            view = SavedFieldSnapshot(SavedFieldStatus.READ_ERROR, binding)
        } catch (_: Exception) {
            view = SavedFieldSnapshot(SavedFieldStatus.INVALID, binding)
        }
    }

    private fun load(b: SavedFieldBinding) = loadSavedField(inputs, analyses, b)
}

internal fun loadSavedField(
    inputs: InputRepository,
    analyses: AnalysisRepository,
    b: SavedFieldBinding,
): SavedFieldSnapshot {
    fun state(s: SavedFieldStatus, date: Instant? = null, generation: Long? = null) =
        SavedFieldSnapshot(s, b, date, generation)
    val session = inputs.session(b.session) ?: return state(SavedFieldStatus.REMOVED)
    require(
        session.phase == SessionPhase.ENDED_LOCALLY &&
            session.origin == SessionOrigin.OBSERVED_START &&
            session.ambiguityGroupId == null
    )
    require(session.source.adapterId == "karoo" && session.source.adapterVersion == "1.1.9")
    require(
        session.normalizationVersion == "karoo-normalization-v1" &&
            session.policy == LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000)
    )
    val saved =
        readRealSavedCurve(analyses, session)
            ?: return state(SavedFieldStatus.UNAVAILABLE, session.firstReceipt.utc)
    if (saved.status != AnalysisStatus.AVAILABLE)
        return state(
            when (saved.status) {
                AnalysisStatus.PENDING -> SavedFieldStatus.PENDING
                AnalysisStatus.FAILED -> SavedFieldStatus.FAILED
                else -> SavedFieldStatus.UNAVAILABLE
            },
            session.firstReceipt.utc,
            saved.generation,
        )
    val curve = checkNotNull(saved.terminal?.curve).snapshot
    return SavedFieldSnapshot(
        SavedFieldStatus.SAVED,
        b,
        session.firstReceipt.utc,
        saved.generation,
        java.util.Collections.unmodifiableList(
            curve.durations.map {
                FieldPower(
                    it.durationSeconds,
                    it.record?.meanWatts,
                    it.unavailableReason == PowerUnavailableReason.RESOURCE_LIMIT,
                )
            }
        ),
        curve.resourceLimited,
    )
}
