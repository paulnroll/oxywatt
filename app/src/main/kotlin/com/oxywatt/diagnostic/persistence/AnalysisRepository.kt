package com.oxywatt.diagnostic.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.time.Instant
import java.util.UUID
import org.json.JSONObject

data class AnalysisSlot(val session: UUID, val metric: String, val cohort: String) {
    init {
        require(metric.isNotBlank() && cohort.isNotBlank())
    }
}

data class AnalysisProvenance(
    val inputHash: String,
    val sourceVersion: String,
    val profileHash: String,
    val modelVersion: String,
    val preprocessingVersion: String,
    val parameterHash: String,
    val riderSettingsHash: String,
    val trendVersion: String,
    val requestedAt: Instant,
    val codeVersion: String,
) {
    internal fun encode(): String {
        val fields =
            listOf(
                inputHash,
                sourceVersion,
                profileHash,
                modelVersion,
                preprocessingVersion,
                parameterHash,
                riderSettingsHash,
                trendVersion,
            )
        require(fields.all { it.isNotBlank() } && codeVersion.isNotBlank())
        return JSONObject()
            .put("codec", 1)
            .put("codeVersion", codeVersion)
            .put("fields", org.json.JSONArray(fields))
            .put("requestedAt", requestedAt.toString())
            .toString()
    }
}

enum class AnalysisStatus {
    PENDING,
    AVAILABLE,
    UNAVAILABLE,
    FAILED,
}

data class AnalysisTerminal(
    val status: AnalysisStatus,
    val calculatedAt: Instant,
    val value: Double? = null,
    val unit: String? = null,
    val quality: String? = null,
    val reason: String? = null,
) {
    init {
        require(status != AnalysisStatus.PENDING)
        if (status == AnalysisStatus.AVAILABLE)
            require(
                value != null &&
                    value.isFinite() &&
                    !unit.isNullOrBlank() &&
                    !quality.isNullOrBlank() &&
                    reason == null
            )
        else require(value == null && unit == null && quality == null && !reason.isNullOrBlank())
    }

    internal fun encode(): String =
        JSONObject()
            .put("codec", 1)
            .put("status", status.name)
            .put("at", calculatedAt.toString())
            .put("value", value?.let(java.lang.Double::toHexString) ?: JSONObject.NULL)
            .put("unit", unit ?: JSONObject.NULL)
            .put("quality", quality ?: JSONObject.NULL)
            .put("reason", reason ?: JSONObject.NULL)
            .toString()

    companion object {
        internal fun decode(text: String): AnalysisTerminal {
            val j = JSONObject(text)
            require(j.getInt("codec") == 1)
            return AnalysisTerminal(
                AnalysisStatus.valueOf(j.getString("status")),
                Instant.parse(j.getString("at")),
                if (j.isNull("value")) null else j.getString("value").toDouble(),
                if (j.isNull("unit")) null else j.getString("unit"),
                if (j.isNull("quality")) null else j.getString("quality"),
                if (j.isNull("reason")) null else j.getString("reason"),
            )
        }
    }
}

data class StoredAnalysis(
    val generation: Long,
    val slot: AnalysisSlot,
    val provenance: String,
    val terminal: AnalysisTerminal?,
) {
    val status: AnalysisStatus
        get() = terminal?.status ?: AnalysisStatus.PENDING
}

/** Blocking storage port; call from an owner worker. No estimator or trend calculation. */
class AnalysisRepository(context: Context, name: String) : AutoCloseable {
    private val helper = CollectorDatabase(context, name)
    internal var beforeCommit: () -> Unit = {}

    private fun <T> transaction(action: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        var failure: Throwable? = null
        try {
            val result = action(db)
            beforeCommit()
            db.setTransactionSuccessful()
            return result
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            try {
                db.endTransaction()
            } catch (cleanup: Throwable) {
                if (failure != null) failure.addSuppressed(cleanup) else throw cleanup
            }
        }
    }

