package com.oxywatt.diagnostic.persistence

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
class InputRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val names = mutableListOf<String>()

    private fun databaseName() = "repository-${UUID.randomUUID()}.db".also(names::add)

    @After
    fun cleanFiles() {
        names.forEach { context.deleteDatabase(it) }
    }

    private fun collector(epochId: UUID = epoch, recovered: CollectorCheckpoint? = null) =
        CollectorLifecycle(epochId, LifecyclePolicy("test-v1", 100), recovered = recovered)

    private fun batch(vararg inputs: SourceObservation): AdmissionBatch =
        BoundedAdmission().let { admission ->
            inputs.forEach { check(admission.offer(it)) }
            admission.takeBatch()!!
        }

    private fun state(n: Long, state: RecordingState) = input(n, InputPayload.Recording(state))

    private fun power(n: Long) = input(n, InputPayload.MetricReading(Metric.POWER_W, 0.0))

    private val epoch = UUID(0, 44)

    private fun input(n: Long, payload: InputPayload) =
        SourceObservation(
            ReceiptStamp(Instant.EPOCH.plusNanos(n), epoch, n, n),
            SourceRef("synthetic", "v1"),
            "v1",
            payload,
        )

    @Test
    fun `committed active checkpoint survives closing and reopening the file`() {
        val name = databaseName()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test-v1", 100))
        val admission = BoundedAdmission()
        admission.offer(input(0, InputPayload.Recording(RecordingState.RECORDING)))
        InputRepository(context, name).use {
            it.commit(it.prepare(admission.takeBatch()!!, lifecycle, 0))
        }
        InputRepository(context, name).use {
            assertEquals(SessionPhase.RECORDING, it.checkpoint()?.latestSession?.phase)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `inputs effects and checkpoint roll back together and same frozen write retries`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = databaseName()
        val lifecycle = CollectorLifecycle(epoch, LifecyclePolicy("test-v1", 100))
        val admission = BoundedAdmission()
        val inputs =
            listOf(
                input(0, InputPayload.Recording(RecordingState.IDLE)),
                input(1, InputPayload.Recording(RecordingState.RECORDING)),
                input(2, InputPayload.MetricReading(Metric.POWER_W, 0.0)),
                input(3, InputPayload.MetricReading(Metric.POWER_W, 0.0)),
            )
        inputs.forEach { assertTrue(admission.offer(it)) }
        val batch = admission.takeBatch()!!
        lateinit var write: PreparedInputWrite
        InputRepository(context, name).use { repo ->
            write = repo.prepare(batch, lifecycle, 0)
            repo.beforeCheckpoint = {
                throw IllegalStateException("Injected transaction interruption")
            }
            assertThrows(IllegalStateException::class.java) { repo.commit(write) }
            assertEquals(0L, repo.state().revision)
            assertTrue(repo.observations().isEmpty())
            assertTrue(repo.effects(write.batchId).isEmpty())
            assertEquals(4, admission.pendingCount)
        }
        InputRepository(context, name).use { repo ->
            assertEquals(1L, repo.commit(write))
            assertEquals(1L, repo.commit(write))
            assertEquals(inputs, repo.observations().map { it.observation })
            assertEquals(4, repo.effects(write.batchId).size)
            assertEquals(lifecycle.checkpoint(), repo.checkpoint())
            assertEquals(
                lifecycle.checkpoint().latestSession,
                repo.session(lifecycle.checkpoint().latestSession!!.id),
            )
            admission.committed(batch)
            assertEquals(0, admission.pendingCount)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `all canonical fields and bounded replay pages round trip exactly`() {
        val values =
            listOf(
                state(0, RecordingState.IDLE),
                input(
                    1,
                    InputPayload.Profile(
                        RiderProfileSnapshot(
                            "profile-α",
                            Instant.EPOCH.minusNanos(1),
                            ProfileSetting(70.25, SettingOrigin.MANUAL),
                            ProfileSetting(190.0, SettingOrigin.CONFIGURED),
                            ProfileSetting(251.125, SettingOrigin.CONFIGURED),
                        )
                    ),
                ),
                state(2, RecordingState.RECORDING),
                input(
                        3,
                        InputPayload.MetricReading(
                            Metric.CADENCE_RPM,
                            -0.0,
                            sourceTime =
                                SourceTime(
                                    Instant.EPOCH.plusNanos(987654321),
                                    SourceTimeMeaning.FILE_RECORD,
                                ),
                        ),
                    )
                    .copy(source = SourceRef("file", "version-β", Long.MAX_VALUE)),
                input(
                    4,
                    InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.DROPOUT),
                ),
                state(5, RecordingState.PAUSED),
                power(6),
                state(7, RecordingState.RECORDING),
                state(8, RecordingState.IDLE),
            )
        // Preserve source identity throughout the session; file time remains separate from receipt
        // time.
        val inputs = values.map { it.copy(source = SourceRef("file", "version-β", Long.MAX_VALUE)) }
        val lifecycle = collector()
        InputRepository(context, databaseName()).use { repo ->
            val write = repo.prepare(batch(*inputs.toTypedArray()), lifecycle, 0)
            repo.commit(write)
            val a = repo.observations(limit = 4)
            val b = repo.observations(a.last().position, 4)
            val c = repo.observations(b.last().position, 4)
            assertEquals(inputs, (a + b + c).map { it.observation })
            assertNull(a.first().sessionId)
            assertNull(b[2].segment)
            assertEquals(lifecycle.checkpoint(), repo.checkpoint())
            assertEquals(9, repo.effects(write.batchId).size)
            assertEquals(AdmissionPolicy(), repo.batchMetadata(write.batchId)!!.policy)
            assertThrows(IllegalArgumentException::class.java) { repo.observations(limit = 257) }
        }
    }

    @Test
    fun `stale writers conflicting retry IDs and duplicate receipt keys cannot change checkpoint`() {
        val name = databaseName()
        InputRepository(context, name).use { repo ->
            val id = UUID.randomUUID()
            val first = repo.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 0, id)
            val conflicting =
                repo.prepare(batch(state(1, RecordingState.PAUSED)), collector(), 0, id)
            val stale = repo.prepare(batch(state(2, RecordingState.RECORDING)), collector(), 0)
            repo.commit(first)
            val saved = repo.state()
            assertThrows(IllegalStateException::class.java) { repo.commit(conflicting) }
            assertThrows(IllegalStateException::class.java) { repo.commit(stale) }
            assertThrows(IllegalStateException::class.java) {
                repo.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 1)
            }
            assertEquals(saved, repo.state())
            assertEquals(1, repo.observations().size)
            // A known committed ID is checked before batch.applyTo can mutate lifecycle.
            val untouched = collector()
            assertThrows(IllegalStateException::class.java) {
                repo.prepare(batch(power(10)), untouched, 1, id)
            }
            assertNull(untouched.checkpoint().lastReceipt)
        }
    }

    @Test
    fun `restart interrupts prior session atomically and retains original profile provenance`() {
        val name = databaseName()
        lateinit var saved: CollectorCheckpoint
        InputRepository(context, name).use { repo ->
            val lifecycle = collector()
            repo.commit(
                repo.prepare(
                    batch(
                        input(0, InputPayload.Profile(RiderProfileSnapshot("profile"))),
                        state(1, RecordingState.RECORDING),
                        power(2),
                    ),
                    lifecycle,
                    0,
                )
            )
            saved = repo.checkpoint()!!
        }
        val nextEpoch = UUID(0, 45)
        val next =
            state(0, RecordingState.PAUSED)
                .copy(stamp = ReceiptStamp(Instant.EPOCH.minusSeconds(50), nextEpoch, 0, 0))
        InputRepository(context, name).use { repo ->
            val lifecycle = collector(nextEpoch, saved)
            val write = repo.prepare(batch(next), lifecycle, 1)
            repo.commit(write)
            val prior = repo.session(saved.latestSession!!.id)!!
            val fragment = repo.checkpoint()!!.latestSession!!
            assertEquals(SessionPhase.INTERRUPTED, prior.phase)
            assertEquals(InterruptionReason.PROCESS_RESTART, prior.interruptionReason)
            assertEquals(SessionOrigin.RECOVERY_FRAGMENT, fragment.origin)
            assertNotEquals(prior.id, fragment.id)
            assertEquals(prior.ambiguityGroupId, fragment.ambiguityGroupId)
            assertEquals(saved.profileObservation, repo.checkpoint()!!.profileObservation)
            assertFalse(fragment.trendEligible)
            assertEquals(2, repo.effects(write.batchId).single().sessionChanges.size)
        }
    }

    @Test
    fun `loss only batch persists interruption and failed storage retains admission budget`() {
        val admission = BoundedAdmission(AdmissionPolicy("tiny-v1", 4, 2000, 1, 200, 1024, 5))
        admission.offer(state(0, RecordingState.RECORDING))
        repeat(3) { admission.offer(power(it + 1L)) }
        val retained = admission.takeBatch()!!
        repeat(1000) { assertFalse(admission.offer(power(it + 4L))) }
        InputRepository(context, databaseName()).use { repo ->
            val lifecycle = collector()
            repo.commit(repo.prepare(retained, lifecycle, 0))
            admission.committed(retained)
            val loss = admission.takeBatch()!!
            assertTrue(loss.observations.isEmpty())
            val write = repo.prepare(loss, lifecycle, 1)
            repo.beforeCheckpoint = {
                throw android.database.sqlite.SQLiteFullException("Injected storage error")
            }
            assertThrows(android.database.sqlite.SQLiteFullException::class.java) {
                repo.commit(write)
            }
            assertEquals(1L, repo.state().revision)
            repo.beforeCheckpoint = {}
            repo.commit(write)
            assertEquals(loss.loss, repo.batchMetadata(write.batchId)!!.loss)
            assertEquals(SessionPhase.INTERRUPTED, repo.checkpoint()!!.latestSession!!.phase)
            assertEquals(4, repo.observations().size)
            assertNull(repo.effects(write.batchId).single().envelope)
            admission.committed(loss)
        }
    }

    @Test
    fun `native SQLite full error rolls back the whole batch and exact retry succeeds`() {
        val name = databaseName()
        val admission = BoundedAdmission()
        admission.offer(state(0, RecordingState.RECORDING))
        repeat(500) { admission.offer(power(it + 1L)) }
        val batch = admission.takeBatch()!!
        InputRepository(context, name).use { repo ->
            val write = repo.prepare(batch, collector(), 0)
            repo.limitStorageForTest(65536)
            assertThrows(android.database.sqlite.SQLiteFullException::class.java) {
                repo.commit(write)
            }
            assertEquals(0L, repo.state().revision)
            assertNull(repo.checkpoint())
            assertTrue(repo.observations().isEmpty())
            assertTrue(repo.effects(write.batchId).isEmpty())
            assertEquals(501, admission.pendingCount)
            repo.limitStorageForTest(64L * 1024 * 1024)
            assertEquals(1L, repo.commit(write))
            assertEquals(256, repo.observations().size)
            admission.committed(batch)
            assertEquals(0, admission.pendingCount)
        }
        InputRepository(context, name).use { assertEquals(1L, it.state().revision) }
    }

    @Test
    fun `timer and terminal effects commit without manufacturing input rows`() {
        InputRepository(context, databaseName()).use { repo ->
            val lifecycle = collector()
            repo.commit(
                repo.prepare(batch(state(0, RecordingState.RECORDING), power(1)), lifecycle, 0)
            )
            val silence = lifecycle.advanceTime(101)
            val timer = repo.prepareEffect(silence, AdmissionPolicy(), 1)
            repo.commit(timer)
            assertEquals(
                listOf(
                    ChannelCoverageBreak(
                        lifecycle.checkpoint().latestSession!!.id,
                        Metric.POWER_W,
                        CoverageBreakReason.SILENCE,
                        101,
                    )
                ),
                repo.effects(timer.batchId).single().coverageBreaks,
            )
            val stopped = repo.prepareEffect(lifecycle.sourceStopped(), AdmissionPolicy(), 2)
            repo.commit(stopped)
            assertEquals(2, repo.observations().size)
            assertEquals(SessionPhase.INTERRUPTED, repo.checkpoint()!!.latestSession!!.phase)
            assertEquals(1L, repo.committedRevision(timer.batchId)?.minus(1))
            assertEquals(2L, repo.commit(timer))
            assertEquals(3L, repo.state().revision)
        }
    }

    @Test
    fun `reused historical UUID in another epoch rolls back instead of merging sessions`() {
        val id = UUID(0, 500)
        InputRepository(context, databaseName()).use { repo ->
            val first = CollectorLifecycle(epoch, LifecyclePolicy("test-v1", 100), { id })
            repo.commit(repo.prepare(batch(state(0, RecordingState.RECORDING)), first, 0))
            val otherEpoch = UUID(0, 100)
            val other = CollectorLifecycle(otherEpoch, LifecyclePolicy("test-v1", 100), { id })
            val input =
                state(0, RecordingState.RECORDING)
                    .copy(stamp = ReceiptStamp(Instant.EPOCH, otherEpoch, 0, 0))
            val write = repo.prepare(batch(input), other, 1)
            assertThrows(IllegalStateException::class.java) { repo.commit(write) }
            assertEquals(first.checkpoint(), repo.checkpoint())
            assertEquals(1, repo.observations().size)
        }
    }

    @Test
    fun `future database schema and corrupt checkpoint fail without destructive fallback`() {
        val name = databaseName()
        InputRepository(context, name).use { repo ->
            repo.commit(repo.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 0))
        }
        val path = context.getDatabasePath(name).path
        android.database.sqlite.SQLiteDatabase.openDatabase(
                path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            )
            .use { it.version = 2 }
        assertThrows(android.database.sqlite.SQLiteException::class.java) {
            InputRepository(context, name).use { it.state() }
        }
        android.database.sqlite.SQLiteDatabase.openDatabase(
                path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            )
            .use { db ->
                db.version = 1
                db.rawQuery("SELECT COUNT(*) FROM observations", null).use {
                    it.moveToFirst()
                    assertEquals(1, it.getInt(0))
                }
                db.execSQL(
                    "UPDATE collector_state SET checkpoint=replace(checkpoint,'collector-checkpoint-v1','collector-checkpoint-v9')"
                )
            }
        assertThrows(IllegalArgumentException::class.java) {
            InputRepository(context, name).use { it.state() }
        }
    }

    @Test
    fun `committed receipt replay is rejected before consuming the new lifecycle`() {
        InputRepository(context, databaseName()).use { repo ->
            repo.commit(repo.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 0))
            val untouched = collector()
            assertThrows(IllegalStateException::class.java) {
                repo.prepare(batch(state(0, RecordingState.RECORDING)), untouched, 1)
            }
            assertNull(untouched.checkpoint().lastReceipt)
        }
    }

    @Test
    fun `new epoch without conservative recovery cannot abandon a durable active session`() {
        InputRepository(context, databaseName()).use { repo ->
            repo.commit(repo.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 0))
            val saved = repo.state()
            val nextEpoch = UUID(0, 999)
            val next =
                state(0, RecordingState.RECORDING)
                    .copy(stamp = ReceiptStamp(Instant.EPOCH, nextEpoch, 0, 0))
            val write = repo.prepare(batch(next), collector(nextEpoch), 1)
            assertThrows(IllegalStateException::class.java) { repo.commit(write) }
            assertEquals(saved, repo.state())
        }
    }

    @Test
    fun `independent writers cannot both commit the same expected revision`() {
        val name = databaseName()
        InputRepository(context, name).use { first ->
            InputRepository(context, name).use { second ->
                val a = first.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 0)
                val b = second.prepare(batch(state(1, RecordingState.RECORDING)), collector(), 0)
                val start = java.util.concurrent.CountDownLatch(1)
                val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
                try {
                    val outcomes =
                        listOf(first to a, second to b).map { (repo, write) ->
                            pool.submit<Boolean> {
                                check(start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                try {
                                    repo.commit(write)
                                    true
                                } catch (_: IllegalStateException) {
                                    false
                                }
                            }
                        }
                    start.countDown()
                    assertEquals(
                        listOf(false, true),
                        outcomes.map { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }.sorted(),
                    )
                    assertEquals(1L, first.state().revision)
                    assertEquals(first.state(), second.state())
                    assertEquals(1, first.observations().size)
                } finally {
                    pool.shutdownNow()
                }
            }
        }
    }

    @Test
    fun `control overflow commits terminal status and cannot turn retained Idle into completion`() {
        val admission = BoundedAdmission(AdmissionPolicy("tiny-v1", 4, 2000, 1, 200, 1024, 5))
        admission.offer(state(0, RecordingState.RECORDING))
        repeat(3) { admission.offer(power(it + 1L)) }
        assertFalse(admission.offer(state(4, RecordingState.IDLE)))
        assertTrue(admission.stopped)
        InputRepository(context, databaseName()).use { repo ->
            val retained = admission.takeBatch()!!
            val write = repo.prepare(retained, collector(), 0)
            repo.commit(write)
            assertEquals(
                InterruptionReason.CONTROL_OVERFLOW,
                repo.batchMetadata(write.batchId)!!.terminal,
            )
            assertEquals(SessionPhase.INTERRUPTED, repo.checkpoint()!!.latestSession!!.phase)
            assertFalse(repo.checkpoint()!!.latestSession!!.trendEligible)
            val id = repo.checkpoint()!!.latestSession!!.id
            assertEquals(4, repo.observations(sessionId = id).size)
            admission.committed(retained)
            assertTrue(admission.stopped)
        }
    }

    @Test
    fun `SQLite receipt uniqueness rejects an actual conflicting row`() {
        val name = databaseName()
        InputRepository(context, name).use {
            it.commit(it.prepare(batch(state(0, RecordingState.RECORDING)), collector(), 0))
        }
        android.database.sqlite.SQLiteDatabase.openDatabase(
                context.getDatabasePath(name).path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            )
            .use { db ->
                assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
                    db.execSQL(
                        "INSERT INTO observations(epoch,sequence,batch,session,segment,data) SELECT epoch,sequence,batch,session,segment,data FROM observations"
                    )
                }
            }
        InputRepository(context, name).use {
            assertEquals(1, it.observations().size)
            assertEquals(1L, it.state().revision)
        }
    }

    @Test
    fun `missing reasons and long receipt keys survive persisted replay without float coercion`() {
        val keys = listOf(9007199254740993L, 9007199254740994L, Long.MAX_VALUE)
        val inputs =
            MissingReason.entries.zip(keys).map { (reason, key) ->
                input(key, InputPayload.MetricReading(Metric.POWER_W, null, reason))
            }
        InputRepository(context, databaseName()).use { repo ->
            repo.commit(repo.prepare(batch(*inputs.toTypedArray()), collector(), 0))
            assertEquals(inputs, repo.observations().map { it.observation })
            assertEquals(Long.MAX_VALUE, repo.checkpoint()!!.lastReceipt!!.ingestSequence)
            assertTrue(repo.observations().all { it.sessionId == null })
        }
    }
}
