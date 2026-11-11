package com.oxywatt.diagnostic.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.oxywatt.core.collection.*
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Frozen effects are retained on write failure; never recompute them for a storage retry. */
class PreparedInputWrite
internal constructor(
    val batchId: UUID,
    val expectedRevision: Long,
    internal val document: String,
)

data class StoredObservation(
    val position: Long,
    val observation: SourceObservation,
    val sessionId: UUID?,
    val segment: Long?,
)

data class StoredBatchMetadata(
    val policy: AdmissionPolicy,
    val loss: AdmissionLoss?,
    val terminal: InterruptionReason?,
)

data class StoredCollectorState(val revision: Long, val checkpoint: CollectorCheckpoint?)

/** Blocking adapter: its owner must call it on a worker, never from a sensor or UI callback. */
class InputRepository(context: Context, name: String) : AutoCloseable {
    private val helper =
        object : SQLiteOpenHelper(context.applicationContext, name, null, 1) {
            override fun onConfigure(db: SQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE batches (id TEXT PRIMARY KEY NOT NULL, digest TEXT NOT NULL, revision INTEGER NOT NULL UNIQUE, epoch TEXT NOT NULL, document TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE sessions (id TEXT PRIMARY KEY NOT NULL, snapshot TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE observations (position INTEGER PRIMARY KEY, epoch TEXT NOT NULL, sequence INTEGER NOT NULL, batch TEXT NOT NULL REFERENCES batches(id), session TEXT REFERENCES sessions(id), segment INTEGER, data TEXT NOT NULL, UNIQUE(epoch,sequence))"
                )
                db.execSQL("CREATE INDEX observations_session ON observations(session,position)")
                db.execSQL(
                    "CREATE TABLE effects (batch TEXT NOT NULL REFERENCES batches(id), item INTEGER NOT NULL, data TEXT NOT NULL, PRIMARY KEY(batch,item))"
                )
                db.execSQL(
                    "CREATE TABLE collector_state (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL, checkpoint TEXT NOT NULL)"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                error("Unsupported input database migration")
            }
        }

    fun state(): StoredCollectorState =
        helper.readableDatabase
            .rawQuery("SELECT revision, checkpoint FROM collector_state WHERE id=1", null)
            .use {
                if (!it.moveToFirst()) StoredCollectorState(0, null)
                else
                    StoredCollectorState(
                        it.getLong(0),
                        CollectorCodec.checkpoint(JSONObject(it.getString(1))),
                    )
            }

    fun checkpoint(): CollectorCheckpoint? = state().checkpoint

    fun observations(
        afterPosition: Long = 0,
        limit: Int = 256,
        sessionId: UUID? = null,
    ): List<StoredObservation> {
        require(afterPosition >= 0 && limit in 1..256)
        return helper.readableDatabase
            .rawQuery(
                if (sessionId == null)
                    "SELECT position,data,session,segment FROM observations WHERE position>? ORDER BY position LIMIT ?"
                else
                    "SELECT position,data,session,segment FROM observations WHERE session=? AND position>? ORDER BY position LIMIT ?",
                if (sessionId == null) arrayOf(afterPosition.toString(), limit.toString())
                else arrayOf(sessionId.toString(), afterPosition.toString(), limit.toString()),
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(
                        StoredObservation(
                            cursor.getLong(0),
                            CollectorCodec.observation(JSONObject(cursor.getString(1))),
                            if (cursor.isNull(2)) null else UUID.fromString(cursor.getString(2)),
                            if (cursor.isNull(3)) null else cursor.getLong(3),
                        )
                    )
                }
            }
    }