    fun request(slot: AnalysisSlot, requestId: UUID, provenance: AnalysisProvenance): Long =
        transaction { db ->
            val encoded = provenance.encode()
            db.rawQuery(
                    "SELECT generation,session,metric,cohort,provenance FROM analysis_runs WHERE request=?",
                    arrayOf(requestId.toString()),
                )
                .use {
                    if (it.moveToFirst()) {
                        check(
                            it.getString(1) == slot.session.toString() &&
                                it.getString(2) == slot.metric &&
                                it.getString(3) == slot.cohort &&
                                it.getString(4) == encoded
                        ) {
                            "Conflicting analysis request retry"
                        }
                        return@transaction it.getLong(0)
                    }
                }
            newest(slot)?.let { invalidate(db, it.generation) }
            db.execSQL(
                "INSERT INTO analysis_runs(request,session,metric,cohort,provenance) VALUES(?,?,?,?,?)",
                arrayOf(
                    requestId.toString(),
                    slot.session.toString(),
                    slot.metric,
                    slot.cohort,
                    encoded,
                ),
            )
            val generation =
                db.rawQuery("SELECT last_insert_rowid()", null).use {
                    it.moveToFirst()
                    it.getLong(0)
                }
            db.execSQL(
                "INSERT OR REPLACE INTO analysis_slots(session,metric,cohort,newest) VALUES(?,?,?,?)",
                arrayOf<Any>(slot.session.toString(), slot.metric, slot.cohort, generation),
            )
            generation
        }

    fun complete(generation: Long, terminal: AnalysisTerminal) = transaction { db ->
        val old = checkNotNull(run(generation)) { "Unknown generation" }
        if (old.terminal != null) check(old.terminal == terminal) { "Conflicting terminal retry" }
        else {
            db.execSQL(
                "UPDATE analysis_runs SET terminal=? WHERE generation=?",
                arrayOf<Any>(terminal.encode(), generation),
            )
            invalidate(db, generation)
        }
    }

    private fun invalidate(db: SQLiteDatabase, generation: Long) {
        db.execSQL(
            "DELETE FROM aggregate_members WHERE aggregate_id IN (SELECT aggregate_id FROM aggregate_members WHERE generation=?)",
            arrayOf(generation),
        )
    }

    fun run(generation: Long): StoredAnalysis? =
        helper.readableDatabase
            .rawQuery(
                "SELECT session,metric,cohort,provenance,terminal FROM analysis_runs WHERE generation=?",
                arrayOf(generation.toString()),
            )
            .use {
                if (!it.moveToFirst()) null
                else
                    StoredAnalysis(
                        generation,
                        AnalysisSlot(
                            UUID.fromString(it.getString(0)),
                            it.getString(1),
                            it.getString(2),
                        ),
                        it.getString(3),
                        if (it.isNull(4)) null else AnalysisTerminal.decode(it.getString(4)),
                    )
            }

    fun newest(slot: AnalysisSlot): StoredAnalysis? =
        helper.readableDatabase
            .rawQuery(
                "SELECT newest FROM analysis_slots WHERE session=? AND metric=? AND cohort=?",
                arrayOf(slot.session.toString(), slot.metric, slot.cohort),
            )
            .use { if (it.moveToFirst()) run(it.getLong(0)) else null }

    fun current(slot: AnalysisSlot): StoredAnalysis? =
        newest(slot)?.takeIf { it.status == AnalysisStatus.AVAILABLE }

    fun replaceMembership(aggregateId: String, generations: Set<Long>) = transaction { db ->
        require(aggregateId.isNotBlank() && generations.size <= 256)
        val runs = generations.map { checkNotNull(run(it)) }
        check(runs.map { it.slot }.distinct().size == runs.size)
        check(runs.map { it.slot.metric to it.slot.cohort }.distinct().size <= 1)
        runs.forEach { run ->
            check(current(run.slot)?.generation == run.generation)
            db.rawQuery(
                    "SELECT snapshot FROM sessions WHERE id=?",
                    arrayOf(run.slot.session.toString()),
                )
                .use {
                    check(
                        it.moveToFirst() &&
                            CollectorCodec.session(JSONObject(it.getString(0))).trendEligible
                    )
                }
        }
        db.execSQL("DELETE FROM aggregate_members WHERE aggregate_id=?", arrayOf(aggregateId))
        generations.forEach {
            db.execSQL(
                "INSERT INTO aggregate_members(aggregate_id,generation) VALUES(?,?)",
                arrayOf<Any>(aggregateId, it),
            )
        }
    }

    fun membership(aggregateId: String): Set<Long> =
        helper.readableDatabase
            .rawQuery(
                "SELECT generation FROM aggregate_members WHERE aggregate_id=? ORDER BY generation",
                arrayOf(aggregateId),
            )
            .use { buildSet { while (it.moveToNext()) add(it.getLong(0)) } }

    override fun close() = helper.close()
}
