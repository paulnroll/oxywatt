package com.oxywatt.diagnostic.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.*
import java.time.Instant
import java.util.UUID
import org.json.JSONObject
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
class LivePowerCurveTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val names = mutableListOf<String>()
    private val workerInputs = mutableListOf<InputRepository>()
    private val epoch = UUID(0, 201)
    private val id = UUID(0, 202)

    private fun name() = "live-curve-${UUID.randomUUID()}.db".also(names::add)

    @After
    fun clean() {
        workerInputs.forEach { it.close() }
        names.forEach { context.deleteDatabase(it) }
    }

    private fun metadata(receiptEpoch: UUID = epoch) =
        JSONObject()
            .put("version", 1)
            .put("origin", "real-karoo-diagnostic")
            .put("appVersion", "fixture-v1")
            .put("codeRevision", "test-only")
            .put("dirty", true)
            .put("epoch", receiptEpoch.toString())
            .put("ordering", "karoo-diagnostic-order-v1")
            .put("retry", "karoo-diagnostic-retry-v1")
            .put("firstRetryNanos", 1_000_000_000L)
            .put("subsequentRetryNanos", 5_000_000_000L)
            .put("storage", "karoo-diagnostic-storage-v1")
            .put("maximumBytes", 32L * 1024 * 1024)
            .put("silence", "karoo-diagnostic-silence-v1")
            .put("silenceNanos", 5_000_000_000L)
            .toString()

    private fun fill(
        repo: InputRepository,
        missing: Boolean = false,
        seconds: Long = 6,
    ): CollectorLifecycle {
        val l =
            CollectorLifecycle(
                epoch,
                LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
                { id },
            )
        val q = BoundedAdmission()
        var seq = 0L
        fun put(t: Long, p: InputPayload) {
            q.offer(
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, epoch, t * 1_000_000_000, seq++),
                    SourceRef("karoo", "1.1.9"),
                    "karoo-normalization-v1",
                    p,
                )
            )
        }
        put(0, InputPayload.Recording(RecordingState.IDLE))
        put(0, InputPayload.Recording(RecordingState.RECORDING))
        for (t in 0L until seconds) put(
            t,
            InputPayload.MetricReading(
                Metric.POWER_W,
                if (missing) null else 200.0,
                if (missing) MissingReason.UNAVAILABLE else null,
            ),
        )
        put(seconds, InputPayload.Recording(RecordingState.IDLE))
        val batch = q.takeBatch()!!
        repo.commit(repo.prepare(batch, l, repo.state().revision))
        q.committed(batch)
        return l
    }

    private fun rides(repo: InputRepository, count: Int, baseWatts: Double = 100.0): List<UUID> {
        var number = 202L
        val lifecycle =
            CollectorLifecycle(
                epoch,
                LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
                { UUID(0, number++) },
            )
        val queue = BoundedAdmission()
        var sequence = 0L
        repeat(count) { ride ->
            val base = ride * 10L
            fun put(t: Long, payload: InputPayload) {
                queue.offer(
                    SourceObservation(
                        ReceiptStamp(
                            Instant.EPOCH.plusSeconds(t),
                            epoch,
                            t * 1_000_000_000,
                            sequence++,
                        ),
                        SourceRef("karoo", "1.1.9"),
                        "karoo-normalization-v1",
                        payload,
                    )
                )
            }
            put(base, InputPayload.Recording(RecordingState.IDLE))
            put(base, InputPayload.Recording(RecordingState.RECORDING))
            repeat(6) {
                put(base + it, InputPayload.MetricReading(Metric.POWER_W, baseWatts + ride * 50))
            }
            put(base + 6, InputPayload.Recording(RecordingState.IDLE))
            val batch = queue.takeBatch()!!
            repo.commit(repo.prepare(batch, lifecycle, repo.state().revision))
            queue.committed(batch)
        }
        return (0 until count).map { UUID(0, 202L + it) }
    }

    @Test
    fun `superseding selection discards an unstarted calculation instead of retargeting its tap`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val ids = rides(inputs, 2)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, owner(n), "test", { now })
                c.select(ids.first())
                repeat(30) { c.pump() }
                c.request()
                c.select(ids.last())
                repeat(60) { c.pump() }
                ids.forEach {
                    assertNull(
                        "Selection must not redirect a previous tap",
                        results.newest(PowerCurveCalculation.realSlot(it)),
                    )
                }
            }
        }
    }

    @Test
    fun `older selected result is immediately visible after completion and latest checkpoint is unchanged`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val ids = rides(inputs, 2)
            val current = owner(n)
            val checkpoint = inputs.checkpoint()
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(ids.first())
                repeat(30) { c.pump() }
                c.request()
                var done = false
                repeat(100) {
                    if (!done) {
                        c.pump()
                        if (
                            results.newest(PowerCurveCalculation.realSlot(ids.first()))?.terminal !=
                                null
                        ) {
                            done = true
                            assertEquals(LiveCurveStatus.SAVED, c.snapshot().status)
                            assertEquals(ids.first(), c.snapshot().saved!!.slot.session)
                            assertEquals(
                                100.0,
                                c.snapshot()
                                    .saved!!
                                    .terminal!!
                                    .curve!!
                                    .snapshot
                                    .durations
                                    .first()
                                    .record!!
                                    .meanWatts,
                                1e-8,
                            )
                        }
                    }
                }
                assertTrue(done)
                assertNull(results.newest(PowerCurveCalculation.realSlot(ids.last())))
                assertEquals(checkpoint, inputs.checkpoint())
            }
        }
    }

    @Test
    fun `bounded recent list reports truncation and unknown coverage without scanning into an analysis`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            assertTrue(inputs.recentSessions().sessions.isEmpty())
            val ids = rides(inputs, 22)
            val page = inputs.recentSessions()
            assertTrue(page.truncated)
            assertEquals(ids.reversed().take(20), page.sessions.map { it.id })
            AnalysisRepository(context, n).use { results ->
                val current = owner(n)
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.pump()
                assertEquals(20, c.sessions().cards.size)
                assertTrue(c.sessions().cards.all { it.loading })
                c.pump()
                assertEquals(1, c.sessions().cards.count { !it.loading })
                val initialCommits = current.snapshot().committed
                repeat(25) { step ->
                    now += 1_000_000_000
                    offer(current, InputPayload.MetricReading(Metric.POWER_W, 999.0))
                    current.pump(true)
                    c.pump()
                    assertEquals(initialCommits + step + 1, current.snapshot().committed)
                    assertEquals(0, current.snapshot().pending)
                }
                assertFalse(c.sessions().loading)
                assertTrue(
                    c.sessions().cards.all { it.status == null && it.availableDurations == null }
                )
                assertTrue(
                    sessionCardText(context, c.sessions().cards.first())
                        .contains("Power coverage unknown")
                )
                assertFalse(c.snapshot().canCalculate)
                ids.forEach { assertNull(results.newest(PowerCurveCalculation.realSlot(it))) }
            }
        }
    }

    @Test
    fun `refresh can truncate the explicit selection without replacing or recalculating it`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(id)
                repeat(30) { c.pump() }
                repeat(22) {
                    now += 1_000_000_000
                    offer(current, InputPayload.Recording(RecordingState.RECORDING))
                    current.pump(true)
                    now += 1_000_000_000
                    offer(current, InputPayload.Recording(RecordingState.IDLE))
                    current.pump(true)
                }
                assertTrue(c.sessions().dirty)
                c.refreshSessions()
                repeat(30) { c.pump() }
                assertTrue(c.sessions().truncated)
                assertTrue(c.sessions().cards.none { it.session.id == id })
                assertEquals(id, c.snapshot().session!!.id)
                assertNull(c.snapshot().saved)
                assertTrue(c.snapshot().canCalculate)
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
            }
        }
    }

    @Test
    fun `older selected target waits for received control acknowledgement but not for ordinary telemetry`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(id)
                repeat(30) { c.pump() }
                offer(current, InputPayload.Recording(RecordingState.RECORDING))
                offer(current, InputPayload.Recording(RecordingState.IDLE))
                c.request()
                c.pump()
                assertFalse(c.snapshot().canCalculate)
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                current.pump(true)
                c.pump()
                offer(current, InputPayload.MetricReading(Metric.POWER_W, 999.0))
                assertEquals(1, current.snapshot().pending)
                c.request()
                repeat(60) {
                    current.pump()
                    c.pump()
                }
                assertEquals(id, c.snapshot().saved!!.slot.session)
                assertEquals(
                    200.0,
                    c.snapshot()
                        .saved!!
                        .terminal!!
                        .curve!!
                        .snapshot
                        .durations
                        .first()
                        .record!!
                        .meanWatts,
                    1e-8,
                )
                assertEquals(1, current.snapshot().pending)
            }
        }
    }

    @Test
    fun `selection and refresh cannot redirect an uncertain analysis request or completion`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val ids = rides(inputs, 2)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(ids.first())
                repeat(30) { c.pump() }
                var lost = false
                results.afterCommit = {
                    if (!lost) {
                        lost = true
                        throw android.database.sqlite.SQLiteDiskIOException()
                    }
                }
                c.request()
                repeat(60) {
                    now += 1_000_000_000
                    current.pump()
                    c.pump()
                    if (!c.sessions().canChange) {
                        c.select(ids.last())
                        c.refreshSessions()
                        c.request()
                    }
                }
                assertTrue(lost)
                assertEquals(ids.first(), c.snapshot().session!!.id)
                assertEquals(1L, c.snapshot().saved!!.generation)
                assertNull(results.newest(PowerCurveCalculation.realSlot(ids.last())))
            }
        }
    }

    @Test
    fun `missing only saved card reports zero covered durations and selection does not recalculate`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs, true)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val completion = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                results.complete(completion.generation, completion.terminal)
                val c = LiveCurveController(inputs, results, current, "test", { now })
                repeat(30) { c.pump() }
                assertEquals(0, c.sessions().cards.single().availableDurations)
                c.select(id)
                c.pump()
                assertEquals(completion.generation, c.snapshot().saved!!.generation)
                assertTrue(
                    c.snapshot().saved!!.terminal!!.curve!!.snapshot.durations.all {
                        it.record == null
                    }
                )
                assertTrue(liveCurveText(context, c.snapshot()).contains("Unavailable"))
            }
        }
    }

    @Test
    fun `a mismatched saved slot is an explicit read error and cannot display another session curve`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val ids = rides(inputs, 2)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val runs = ids.map { id ->
                    PowerCurveCalculation(inputs, results).prepareReal(id, "test").also {
                        results.complete(it.generation, it.terminal)
                    }
                }
                android.database.sqlite.SQLiteDatabase.openDatabase(
                        context.getDatabasePath(n).path,
                        null,
                        0,
                    )
                    .use { db ->
                        db.execSQL(
                            "UPDATE analysis_slots SET newest=? WHERE session=?",
                            arrayOf<Any>(runs.first().generation, ids.last().toString()),
                        )
                    }
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(ids.last())
                repeat(30) { c.pump() }
                assertEquals(LiveCurveStatus.FAILED, c.snapshot().status)
                assertNull(c.snapshot().saved)
                assertTrue(c.sessions().cards.single { it.session.id == ids.last() }.error)
                offer(current, InputPayload.MetricReading(Metric.POWER_W, 0.0))
                current.pump(true)
                assertEquals(0, current.snapshot().pending)
                assertEquals(LiveStorageStatus.SAVED, current.snapshot().status)
            }
        }
    }

    @Test
    fun `explicit refresh reloads a selected active session after its acknowledged local end`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val current = owner(n)
            offer(current, InputPayload.Recording(RecordingState.RECORDING))
            current.pump(true)
            val selected = current.snapshot().session!!.id
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(selected)
                repeat(30) { c.pump() }
                assertEquals(LiveCurveReason.ACTIVE_SESSION, c.snapshot().reason)
                assertFalse(c.snapshot().canCalculate)
                offer(current, InputPayload.Recording(RecordingState.IDLE))
                current.pump(true)
                c.refreshSessions()
                repeat(30) { c.pump() }
                assertEquals(selected, c.snapshot().session!!.id)
                assertEquals(SessionPhase.ENDED_LOCALLY, c.snapshot().session!!.phase)
                assertTrue(c.snapshot().canCalculate)
                assertNull(results.newest(PowerCurveCalculation.realSlot(selected)))
            }
        }
    }

    @Test
    fun `selected valid zero and positive sessions retain distinct values and generations after reopen`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val ids = rides(inputs, 2, baseWatts = 0.0)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                ids.forEachIndexed { index, selected ->
                    c.select(selected)
                    c.pump()
                    c.request()
                    repeat(60) { c.pump() }
                    assertEquals(index + 1L, c.snapshot().saved!!.generation)
                    assertEquals(
                        index * 50.0,
                        c.snapshot()
                            .saved!!
                            .terminal!!
                            .curve!!
                            .snapshot
                            .durations
                            .first()
                            .record!!
                            .meanWatts,
                        1e-8,
                    )
                    assertTrue(
                        c.snapshot().saved!!.terminal!!.curve!!.snapshot.durations.drop(1).all {
                            it.record == null
                        }
                    )
                }
            }
            AnalysisRepository(context, n).use { results ->
                val fresh = LiveCurveController(inputs, results, current, "test", { now })
                repeat(30) { fresh.pump() }
                assertNull(fresh.snapshot().session)
                fresh.select(ids.first())
                fresh.pump()
                assertEquals(1L, fresh.snapshot().saved!!.generation)
                assertEquals(
                    0.0,
                    fresh
                        .snapshot()
                        .saved!!
                        .terminal!!
                        .curve!!
                        .snapshot
                        .durations
                        .first()
                        .record!!
                        .meanWatts,
                    1e-8,
                )
                assertEquals(
                    2L,
                    results.newest(PowerCurveCalculation.realSlot(ids.last()))!!.generation,
                )
            }
        }
    }

    @Test
    fun `starting a new generation clears older coverage from the selected list card`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val old = PowerCurveCalculation(inputs, results).prepareReal(id, "old-code")
                results.complete(old.generation, old.terminal)
                val c = LiveCurveController(inputs, results, current, "new-code", { now })
                c.select(id)
                repeat(30) { c.pump() }
                assertEquals(1, c.sessions().cards.single().availableDurations)
                c.request()
                c.pump()
                assertEquals(LiveCurveStatus.CALCULATING, c.snapshot().status)
                assertNull(c.snapshot().saved)
                assertNull(
                    "An in-progress generation must not expose older coverage",
                    c.sessions().cards.single().availableDurations,
                )
                offer(current, InputPayload.Recording(RecordingState.RECORDING))
                c.pump()
                assertTrue(c.sessions().cards.single().error)
                assertNull(c.sessions().cards.single().availableDurations)
            }
        }
    }

    @Test
    fun `new controller never implicitly selects or loads the latest saved session`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val prepared = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                results.complete(prepared.generation, prepared.terminal)
                val c = LiveCurveController(inputs, results, owner(n), "test", { now })
                repeat(30) { c.pump() }
                assertFalse(
                    "A fresh runtime must require explicit selection",
                    c.snapshot().canCalculate,
                )
                assertNull(c.snapshot().saved)
                assertEquals(
                    prepared.generation,
                    results.newest(PowerCurveCalculation.realSlot(id))!!.generation,
                )
            }
        }
    }

    @Test
    fun `real policy session stores five second record with real origin and reopens`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val completion = PowerCurveCalculation(inputs, results).prepareReal(id, "test-code")
                assertEquals(AnalysisStatus.AVAILABLE, completion.terminal.status)
                val curve = completion.terminal.curve!!
                assertEquals(200.0, curve.snapshot.durations.first().record!!.meanWatts, 1e-9)
                assertTrue(curve.snapshot.durations.drop(1).all { it.record == null })
                assertEquals(
                    "committed-real-karoo-diagnostic-effects-v1",
                    curve.snapshot.provenance.coverageOrigin,
                )
                results.complete(completion.generation, completion.terminal)
            }
        }
        AnalysisRepository(context, n).use { results ->
            val saved =
                results.current(
                    AnalysisSlot(
                        id,
                        "observed-power-curve",
                        "real-karoo-diagnostic-power-hold-v1-work-v2",
                    )
                )!!
            assertEquals(
                200.0,
                saved.terminal!!.curve!!.snapshot.durations.first().record!!.meanWatts,
                1e-9,
            )
        }
    }

    private var now = 0L

    private fun owner(n: String): LiveDiagnosticCoordinator {
        val newEpoch = UUID(0, 203)
        val inputs =
            InputRepository(context, n, acquisition = metadata(newEpoch)).also(workerInputs::add)
        val owner = LiveDiagnosticCoordinator(inputs, epoch = newEpoch, elapsed = { now })
        offer(owner, InputPayload.Recording(RecordingState.IDLE))
        owner.pump(true)
        return owner
    }

    private fun offer(owner: LiveDiagnosticCoordinator, p: InputPayload) =
        synchronized(owner.gate) {
            owner.accept(
                SourceObservation(
                    owner.stamp(),
                    SourceRef("karoo", "1.1.9"),
                    "karoo-normalization-v1",
                    p,
                )
            )
        }

    @Test
    fun `missing only session persists unavailable durations rather than zeros`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs, true)
            AnalysisRepository(context, n).use { results ->
                val c = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                assertEquals(AnalysisStatus.AVAILABLE, c.terminal.status)
                assertTrue(
                    c.terminal.curve!!.snapshot.durations.all {
                        it.record == null && it.unavailableReason != null
                    }
                )
            }
        }
    }

    @Test
    fun `cooperative job freezes revision while real canonical writes continue between steps`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val job = PowerCurveCalculation(inputs, results).beginReal(id, "test")
                val frozen = job.revision
                var iterations = 0
                while (job.completion == null) {
                    now += 1_000_000_000
                    offer(owner, InputPayload.MetricReading(Metric.POWER_W, 999.0))
                    owner.pump(true)
                    job.step()
                    assertTrue(job.retainedPageSize <= 256)
                    assertTrue(++iterations < 100)
                }
                val c = job.completion!!
                assertTrue(inputs.state().revision > frozen)
                assertEquals(frozen, c.terminal.curve!!.revision)
                assertEquals(
                    200.0,
                    c.terminal.curve!!.snapshot.durations.first().record!!.meanWatts,
                    1e-9,
                )
                assertEquals(iterations + 1L, owner.snapshot().committed)
            }
        }
    }

    @Test
    fun `controller coalesces taps and reloads saved result without creating another generation`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                c.pump()
                repeat(100) { c.request() }
                repeat(50) {
                    owner.pump()
                    c.pump()
                    c.request()
                }
                assertEquals(LiveCurveStatus.SAVED, c.snapshot().status)
                val generation = c.snapshot().saved!!.generation
                val reopen =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                repeat(30) { reopen.pump() }
                assertEquals(generation, reopen.snapshot().saved!!.generation)
                assertFalse(reopen.hasWork)
                assertEquals(1L, generation)
                val text = liveCurveText(context, reopen.snapshot())
                assertTrue(text.contains("5 s: 200.0 W"))
                assertTrue(text.contains("Acquisition verification incomplete"))
                assertTrue(text.contains("30 s: Unavailable"))
            }
        }
    }

    @Test
    fun `new received recording cancels allocated job before applying queued lifecycle`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                c.pump()
                c.request()
                repeat(20) {
                    if (results.newest(PowerCurveCalculation.realSlot(id)) == null) c.pump()
                }
                assertEquals(
                    AnalysisStatus.PENDING,
                    results.newest(PowerCurveCalculation.realSlot(id))!!.status,
                )
                val committed = owner.snapshot().committed
                offer(owner, InputPayload.Recording(RecordingState.RECORDING))
                c.pump()
                assertEquals(committed, owner.snapshot().committed)
                assertEquals(1, owner.snapshot().pending)
                assertEquals(
                    "CANCELLED",
                    results.newest(PowerCurveCalculation.realSlot(id))!!.terminal!!.reason,
                )
                assertNull(c.snapshot().saved)
                owner.pump(true)
                c.pump()
                assertFalse(c.snapshot().canCalculate)
            }
        }
    }

    @Test
    fun `uncertain request and completion responses reuse their exact generation and terminal`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                c.pump()
                c.request()
                var requestLost = false
                var completionLost = false
                results.afterCommit = {
                    val run = results.newest(PowerCurveCalculation.realSlot(id))
                    if (run?.terminal == null && !requestLost) {
                        requestLost = true
                        throw android.database.sqlite.SQLiteDiskIOException()
                    }
                    if (run?.terminal != null && !completionLost) {
                        completionLost = true
                        throw android.database.sqlite.SQLiteDiskIOException()
                    }
                }
                repeat(100) {
                    now += 1_000_000_000
                    owner.pump()
                    c.pump()
                }
                assertEquals(LiveCurveStatus.SAVED, c.snapshot().status)
                assertEquals(1L, c.snapshot().saved!!.generation)
                assertTrue(requestLost && completionLost)
                val saved = c.snapshot().saved!!.terminal
                results.afterCommit = { throw android.database.sqlite.SQLiteDiskIOException() }
                assertThrows(android.database.sqlite.SQLiteDiskIOException::class.java) {
                    results.complete(1, saved!!)
                }
                results.afterCommit = {}
                assertEquals(saved, results.newest(PowerCurveCalculation.realSlot(id))!!.terminal)
            }
        }
    }

    @Test
    fun `live result cap and native full error preserve canonical rows and pending newest`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n, 32L * 1024 * 1024).use { results ->
                assertEquals(32L * 1024 * 1024, results.maximumBytesForTest())
                val c = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                results.limitStorageForTest(1)
                assertThrows(android.database.sqlite.SQLiteFullException::class.java) {
                    results.complete(
                        c.generation,
                        AnalysisTerminal(
                            AnalysisStatus.FAILED,
                            Instant.EPOCH,
                            reason = "x".repeat(500_000),
                        ),
                    )
                }
                assertEquals(9, inputs.observations().size)
                assertEquals(
                    AnalysisStatus.PENDING,
                    results.newest(PowerCurveCalculation.realSlot(id))!!.status,
                )
            }
        }
    }

    @Test
    fun `real origin absent or unknown metadata is refused without a synthetic fallback`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                assertThrows(IllegalArgumentException::class.java) {
                    PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                }
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                assertNull(results.newest(PowerCurveCalculation.slot(id)))
            }
        }
    }

    @Test
    fun `real curve cannot be committed to the synthetic result slot`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val c = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                val wrong =
                    results.request(
                        PowerCurveCalculation.slot(id),
                        UUID.randomUUID(),
                        AnalysisProvenance(
                            "i",
                            "s",
                            "p",
                            "m",
                            "pre",
                            "par",
                            "r",
                            "t",
                            Instant.EPOCH,
                            "test",
                        ),
                    )
                assertThrows(IllegalStateException::class.java) {
                    results.complete(wrong, c.terminal)
                }
            }
        }
    }

    @Test
    fun `unsupported stored codec stays failed until explicit retry rather than polling back to an old value`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val prepared = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                results.complete(prepared.generation, prepared.terminal)
                val owner = owner(n)
                val controller =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                val db =
                    android.database.sqlite.SQLiteDatabase.openDatabase(
                        context.getDatabasePath(n).path,
                        null,
                        0,
                    )
                val bad =
                    JSONObject(prepared.terminal.encode())
                        .also { it.getJSONObject("curve").put("codec", 99) }
                        .toString()
                db.execSQL("UPDATE analysis_runs SET terminal=?", arrayOf(bad))
                controller.pump()
                assertEquals(LiveCurveStatus.FAILED, controller.snapshot().status)
                assertNull(controller.snapshot().saved)
                db.execSQL(
                    "UPDATE analysis_runs SET terminal=?",
                    arrayOf(prepared.terminal.encode()),
                )
                controller.pump()
                assertEquals(LiveCurveStatus.FAILED, controller.snapshot().status)
                controller.request()
                controller.pump()
                assertEquals(LiveCurveStatus.SAVED, controller.snapshot().status)
                assertEquals(prepared.generation, controller.snapshot().saved!!.generation)
                db.close()
            }
        }
    }

    @Test
    fun `all nine real durations survive paged calculation and v3 codec refuses false origin`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs, seconds = 3600)
            AnalysisRepository(context, n).use { results ->
                val job = PowerCurveCalculation(inputs, results).beginReal(id, "test")
                var high = 0
                while (job.completion == null) {
                    job.step()
                    high = maxOf(high, job.retainedPageSize)
                }
                assertEquals(256, high)
                val c = job.completion!!
                c.terminal.curve!!.snapshot.durations.forEach {
                    assertEquals(200.0, it.record!!.meanWatts, 1e-9)
                }
                val encoded = c.terminal.encode()
                assertEquals(3, JSONObject(encoded).getJSONObject("curve").getInt("codec"))
                assertEquals(c.terminal, AnalysisTerminal.decode(encoded))
                assertThrows(IllegalArgumentException::class.java) {
                    c.terminal.curve!!.copy(acquisition = null)
                }
                val unknown =
                    JSONObject(c.terminal.curve!!.acquisition!!).put("ordering", "unknown-order-v2")
                assertThrows(IllegalArgumentException::class.java) {
                    c.terminal.curve!!.copy(acquisition = unknown.toString())
                }

                assertThrows(IllegalStateException::class.java) {
                    results.replaceMembership("forbidden", setOf(c.generation))
                }
            }
        }
    }

    @Test
    fun `abandoned selected result needs explicit retry and remains readable during a newer ride`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val abandoned = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                val owner = owner(n)
                val controller =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                repeat(20) { controller.pump() }
                assertEquals(LiveCurveStatus.PENDING, controller.snapshot().status)
                assertEquals(abandoned.generation, controller.snapshot().saved!!.generation)
                controller.request()
                repeat(50) {
                    owner.pump()
                    controller.pump()
                }
                assertEquals(LiveCurveStatus.SAVED, controller.snapshot().status)
                val newer = controller.snapshot().saved!!.generation
                assertTrue(newer > abandoned.generation)
                results.complete(abandoned.generation, abandoned.terminal)
                assertEquals(newer, results.newest(PowerCurveCalculation.realSlot(id))!!.generation)
                offer(owner, InputPayload.Recording(RecordingState.RECORDING))
                owner.pump(true)
                controller.pump()
                assertFalse(controller.snapshot().canCalculate)
                assertEquals(newer, controller.snapshot().saved!!.generation)
                assertEquals(id, controller.snapshot().session!!.id)
            }
        }
    }

    @Test
    fun `received recording then idle cannot hide cancellation while earlier controls remain queued`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val controller =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                controller.pump()
                controller.request()
                controller.pump()
                offer(owner, InputPayload.Recording(RecordingState.RECORDING))
                offer(owner, InputPayload.Recording(RecordingState.IDLE))
                controller.pump()
                assertEquals(LiveCurveReason.CANCELLED, controller.snapshot().reason)
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                assertEquals(2, owner.snapshot().pending)
            }
        }
    }

    @Test
    fun `canonical storage failure cancels numerical work and retains its failed terminal until writer recovers`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val controller =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                controller.pump()
                controller.request()
                repeat(20) {
                    if (results.newest(PowerCurveCalculation.realSlot(id)) == null)
                        controller.pump()
                }
                offer(owner, InputPayload.MetricReading(Metric.POWER_W, 0.0))
                workerInputs.last().beforeCheckpoint = {
                    throw android.database.sqlite.SQLiteFullException()
                }
                owner.pump(true)
                controller.pump()
                assertEquals(LiveStorageStatus.RETRYING, owner.snapshot().status)
                assertEquals(
                    AnalysisStatus.PENDING,
                    results.newest(PowerCurveCalculation.realSlot(id))!!.status,
                )
                assertFalse(controller.hasWork)
                workerInputs.last().beforeCheckpoint = {}
                now += 1_000_000_000
                owner.pump(true)
                controller.pump()
                assertEquals(
                    "CANCELLED",
                    results.newest(PowerCurveCalculation.realSlot(id))!!.terminal!!.reason,
                )
                assertEquals(0, owner.snapshot().pending)
            }
        }
    }

    @Test
    fun `queued recording and idle cannot select the previous ended session before control acknowledgement`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val owner = owner(n)
            AnalysisRepository(context, n).use { results ->
                val controller =
                    LiveCurveController(inputs, results, owner, "test", { now }).also {
                        it.select(id)
                    }
                controller.pump()
                assertTrue(controller.snapshot().canCalculate)
                offer(owner, InputPayload.Recording(RecordingState.RECORDING))
                offer(owner, InputPayload.Recording(RecordingState.IDLE))
                controller.request()
                owner.pump()
                controller.pump()
                assertEquals(LiveCurveStatus.UNAVAILABLE, controller.snapshot().status)
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                assertFalse(owner.canAnalyze())
                assertEquals(2, owner.snapshot().pending)
                owner.pump(true)
                controller.pump()
                assertTrue(controller.snapshot().canCalculate)
                val latest = owner.snapshot().session!!.id
                assertNotEquals(id, latest)
                assertEquals(id, controller.snapshot().session!!.id)
                controller.select(latest)
                controller.pump()
                controller.request()
                repeat(60) {
                    owner.pump()
                    controller.pump()
                }
                assertEquals(latest, controller.snapshot().saved!!.slot.session)
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
            }
        }
    }
}