    fun effects(batchId: UUID, afterIndex: Int = -1, limit: Int = 256): List<LifecycleUpdate> {
        require(afterIndex >= -1 && limit in 1..256)
        return helper.readableDatabase
            .rawQuery(
                "SELECT data FROM effects WHERE batch=? AND item>? ORDER BY item LIMIT ?",
                arrayOf(batchId.toString(), afterIndex.toString(), limit.toString()),
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val value = JSONObject(cursor.getString(0))
                        require(value.getInt("codec") == 1)
                        add(CollectorCodec.update(value.getJSONObject("update")))
                    }
                }
            }
    }

    fun session(id: UUID): SessionSnapshot? =
        helper.readableDatabase
            .rawQuery("SELECT snapshot FROM sessions WHERE id=?", arrayOf(id.toString()))
            .use {
                if (it.moveToFirst()) CollectorCodec.session(JSONObject(it.getString(0))) else null
            }

    fun batchMetadata(id: UUID): StoredBatchMetadata? =
        helper.readableDatabase
            .rawQuery("SELECT document FROM batches WHERE id=?", arrayOf(id.toString()))
            .use {
                if (!it.moveToFirst()) null
                else {
                    val value = JSONObject(it.getString(0))
                    require(value.getInt("codec") == 1)
                    StoredBatchMetadata(
                        CollectorCodec.admissionPolicy(value),
                        CollectorCodec.loss(value),
                        if (value.isNull("terminal")) null
                        else InterruptionReason.valueOf(value.getString("terminal")),
                    )
                }
            }

    internal var beforeCheckpoint: () -> Unit = {}

    fun prepare(
        batch: AdmissionBatch,
        lifecycle: CollectorLifecycle,
        expectedRevision: Long,
        batchId: UUID = UUID.randomUUID(),
    ): PreparedInputWrite {
        require(expectedRevision >= 0)
        check(committedRevision(batchId) == null) {
            "Batch already committed; do not replay its lifecycle"
        }
        check(state().revision == expectedRevision) { "Stale collector revision" }
        for (input in batch.observations) {
            helper.readableDatabase
                .rawQuery(
                    "SELECT position FROM observations WHERE epoch=? AND sequence=?",
                    arrayOf(input.stamp.epochId.toString(), input.stamp.ingestSequence.toString()),
                )
                .use {
                    check(!it.moveToFirst()) {
                        "Committed receipt cannot be replayed into lifecycle"
                    }
                }
        }
        val effects = batch.applyTo(lifecycle)
        return freeze(
            batch.observations,
            effects,
            lifecycle.checkpoint(),
            CollectorCodec.metadata(batch),
            expectedRevision,
            batchId,
        )
    }

    fun prepareEffect(
        update: LifecycleUpdate,
        admissionPolicy: AdmissionPolicy,
        expectedRevision: Long,
        batchId: UUID = UUID.randomUUID(),
    ): PreparedInputWrite {
        require(update.envelope == null) { "Standalone effects cannot introduce input envelopes" }
        require(expectedRevision >= 0)
        check(committedRevision(batchId) == null) { "Batch already committed" }
        check(state().revision == expectedRevision) { "Stale collector revision" }
        return freeze(
            emptyList(),
            listOf(update),
            update.checkpoint,
            CollectorCodec.metadata(admissionPolicy),
            expectedRevision,
            batchId,
        )
    }

    private fun freeze(
        inputs: List<SourceObservation>,
        effects: List<LifecycleUpdate>,
        checkpoint: CollectorCheckpoint,
        metadata: JSONObject,
        expectedRevision: Long,
        batchId: UUID,
    ): PreparedInputWrite {
        val document =
            JSONObject()
                .put("codec", 1)
                .put("metadata", metadata)
                .put("observations", JSONArray(inputs.map(CollectorCodec::observation)))
                .put("effects", JSONArray(effects.map(CollectorCodec::update)))
                .put("checkpoint", CollectorCodec.checkpoint(checkpoint))
                .toString()
        return PreparedInputWrite(batchId, expectedRevision, document)
    }

    internal fun limitStorageForTest(bytes: Long): Long =
        helper.writableDatabase.setMaximumSize(bytes)

    fun committedRevision(batchId: UUID): Long? =
        helper.readableDatabase
            .rawQuery("SELECT revision FROM batches WHERE id=?", arrayOf(batchId.toString()))
            .use { if (it.moveToFirst()) it.getLong(0) else null }

    fun commit(write: PreparedInputWrite): Long {
        val db = helper.writableDatabase
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(
                    (write.expectedRevision.toString() + "\n" + write.document).toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") { "%02x".format(it) }
        db.beginTransaction()
        var failure: Throwable? = null
        try {
            db.rawQuery(
                    "SELECT digest, revision FROM batches WHERE id=?",
                    arrayOf(write.batchId.toString()),
                )
                .use {
                    if (it.moveToFirst()) {
                        check(it.getString(0) == digest) { "Conflicting batch retry" }
                        return it.getLong(1)
                    }
                }
            val previous = state()
            check(previous.revision == write.expectedRevision) { "Stale collector revision" }
            val revision = Math.incrementExact(write.expectedRevision)
            val document = JSONObject(write.document)
            require(document.getInt("codec") == 1)
            val checkpoint = CollectorCodec.checkpoint(document.getJSONObject("checkpoint"))
            previous.checkpoint?.let { prior ->
                if (prior.epochId == checkpoint.epochId) {
                    prior.lastReceipt?.let { old ->
                        val next = checkNotNull(checkpoint.lastReceipt)
                        check(
                            next.ingestSequence >= old.ingestSequence &&
                                next.elapsedNanos >= old.elapsedNanos
                        ) {
                            "Regressing checkpoint"
                        }
                    }
                } else {
                    db.rawQuery(
                            "SELECT id FROM batches WHERE epoch=? LIMIT 1",
                            arrayOf(checkpoint.epochId.toString()),
                        )
                        .use { check(!it.moveToFirst()) { "Acquisition epoch cannot be reopened" } }
                }
            }
            db.execSQL(
                "INSERT INTO batches(id,digest,revision,epoch,document) VALUES(?,?,?,?,?)",
                arrayOf<Any>(
                    write.batchId.toString(),
                    digest,
                    revision,
                    checkpoint.epochId.toString(),
                    document.getJSONObject("metadata").toString(),
                ),
            )
            val updates = document.getJSONArray("effects")
            val envelopes = mutableMapOf<Pair<UUID, Long>, LifecycleUpdate>()
            for (index in 0 until updates.length()) {
                val update = CollectorCodec.update(updates.getJSONObject(index))
                for (snapshot in update.sessionChanges) {
                    val old = session(snapshot.id)
                    if (old != null) {
                        check(
                            old.epochId == snapshot.epochId &&
                                old.firstReceipt == snapshot.firstReceipt &&
                                old.origin == snapshot.origin &&
                                old.source == snapshot.source &&
                                old.policy == snapshot.policy &&
                                old.normalizationVersion == snapshot.normalizationVersion
                        ) {
                            "Session UUID collision"
                        }
                        check(
                            snapshot.lastReceipt.ingestSequence >= old.lastReceipt.ingestSequence &&
                                snapshot.lastReceipt.elapsedNanos >= old.lastReceipt.elapsedNanos &&
                                snapshot.segmentIndex >= old.segmentIndex
                        ) {
                            "Regressing session"
                        }
                        if (
                            old.phase == SessionPhase.INTERRUPTED ||
                                old.phase == SessionPhase.ENDED_LOCALLY
                        )
                            check(old == snapshot) { "Terminal session cannot be reopened" }
                    }
                    db.execSQL(
                        "INSERT OR REPLACE INTO sessions(id,snapshot) VALUES(?,?)",
                        arrayOf(
                            snapshot.id.toString(),
                            CollectorCodec.session(snapshot).toString(),
                        ),
                    )
                }
                update.envelope?.let {
                    envelopes[it.stamp.epochId to it.stamp.ingestSequence] = update
                }
                db.execSQL(
                    "INSERT INTO effects(batch,item,data) VALUES(?,?,?)",
                    arrayOf<Any>(
                        write.batchId.toString(),
                        index,
                        JSONObject()
                            .put("codec", 1)
                            .put("update", updates.getJSONObject(index))
                            .toString(),
                    ),
                )
            }
            val inputs = document.getJSONArray("observations")
            for (index in 0 until inputs.length()) {
                val encoded = inputs.getJSONObject(index)
                val input = CollectorCodec.observation(encoded)
                val update = envelopes[input.stamp.epochId to input.stamp.ingestSequence]
                db.execSQL(
                    "INSERT INTO observations(epoch,sequence,batch,session,segment,data) VALUES(?,?,?,?,?,?)",
                    arrayOf<Any?>(
                        input.stamp.epochId.toString(),
                        input.stamp.ingestSequence,
                        write.batchId.toString(),
                        update?.envelope?.sessionId?.toString(),
                        update?.recordingSegment,
                        encoded.toString(),
                    ),
                )
            }
            checkpoint.latestSession?.let {
                check(session(it.id) == it) {
                    "Checkpoint session does not match persisted effects"
                }
            }
            previous.checkpoint?.let { prior ->
                val active =
                    prior.latestSession?.takeIf {
                        it.phase == SessionPhase.RECORDING || it.phase == SessionPhase.PAUSED
                    }
                if (active != null && prior.epochId != checkpoint.epochId) {
                    val recovered = session(active.id)
                    check(
                        recovered?.phase == SessionPhase.INTERRUPTED &&
                            recovered.interruptionReason == InterruptionReason.PROCESS_RESTART
                    ) {
                        "New epoch requires conservative recovery effects"
                    }
                }
            }
            beforeCheckpoint()
            db.execSQL(
                "INSERT OR REPLACE INTO collector_state(id,revision,checkpoint) VALUES(1,?,?)",
                arrayOf<Any>(revision, document.getJSONObject("checkpoint").toString()),
            )
            db.setTransactionSuccessful()
            return revision
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            try {
                db.endTransaction()
            } catch (cleanup: Throwable) {
                // SQLITE_FULL can already have rolled SQLite back. Preserve the original failure.
                if (failure != null) failure.addSuppressed(cleanup) else throw cleanup
            }
        }
    }

    override fun close() = helper.close()
}
