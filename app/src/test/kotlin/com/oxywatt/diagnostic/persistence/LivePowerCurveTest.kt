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
                val c = LiveCurveController(inputs, results, owner, "test", { now })
                c.pump()
                repeat(100) { c.request() }
                repeat(50) {
                    owner.pump()
                    c.pump()
                    c.request()
                }
                assertEquals(LiveCurveStatus.SAVED, c.snapshot().status)
                val generation = c.snapshot().saved!!.generation
                val reopen = LiveCurveController(inputs, results, owner, "test", { now })
                reopen.pump()
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
                val c = LiveCurveController(inputs, results, owner, "test", { now })
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
                val c = LiveCurveController(inputs, results, owner, "test", { now })
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
                val controller = LiveCurveController(inputs, results, owner, "test", { now })
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
    fun `abandoned pending needs explicit retry and an active latest session never falls back`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val abandoned = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                val owner = owner(n)
                val controller = LiveCurveController(inputs, results, owner, "test", { now })
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
                assertNull(controller.snapshot().saved)
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
                val controller = LiveCurveController(inputs, results, owner, "test", { now })
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
                val controller = LiveCurveController(inputs, results, owner, "test", { now })
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
                val controller = LiveCurveController(inputs, results, owner, "test", { now })
                controller.pump()
                assertTrue(controller.snapshot().canCalculate)
                offer(owner, InputPayload.Recording(RecordingState.RECORDING))
                offer(owner, InputPayload.Recording(RecordingState.IDLE))
                controller.request()
                owner.pump()
                controller.pump()
                assertEquals(LiveCurveStatus.UNAVAILABLE, controller.snapshot().status)
                assertFalse(controller.hasWork)
                assertFalse(owner.canAnalyze())
                assertEquals(2, owner.snapshot().pending)
                owner.pump(true)
                controller.pump()
                assertTrue(controller.snapshot().canCalculate)
                val latest = owner.snapshot().session!!.id
                assertNotEquals(id, latest)
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
