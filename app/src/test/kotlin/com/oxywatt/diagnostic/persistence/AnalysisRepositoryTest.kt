package com.oxywatt.diagnostic.persistence

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class AnalysisRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val name = "analysis-${UUID.randomUUID()}.db"
    private val provenance =
        AnalysisProvenance(
            "inputs",
            "synthetic-v1",
            "profile",
            "stub-v1",
            "prep-v1",
            "params",
            "rider",
            "trend-v1",
            Instant.EPOCH,
            "code-v1",
        )
    private val available =
        AnalysisTerminal(
            AnalysisStatus.AVAILABLE,
            Instant.EPOCH,
            123.0,
            "synthetic-unit",
            "synthetic",
        )

    @After
    fun cleanup() {
        context.deleteDatabase(name)
    }

    private fun session(): AnalysisSlot {
        val epoch = UUID.randomUUID()
        InputRepository(context, name).use { repo ->
            val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test", 100))
            val admission = BoundedAdmission()
            listOf(RecordingState.IDLE, RecordingState.RECORDING, RecordingState.IDLE)
                .forEachIndexed { i, state ->
                    admission.offer(
                        SourceObservation(
                            ReceiptStamp(Instant.EPOCH, epoch, i.toLong(), i.toLong()),
                            SourceRef("synthetic", "v1"),
                            "v1",
                            InputPayload.Recording(state),
                        )
                    )
                }
            repo.commit(repo.prepare(admission.takeBatch()!!, lifecycle, repo.state().revision))
            return AnalysisSlot(repo.checkpoint()!!.latestSession!!.id, "synthetic", "cohort-v1")
        }
    }

    @Test
    fun `newest pending failed and unavailable never fall back after reopen`() {
        val slot = session()
        AnalysisRepository(context, name).use { repo ->
            val a = repo.request(slot, UUID.randomUUID(), provenance)
            repo.complete(a, available)
            repo.replaceMembership("trend", setOf(a))
            val b = repo.request(slot, UUID.randomUUID(), provenance)
            assertTrue(b > a)
            assertNull(repo.current(slot))
            assertTrue(repo.membership("trend").isEmpty())
            repo.complete(
                b,
                AnalysisTerminal(
                    AnalysisStatus.UNAVAILABLE,
                    Instant.EPOCH,
                    reason = "synthetic rejection",
                ),
            )
            assertNull(repo.current(slot))
            val c = repo.request(slot, UUID.randomUUID(), provenance)
            repo.complete(
                c,
                AnalysisTerminal(
                    AnalysisStatus.FAILED,
                    Instant.EPOCH,
                    reason = "storage-independent failure",
                ),
            )
        }
        AnalysisRepository(context, name).use {
            assertNull(it.current(slot))
            assertEquals(AnalysisStatus.FAILED, it.newest(slot)!!.status)
        }
    }

    @Test
    fun `reverse completions preserve newest pointer and immutable audit retries`() {
        val slot = session()
        val request = UUID.randomUUID()
        AnalysisRepository(context, name).use { repo ->
            val a = repo.request(slot, request, provenance)
            val b = repo.request(slot, UUID.randomUUID(), provenance)
            repo.complete(b, available)
            repo.replaceMembership("trend", setOf(b))
            repo.complete(a, available.copy(value = 42.0))
            assertEquals(b, repo.current(slot)!!.generation)
            assertEquals(setOf(b), repo.membership("trend"))
            assertEquals(a, repo.request(slot, request, provenance))
            repo.complete(a, available.copy(value = 42.0))
            assertThrows(IllegalStateException::class.java) { repo.complete(a, available) }
            assertThrows(IllegalStateException::class.java) {
                repo.request(slot, request, provenance.copy(inputHash = "changed"))
            }
            assertEquals(42.0, repo.run(a)!!.terminal!!.value!!, 0.0)
        }
    }

    @Test
    fun `request and completion rollback preserve pointer result and whole aggregate`() {
        val slot = session()
        val other = session()
        AnalysisRepository(context, name).use { repo ->
            val a = repo.request(slot, UUID.randomUUID(), provenance)
            repo.complete(a, available)
            val b = repo.request(other, UUID.randomUUID(), provenance)
            repo.complete(b, available)
            repo.replaceMembership("trend", setOf(a, b))
            repo.beforeCommit = { error("fault") }
            val id = UUID.randomUUID()
            assertThrows(IllegalStateException::class.java) { repo.request(slot, id, provenance) }
            assertEquals(a, repo.current(slot)!!.generation)
            assertEquals(setOf(a, b), repo.membership("trend"))
            repo.beforeCommit = {}
            val c = repo.request(slot, id, provenance)
            assertTrue(repo.membership("trend").isEmpty())
            repo.beforeCommit = { error("fault") }
            assertThrows(IllegalStateException::class.java) { repo.complete(c, available) }
            assertEquals(AnalysisStatus.PENDING, repo.newest(slot)!!.status)
        }
        AnalysisRepository(context, name).use { repo ->
            val c = repo.newest(slot)!!.generation
            repo.complete(c, available)
            assertEquals(c, repo.current(slot)!!.generation)
            assertThrows(IllegalStateException::class.java) {
                repo.replaceMembership("bad", setOf(c, repo.run(1)!!.generation))
            }
        }
    }

    @Test
    fun `real v1 migration preserves every input table and allows durable results`() {
        val slot = session()
        val legacyTables =
            listOf("batches", "sessions", "observations", "effects", "collector_state")
        val path = context.getDatabasePath(name)
        val fixtureName = "$name-v1"
        val fixturePath = context.getDatabasePath(fixtureName)
        fun rows(db: SQLiteDatabase, table: String): List<List<String?>> =
            db.rawQuery("SELECT * FROM $table ORDER BY rowid", null).use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        (0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }
                    )
                }
            }
        val expected =
            SQLiteDatabase.openOrCreateDatabase(path, null).use { source ->
                legacyTables
                    .associateWith { rows(source, it) }
                    .also {
                        SQLiteDatabase.openOrCreateDatabase(fixturePath, null).use { fixture ->
                            V1_SCHEMA.forEach(fixture::execSQL)
                            fixture.execSQL("ATTACH DATABASE ? AS original", arrayOf(path.path))
                            legacyTables.forEach { table ->
                                fixture.execSQL("INSERT INTO $table SELECT * FROM original.$table")
                            }
                            fixture.execSQL("DETACH DATABASE original")
                            fixture.version = 1
                        }
                    }
            }
        context.deleteDatabase(name)
        check(fixturePath.renameTo(path))
        InputRepository(context, name).use {
            assertEquals(3, it.observations().size)
            assertEquals(1L, it.state().revision)
            assertEquals(slot.session, it.checkpoint()!!.latestSession!!.id)
        }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            assertEquals(2, db.version)
            legacyTables.forEach { assertEquals(expected[it], rows(db, it)) }
        }
        AnalysisRepository(context, name).use {
            val g = it.request(slot, UUID.randomUUID(), provenance)
            it.complete(g, available)
        }
        AnalysisRepository(context, name).use {
            assertEquals(available, it.current(slot)!!.terminal)
        }
    }

    @Test
    fun `independent analysis writers allocate unique durable increasing generations`() {
        val slot = session()
        AnalysisRepository(context, name).use { first ->
            AnalysisRepository(context, name).use { second ->
                val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
                val start = java.util.concurrent.CountDownLatch(1)
                try {
                    val writes =
                        listOf(first, second).map { repo ->
                            pool.submit<Long> {
                                check(start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                repo.request(slot, UUID.randomUUID(), provenance)
                            }
                        }
                    start.countDown()
                    val generations =
                        writes.map { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }.sorted()
                    assertEquals(2, generations.distinct().size)
                    assertEquals(generations.last(), first.newest(slot)!!.generation)
                    assertEquals(first.newest(slot), second.newest(slot))
                } finally {
                    pool.shutdownNow()
                }
            }
        }
    }

    @Test
    fun `provenance explicitly identifies code version independently of model`() {
        val slot = session()
        AnalysisRepository(context, name).use { repo ->
            val generation = repo.request(slot, UUID.randomUUID(), provenance)
            val stored = org.json.JSONObject(repo.run(generation)!!.provenance)
            assertEquals("code-v1", stored.optString("codeVersion"))
        }
    }

    @Test
    fun `slots stay isolated and SQL foreign keys reject unknown sessions`() {
        val slot = session()
        AnalysisRepository(context, name).use { repo ->
            val a = repo.request(slot, UUID.randomUUID(), provenance)
            repo.complete(a, available)
            listOf(slot.copy(metric = "other"), slot.copy(cohort = "other")).forEach { other ->
                val b = repo.request(other, UUID.randomUUID(), provenance)
                assertNull(repo.current(other))
                assertEquals(a, repo.current(slot)!!.generation)
                repo.complete(b, available)
                assertThrows(IllegalStateException::class.java) {
                    repo.replaceMembership("mixed", setOf(a, b))
                }
            }
            assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
                repo.request(slot.copy(session = UUID.randomUUID()), UUID.randomUUID(), provenance)
            }
            assertEquals(a, repo.current(slot)!!.generation)
        }
    }

    companion object {
        // Frozen production v1 DDL, independent of the v2 helper.
        private val V1_SCHEMA =
            listOf(
                "CREATE TABLE batches (id TEXT PRIMARY KEY NOT NULL, digest TEXT NOT NULL, revision INTEGER NOT NULL UNIQUE, epoch TEXT NOT NULL, document TEXT NOT NULL)",
                "CREATE TABLE sessions (id TEXT PRIMARY KEY NOT NULL, snapshot TEXT NOT NULL)",
                "CREATE TABLE observations (position INTEGER PRIMARY KEY, epoch TEXT NOT NULL, sequence INTEGER NOT NULL, batch TEXT NOT NULL REFERENCES batches(id), session TEXT REFERENCES sessions(id), segment INTEGER, data TEXT NOT NULL, UNIQUE(epoch,sequence))",
                "CREATE INDEX observations_session ON observations(session,position)",
                "CREATE TABLE effects (batch TEXT NOT NULL REFERENCES batches(id), item INTEGER NOT NULL, data TEXT NOT NULL, PRIMARY KEY(batch,item))",
                "CREATE TABLE collector_state (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL, checkpoint TEXT NOT NULL)",
            )
    }
}
