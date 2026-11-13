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

    @Test
    fun `execution accounting excludes nested page work and preserves residual and retry waits`() {
        var execution = 100L
        val t = CurveTiming(clock = { execution })
        execution += 7
        t.mark(CurveMark.FIRST_WORKER)
        t.measure(CurveWork.HASH) {
            execution += 11
            t.measure(CurveWork.HASH_PAGE) { execution += 13 }
            execution += 17
        }
        t.measure(CurveWork.WAIT) { execution += 19 }
        t.measure(CurveWork.BACKOFF) { execution += 23 }
        t.count(CurveCount.WAITS)
        t.count(CurveCount.RETRIES)
        execution += 29
        t.finish(CurveOutcome.SAVED)
        val s = t.snapshot()
        assertEquals(28L, s.work.getValue(CurveWork.HASH))
        assertEquals(13L, s.work.getValue(CurveWork.HASH_PAGE))
        assertEquals(19L, s.work.getValue(CurveWork.WAIT))
        assertEquals(23L, s.work.getValue(CurveWork.BACKOFF))
        assertEquals(36L, s.residualNanos)
        assertEquals(119L, s.elapsedNanos)
        assertEquals(7L, s.marks.getValue(CurveMark.FIRST_WORKER))
        assertEquals(1L, s.counts.getValue(CurveCount.WAITS))
        assertEquals(CurveOutcome.SAVED, s.outcome)
        assertFalse(s.incomplete)
    }

    @Test
    fun `broken execution clock cannot swallow analysis errors or stop work`() {
        var fail = false
        val t = CurveTiming(clock = { if (fail) error("clock failure") else 1L })
        fail = true
        var ran = false
        t.measure(CurveWork.REPLAY) { ran = true }
        assertTrue(ran)
        val failure = IllegalArgumentException("analysis failure")
        try {
            t.measure(CurveWork.HASH) { throw failure }
            fail("Expected analysis failure")
        } catch (actual: IllegalArgumentException) {
            assertSame(failure, actual)
        }
        t.finish(CurveOutcome.FAILED)
        assertTrue(t.snapshot().incomplete)
        assertEquals(CurveOutcome.FAILED, t.snapshot().outcome)
    }

    /** Literal first-ride facts and an independent typed-token SHA-256 encoder. */
    private fun timingHashOracle(
        acquisition: String = metadata(),
        targetId: UUID = id,
        receiptEpoch: UUID = epoch,
    ): String {
        val id = targetId
        val epoch = receiptEpoch
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        fun token(text: String?) {
            digest.update(
                if (text == null) "N;".toByteArray()
                else
                    ("S" + text.toByteArray(Charsets.UTF_8).size + ":" + text).toByteArray(
                        Charsets.UTF_8
                    )
            )
        }
        fun document(value: Any?) {
            when (value) {
                null,
                JSONObject.NULL -> token(null)
                is JSONObject -> {
                    val keys = value.keys().asSequence().sorted().toList()
                    token("object")
                    token(keys.size.toString())
                    keys.forEach {
                        token(it)
                        document(value.get(it))
                    }
                }
                is org.json.JSONArray -> {
                    token("array")
                    token(value.length().toString())
                    repeat(value.length()) { document(value.get(it)) }
                }
                is Number -> {
                    token("number")
                    token(value.toString())
                }
                is Boolean -> {
                    token("boolean")
                    token(value.toString())
                }
                is String -> {
                    token("string")
                    token(value)
                }
                else -> error("Unexpected literal")
            }
        }
        fun stamp(second: Int, sequence: Int) =
            JSONObject(
                """{"utc":"${Instant.EPOCH.plusSeconds(second.toLong())}","epoch":"$epoch","elapsed":${second * 1_000_000_000L},"sequence":$sequence}"""
            )
        val source = """{"adapter":"karoo","version":"1.1.9","sequence":null}"""
        token("power-replay-hash-v1")
        token(id.toString())
        token("canonical-power-hold-v1:power-range-means-v2:power-replay-work-v2:16384:100000000")
        token("REAL_KAROO_DIAGNOSTIC:v1")
        for (sequence in 1..8) {
            val second = if (sequence == 1) 0 else sequence - 2
            val ended = sequence == 8
            val phase = if (ended) "ENDED_LOCALLY" else "RECORDING"
            val payload =
                when (sequence) {
                    1 -> """{"kind":"recording","state":"RECORDING"}"""
                    8 -> """{"kind":"recording","state":"IDLE"}"""
                    else ->
                        """{"kind":"metric","metric":"POWER_W","value":"0x1.9p6","missing":null,"sourceUtc":null,"meaning":null}"""
                }
            val session =
                """{"id":"$id","epoch":"$epoch","phase":"$phase","origin":"OBSERVED_START","first":${stamp(0,1)},"last":${stamp(second,sequence)},"segment":0,"policy":{"version":"karoo-diagnostic-silence-v1","silence":5000000000},"source":$source,"normalization":"karoo-normalization-v1","ambiguity":null,"interruption":null}"""
            val breaks =
                if (ended)
                    """[{"session":"$id","metric":"POWER_W","reason":"SESSION_ENDED","time":6000000000}]"""
                else "[]"
            val selected =
                JSONObject(
                    """{"envelope":{"session":"$id","input":{"stamp":${stamp(second,sequence)},"source":$source,"normalization":"karoo-normalization-v1","payload":$payload}},"segment":${if(ended) "null" else "0"},"sessions":[$session],"breaks":$breaks,"rejection":null,"checkpointSchema":"collector-checkpoint-v1","profile":null,"horizonReceipt":${second * 1_000_000_000L},"metadata":{"codec":1,"policy":{"version":"admission-v1","events":4096,"bytes":4194304,"reservedEvents":64,"reservedBytes":65536,"envelopeBytes":1024,"flushNanos":5000000000},"terminal":null,"loss":null}}"""
                )
            document(selected)
            document(JSONObject(acquisition))
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `matched pacing preserves independent curves while reducing waits with unrelated history and canonical interleaving`() {
        val expectedHash = timingHashOracle()
        val baseline = mutableMapOf<Int, com.oxywatt.core.power.PowerCurveSnapshot>()
        for (quantum in listOf(1, 16)) {
            for (variant in 0..2) {
                val n = name()
                InputRepository(context, n, acquisition = metadata()).use { inputs ->
                    val ids = rides(inputs, 2)
                    val current = owner(n)
                    if (variant == 1)
                        repeat(1024) {
                            now += 1_000_000
                            offer(current, InputPayload.MetricReading(Metric.POWER_W, 999.0))
                            if (it % 128 == 127) current.pump(true)
                        }
                    val frozen = inputs.state().revision
                    // Two rides each have Idle, Recording, six power inputs, Idle: 18 effects.
                    // owner() persists a recovery/time cut and its current-epoch Idle: two more
                    // effects.
                    val scanned = if (variant == 1) 1044L else 20L
                    AnalysisRepository(context, n).use { results ->
                        val c =
                            LiveCurveController(
                                inputs,
                                results,
                                current,
                                "test",
                                { now },
                                pacing = AnalysisPacing(quantum),
                            )
                        c.select(ids.first())
                        repeat(30) { c.pump() }
                        c.request()
                        c.request()
                        assertEquals(0L, c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS))
                        val deadline = System.nanoTime() + 120_000_000_000L
                        c.measureWorker(CurveWork.CANONICAL) { current.pump() }
                        c.pump() // Freeze R before injecting the interleaved canonical writes.
                        if (c.shouldWaitAfterTurn()) c.waitForWorker { Thread.sleep(1) }
                        while (c.snapshot().status != LiveCurveStatus.SAVED) {
                            if (variant == 2) {
                                now += 1_000_000_000
                                offer(current, InputPayload.MetricReading(Metric.POWER_W, 999.0))
                            }
                            c.measureWorker(CurveWork.CANONICAL) { current.pump(variant == 2) }
                            c.pump()
                            val mustWait = c.shouldWaitAfterTurn()
                            if (c.snapshot().status != LiveCurveStatus.SAVED && mustWait)
                                c.waitForWorker { Thread.sleep(1) }
                            assertTrue(
                                "Host timing diagnostic incomplete: 120 s limit",
                                System.nanoTime() < deadline,
                            )
                        }
                        val result = c.snapshot().saved!!
                        val curve = result.terminal!!.curve!!
                        assertEquals(expectedHash, curve.inputHash)
                        if (quantum == 1) baseline[variant] = curve.snapshot
                        else assertEquals(baseline.getValue(variant), curve.snapshot)
                        assertEquals(frozen, curve.revision)
                        assertEquals(1L, curve.firstRevision)
                        assertEquals(1L, curve.lastRevision)
                        assertEquals(8L, curve.effectCount)
                        val record = curve.snapshot.durations.first().record!!
                        assertEquals(100.0, record.meanWatts, 1e-9)
                        assertEquals(0L, record.startNanos)
                        assertEquals(5_000_000_000L, record.endNanos)
                        curve.snapshot.durations.drop(1).forEach {
                            assertNull(it.record)
                            assertEquals(
                                com.oxywatt.core.power.PowerUnavailableReason
                                    .INSUFFICIENT_CONTIGUOUS_COVERAGE,
                                it.unavailableReason,
                            )
                        }
                        assertNull(results.newest(PowerCurveCalculation.realSlot(ids.last())))
                        val timing = c.timingSnapshot()!!
                        assertEquals(CurveOutcome.SAVED, timing.outcome)
                        assertFalse(timing.incomplete)
                        assertEquals(quantum, timing.maximumTurnsBetweenWaits)
                        assertEquals(
                            if (quantum == 1) "single-effect-wait-v1"
                            else "diagnostic-analysis-pacing-v1",
                            timing.pacingPolicy,
                        )
                        assertEquals(
                            (2 * scanned + 3) / quantum,
                            timing.counts.getValue(CurveCount.WAITS),
                        )
                        assertEquals(scanned, timing.counts.getValue(CurveCount.HASH_SCANNED))
                        assertEquals(scanned, timing.counts.getValue(CurveCount.REPLAY_SCANNED))
                        assertEquals(8L, timing.counts.getValue(CurveCount.HASH_SELECTED))
                        assertEquals(8L, timing.counts.getValue(CurveCount.REPLAY_SELECTED))
                        val pages = (scanned + 255) / 256 + 1
                        assertEquals(pages, timing.counts.getValue(CurveCount.HASH_PAGES))
                        assertEquals(pages, timing.counts.getValue(CurveCount.REPLAY_PAGES))
                        assertEquals(2 * scanned + 3, timing.counts.getValue(CurveCount.STEPS))
                        assertEquals(
                            timing.elapsedNanos,
                            timing.work.values.sum() + timing.residualNanos,
                        )
                        assertTrue(timing.work.getValue(CurveWork.WAIT) > 0)
                        c.request()
                        c.pump()
                        assertEquals(timing, c.timingSnapshot())
                        assertEquals(result.generation, c.snapshot().saved!!.generation)
                        assertEquals(1L, result.generation)
                        if (variant == 2) {
                            assertTrue(inputs.state().revision > frozen)
                            assertEquals(0, current.snapshot().pending)
                        }
                        println("S17 synthetic quantum=$quantum variant=$variant timing=$timing")
                    }
                    AnalysisRepository(context, n).use { reopened ->
                        assertEquals(
                            expectedHash,
                            reopened
                                .newest(PowerCurveCalculation.realSlot(id))!!
                                .terminal!!
                                .curve!!
                                .inputHash,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `disabled or failed timing and capped report cannot change the saved result`() {
        val reportFile = java.io.File(context.cacheDir, "timing-limit-${UUID.randomUUID()}")
        try {
            val capture = DiagnosticCapture(reportFile, maxBytes = 1)
            capture.sourceStarted()
            capture.flush()
            assertEquals(DiagnosticReportStatus.LIMIT_REACHED, capture.snapshot().reportStatus)
            val bytes = reportFile.length()
            val expectedHash = timingHashOracle()
            for (clock in
                listOf<(() -> Long)?>(null, { error("clock unavailable") }, System::nanoTime)) {
                val n = name()
                InputRepository(context, n, acquisition = metadata()).use { inputs ->
                    rides(inputs, 2)
                    val current = owner(n)
                    val retained =
                        AnalysisRepository(context, n).use { results ->
                            val c =
                                LiveCurveController(
                                    inputs,
                                    results,
                                    current,
                                    "test",
                                    { now },
                                    clock,
                                )
                            c.select(id)
                            repeat(30) { c.pump() }
                            c.request()
                            repeat(100) {
                                c.pump()
                                if (c.shouldWaitAfterTurn()) c.waitForWorker {}
                            }
                            val saved = c.snapshot().saved!!
                            assertEquals(1L, saved.generation)
                            assertEquals(expectedHash, saved.terminal!!.curve!!.inputHash)
                            assertEquals(
                                100.0,
                                saved.terminal!!
                                    .curve!!
                                    .snapshot
                                    .durations
                                    .first()
                                    .record!!
                                    .meanWatts,
                                1e-9,
                            )
                            val summary = c.timingSnapshot()
                            val readout = curveTimingText(context, summary)
                            if (clock == null) {
                                assertNull(summary)
                                assertTrue(readout.contains("No runtime timing available"))
                            } else {
                                assertEquals(CurveOutcome.SAVED, summary!!.outcome)
                                assertTrue(readout.contains("Outcome: saved"))
                                assertEquals(
                                    summary.incomplete,
                                    readout.contains("Incomplete execution timing"),
                                )
                            }
                            assertEquals(summary, c.timingSnapshot())
                            assertTrue(
                                saved.terminal!!.curve!!.snapshot.durations.drop(1).all {
                                    it.record == null
                                }
                            )
                            assertEquals(0, current.snapshot().pending)
                            saved
                        }
                    AnalysisRepository(context, n).use { reopened ->
                        assertEquals(retained, reopened.newest(PowerCurveCalculation.realSlot(id)))
                    }
                }
            }
            assertEquals(bytes, reportFile.length())
            assertEquals(DiagnosticReportStatus.LIMIT_REACHED, capture.snapshot().reportStatus)
        } finally {
            reportFile.delete()
        }
    }

    @Test
    fun `last attempt readout survives selection refresh and same code readback without a new generation`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            val ids = rides(inputs, 2)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                assertNull(c.timingSnapshot())
                c.select(ids.first())
                repeat(30) { c.pump() }
                c.request()
                repeat(100) { c.pump() }
                val saved = c.snapshot().saved!!
                val timing = c.timingSnapshot()!!
                assertEquals(CurveOutcome.SAVED, timing.outcome)
                val text = curveTimingText(context, timing)
                assertTrue(text.contains("May differ from the selected session; not saved"))
                assertFalse(text.contains(ids.first().toString()))
                c.select(ids.last())
                repeat(30) { c.pump() }
                assertEquals(ids.last(), c.snapshot().session!!.id)
                assertNull(c.snapshot().saved)
                assertEquals(text, curveTimingText(context, c.timingSnapshot()))
                c.refreshSessions()
                repeat(30) { c.pump() }
                assertEquals(timing, c.timingSnapshot())
                assertNull(results.newest(PowerCurveCalculation.realSlot(ids.last())))
                c.select(ids.first())
                repeat(30) { c.pump() }
                c.request()
                repeat(30) { c.pump() }
                assertEquals(saved, c.snapshot().saved)
                assertEquals(timing, c.timingSnapshot())
            }
            AnalysisRepository(context, n).use { reopened ->
                val fresh = LiveCurveController(inputs, reopened, current, "test", { now })
                fresh.select(ids.first())
                repeat(30) { fresh.pump() }
                assertEquals(1L, fresh.snapshot().saved!!.generation)
                assertNull(fresh.timingSnapshot())
                fresh.request()
                repeat(30) { fresh.pump() }
                assertNull(fresh.timingSnapshot())
                assertTrue(
                    curveTimingText(context, fresh.timingSnapshot())
                        .contains("No runtime timing available")
                )
                assertEquals(
                    1L,
                    reopened.newest(PowerCurveCalculation.realSlot(ids.first()))!!.generation,
                )
                assertNull(reopened.newest(PowerCurveCalculation.realSlot(ids.last())))
            }
        }
    }

    @Test
    fun `pre generation failure and superseded tap have terminal timing without a generation`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs) // Deliberately no acquisition provenance for the target.
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                c.select(id)
                c.pump()
                c.request()
                c.select(id)
                c.pump()
                assertEquals(CurveOutcome.DISCARDED, c.timingSnapshot()!!.outcome)
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                c.request()
                repeat(20) { c.pump() }
                val failed = c.timingSnapshot()!!
                assertEquals(CurveOutcome.FAILED, failed.outcome)
                assertFalse(failed.marks.containsKey(CurveMark.REQUESTED))
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                assertTrue(current.canAnalyze())
            }
        }
    }

    @Test
    fun `every work bucket is exclusive and snapshots cannot mutate or resume stopped timing`() {
        var tick = 0L
        val t = CurveTiming(clock = { tick })
        CurveWork.entries.forEachIndexed { index, bucket ->
            t.measure(bucket) { tick += index + 1L }
        }
        t.finish(CurveOutcome.STOPPED)
        val saved = t.snapshot()
        CurveWork.entries.forEachIndexed { index, bucket ->
            assertEquals(index + 1L, saved.work.getValue(bucket))
        }
        assertTrue(saved.incomplete)
        assertEquals(0L, saved.residualNanos)
        assertThrows(UnsupportedOperationException::class.java) {
            (saved.work as MutableMap)[CurveWork.HASH] = 999L
        }
        tick += 100
        t.measure(CurveWork.HASH) { tick += 100 }
        t.count(CurveCount.STEPS)
        assertEquals(saved, t.snapshot())
        val broken = CurveTiming(clock = { tick-- })
        broken.measure(CurveWork.HASH) {}
        assertTrue(broken.snapshot().incomplete)
    }

    @Test
    fun `selection after worker consumes a tap discards timing when the attempt cannot arm`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                var ticks = 0
                lateinit var c: LiveCurveController
                c =
                    LiveCurveController(
                        inputs,
                        results,
                        current,
                        "test",
                        { now },
                        {
                            ticks++
                            // Construction, controller entry, then FIRST_WORKER after consuming the
                            // tap.
                            if (ticks == 3) c.select(id)
                            ticks.toLong()
                        },
                    )
                c.select(id)
                c.pump()
                c.request()
                c.pump()
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                assertEquals(CurveOutcome.DISCARDED, c.timingSnapshot()!!.outcome)
            }
        }
    }

    @Test
    fun `only progressing eligible analysis can skip a wait and received controls cancel before another step`() {
        val n = name()
        InputRepository(context, n, acquisition = metadata()).use { inputs ->
            fill(inputs)
            val current = owner(n)
            AnalysisRepository(context, n).use { results ->
                val c = LiveCurveController(inputs, results, current, "test", { now })
                repeat(5) {
                    c.pump()
                    assertTrue(c.shouldWaitAfterTurn())
                }
                c.select(id)
                c.pump()
                assertTrue(c.shouldWaitAfterTurn())
                c.request()
                c.pump()
                assertFalse(c.shouldWaitAfterTurn())
                val steps = c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS)
                offer(current, InputPayload.Recording(RecordingState.RECORDING))
                offer(current, InputPayload.Recording(RecordingState.IDLE))
                c.measureWorker(CurveWork.CANONICAL) { current.pump() }
                c.pump()
                assertTrue(c.shouldWaitAfterTurn())
                assertEquals(CurveOutcome.CANCELLED, c.timingSnapshot()!!.outcome)
                assertEquals(steps, c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS))
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
            }
        }
    }

    @Test
    fun `pause stop fatal and full revoke immediate analysis before another numerical step`() {
        for (fault in 0..3) {
            val n = name()
            InputRepository(context, n, acquisition = metadata()).use { inputs ->
                fill(inputs)
                val current = owner(n)
                AnalysisRepository(context, n).use { results ->
                    val c = LiveCurveController(inputs, results, current, "test", { now })
                    c.select(id)
                    c.pump()
                    c.request()
                    c.pump()
                    assertFalse(c.shouldWaitAfterTurn())
                    val steps = c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS)
                    when (fault) {
                        0 -> offer(current, InputPayload.Recording(RecordingState.PAUSED))
                        1 -> current.stopped()
                        else -> {
                            offer(current, InputPayload.MetricReading(Metric.POWER_W, 999.0))
                            workerInputs.last().beforeCheckpoint = {
                                if (fault == 2) error("Injected canonical fatal")
                                else throw android.database.sqlite.SQLiteFullException()
                            }
                        }
                    }
                    c.measureWorker(CurveWork.CANONICAL) { current.pump(true) }
                    c.pump()
                    assertTrue(c.shouldWaitAfterTurn())
                    assertEquals(steps, c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS))
                    assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                    if (fault == 2) assertEquals(LiveStorageStatus.FATAL, current.snapshot().status)
                    if (fault == 3)
                        assertEquals(LiveStorageStatus.RETRYING, current.snapshot().status)
                }
            }
        }
    }

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

    private fun rides(
        repo: InputRepository,
        count: Int,
        baseWatts: Double = 100.0,
        receiptEpoch: UUID = epoch,
        firstId: Long = 202L,
    ): List<UUID> {
        var number = firstId
        val lifecycle =
            CollectorLifecycle(
                receiptEpoch,
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
                            receiptEpoch,
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
        return (0 until count).map { UUID(0, firstId + it) }
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
                assertEquals(CurveOutcome.CANCELLED, c.timingSnapshot()!!.outcome)
                assertFalse(c.timingSnapshot()!!.marks.containsKey(CurveMark.REPLAYED))
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
                    val priorRetries = c.timingSnapshot()!!.counts.getValue(CurveCount.RETRIES)
                    c.pump()
                    if (c.timingSnapshot()!!.counts.getValue(CurveCount.RETRIES) > priorRetries) {
                        assertTrue(c.shouldWaitAfterTurn())
                        val before = c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS)
                        c.pump()
                        assertTrue(c.shouldWaitAfterTurn())
                        assertEquals(before, c.timingSnapshot()!!.counts.getValue(CurveCount.STEPS))
                    }
                }
                assertEquals(LiveCurveStatus.SAVED, c.snapshot().status)
                assertEquals(1L, c.snapshot().saved!!.generation)
                assertTrue(requestLost && completionLost)
                assertEquals(2L, c.timingSnapshot()!!.counts.getValue(CurveCount.RETRIES))
                assertEquals(CurveOutcome.SAVED, c.timingSnapshot()!!.outcome)
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
    fun `larger live capacity recovers a native full transaction without changing committed prefix or saved curve`() {
        val n = name()
        val oldCap = 32L * 1024 * 1024
        lateinit var pending: PreparedInputWrite
        lateinit var prefix: List<StoredObservation>
        lateinit var checkpoint: StoredCollectorState
        lateinit var saved: StoredAnalysis
        InputRepository(context, n, oldCap, metadata()).use { inputs ->
            val lifecycle = fill(inputs)
            AnalysisRepository(context, n, oldCap).use { results ->
                val calculation = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                results.complete(calculation.generation, calculation.terminal)
                saved = results.newest(PowerCurveCalculation.realSlot(id))!!
            }
            prefix = inputs.observations()
            checkpoint = inputs.state()
            android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(n).path,
                    null,
                    0,
                )
                .use { db ->
                    db.setMaximumSize(oldCap)
                    db.execSQL("CREATE TABLE capacity_fixture (data BLOB NOT NULL)")
                    db.execSQL(
                        "INSERT INTO capacity_fixture VALUES (zeroblob(?))",
                        arrayOf(oldCap - 262144),
                    )
                }
            val q = BoundedAdmission()
            repeat(1000) { i ->
                assertTrue(
                    q.offer(
                        SourceObservation(
                            ReceiptStamp(Instant.EPOCH, epoch, 7_000_000_000L + i, 9L + i),
                            SourceRef("karoo", "1.1.9"),
                            "karoo-normalization-v1",
                            InputPayload.MetricReading(Metric.POWER_W, 0.0),
                        )
                    )
                )
            }
            pending = inputs.prepare(q.takeBatch()!!, lifecycle, checkpoint.revision)
            assertThrows(android.database.sqlite.SQLiteFullException::class.java) {
                inputs.commit(pending)
            }
            assertEquals(prefix, inputs.observations())
            assertEquals(checkpoint, inputs.state())
        }
        // The retained write retains its original metadata; only connection capacity changes.
        InputRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES).use { inputs ->
            AnalysisRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES).use { results ->
                assertEquals(saved, results.newest(PowerCurveCalculation.realSlot(id)))
                assertEquals(prefix, inputs.observations())
                assertEquals(checkpoint, inputs.state())
                assertEquals(checkpoint.revision + 1, inputs.commit(pending))
                assertEquals(checkpoint.revision + 1, inputs.commit(pending))
                assertEquals(prefix, inputs.observations(limit = 256).take(prefix.size))
                assertEquals(saved, results.newest(PowerCurveCalculation.realSlot(id)))
            }
        }
        assertTrue(context.getDatabasePath(n).length() > oldCap)
        android.database.sqlite.SQLiteDatabase.openDatabase(
                context.getDatabasePath(n).path,
                null,
                0,
            )
            .use { db ->
                assertEquals(67108864L, db.setMaximumSize(LiveDiagnosticStorage.MAXIMUM_BYTES))
                assertEquals(
                    oldCap - 262144,
                    db.rawQuery("SELECT length(data) FROM capacity_fixture", null).use {
                        it.moveToFirst()
                        it.getLong(0)
                    },
                )
                assertThrows(android.database.sqlite.SQLiteFullException::class.java) {
                    db.execSQL(
                        "INSERT INTO capacity_fixture VALUES (zeroblob(?))",
                        arrayOf(67108864L),
                    )
                }
            }
        InputRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES).use { inputs ->
            assertEquals(checkpoint.revision + 1, inputs.state().revision)
            assertEquals(
                1000 + prefix.size,
                inputs.observations(limit = 256).size +
                    inputs.observations(afterPosition = 256, limit = 256).size +
                    inputs.observations(afterPosition = 512, limit = 256).size +
                    inputs.observations(afterPosition = 768, limit = 256).size,
            )
        }
    }

    @Test
    fun `storage v2 real curve matches independent power window and reopens without rewriting v1`() {
        val n = name()
        val nextEpoch = UUID(0, 301)
        val nextId = UUID(0, 302)
        val acquisition =
            JSONObject(metadata(nextEpoch))
                .put("storage", "karoo-diagnostic-storage-v2")
                .put("maximumBytes", 64L * 1024 * 1024)
                .toString()
        lateinit var historical: StoredAnalysis
        lateinit var originalRows: List<CommittedEffect>
        InputRepository(context, n, 32L * 1024 * 1024, metadata()).use { inputs ->
            rides(inputs, 2)
            originalRows = inputs.replay(inputs.state().revision)
            AnalysisRepository(context, n, 32L * 1024 * 1024).use { results ->
                val c = PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                assertEquals(timingHashOracle(), c.terminal.curve!!.inputHash)
                results.complete(c.generation, c.terminal)
                historical = results.newest(PowerCurveCalculation.realSlot(id))!!
            }
        }
        lateinit var newer: StoredAnalysis
        InputRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES, acquisition).use { inputs
            ->
            rides(inputs, 2, receiptEpoch = nextEpoch, firstId = 302L)
            assertEquals(originalRows, inputs.replay(2))
            AnalysisRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES).use { results ->
                assertEquals(historical, results.newest(PowerCurveCalculation.realSlot(id)))
                val c = PowerCurveCalculation(inputs, results).prepareReal(nextId, "test")
                assertEquals(
                    timingHashOracle(acquisition, nextId, nextEpoch),
                    c.terminal.curve!!.inputHash,
                )
                val record = c.terminal.curve!!.snapshot.durations.first().record!!
                assertEquals(100.0, record.meanWatts, 0.0)
                assertEquals(0L, record.startNanos)
                assertEquals(5_000_000_000L, record.endNanos)
                assertTrue(
                    c.terminal.curve!!.snapshot.durations.drop(1).all {
                        it.record == null &&
                            it.unavailableReason ==
                                com.oxywatt.core.power.PowerUnavailableReason
                                    .INSUFFICIENT_CONTIGUOUS_COVERAGE
                    }
                )
                assertEquals(2L, c.generation)
                results.complete(c.generation, c.terminal)
                newer = results.newest(PowerCurveCalculation.realSlot(nextId))!!
            }
        }
        AnalysisRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES).use { results ->
            InputRepository(context, n, LiveDiagnosticStorage.MAXIMUM_BYTES).use { inputs ->
                assertEquals(originalRows, inputs.replay(2))
                assertEquals(historical, results.newest(PowerCurveCalculation.realSlot(id)))
                assertEquals(newer, results.newest(PowerCurveCalculation.realSlot(nextId)))
            }
        }
    }

    @Test
    fun `live curves reject unknown mismatched and mixed storage policy metadata without creating a generation`() {
        for ((policy, cap) in
            listOf(
                "karoo-diagnostic-storage-v1" to 67108864L,
                "karoo-diagnostic-storage-v2" to 33554432L,
                "karoo-diagnostic-storage-v3" to 67108864L,
                "karoo-diagnostic-storage-v2" to 67112960L,
            )) {
            val n = name()
            val acquisition =
                JSONObject(metadata()).put("storage", policy).put("maximumBytes", cap).toString()
            InputRepository(context, n, acquisition = acquisition).use { inputs ->
                fill(inputs)
                val prefix = inputs.replay(inputs.state().revision)
                AnalysisRepository(context, n).use { results ->
                    assertThrows(IllegalArgumentException::class.java) {
                        PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                    }
                    assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                    assertEquals(prefix, inputs.replay(inputs.state().revision))
                }
            }
        }
        val n = name()
        val lifecycle =
            CollectorLifecycle(
                epoch,
                LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
                { id },
            )
        fun part(first: Boolean): AdmissionBatch {
            val q = BoundedAdmission()
            fun put(t: Long, seq: Long, payload: InputPayload) {
                assertTrue(
                    q.offer(
                        SourceObservation(
                            ReceiptStamp(Instant.EPOCH, epoch, t * 1_000_000_000, seq),
                            SourceRef("karoo", "1.1.9"),
                            "karoo-normalization-v1",
                            payload,
                        )
                    )
                )
            }
            if (first) {
                put(0, 0, InputPayload.Recording(RecordingState.IDLE))
                put(0, 1, InputPayload.Recording(RecordingState.RECORDING))
                put(0, 2, InputPayload.MetricReading(Metric.POWER_W, 100.0))
            } else {
                for (t in 1L..5L) put(t, t + 2, InputPayload.MetricReading(Metric.POWER_W, 100.0))
                put(6, 8, InputPayload.Recording(RecordingState.IDLE))
            }
            return q.takeBatch()!!
        }
        InputRepository(context, n, acquisition = metadata()).use {
            it.commit(it.prepare(part(true), lifecycle, 0))
        }
        val mixed =
            JSONObject(metadata())
                .put("storage", "karoo-diagnostic-storage-v2")
                .put("maximumBytes", 67108864L)
                .toString()
        InputRepository(context, n, acquisition = mixed).use { inputs ->
            inputs.commit(inputs.prepare(part(false), lifecycle, 1))
            AnalysisRepository(context, n).use { results ->
                assertThrows(IllegalArgumentException::class.java) {
                    PowerCurveCalculation(inputs, results).prepareReal(id, "test")
                }
                assertNull(results.newest(PowerCurveCalculation.realSlot(id)))
                assertEquals(9, inputs.observations().size)
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
                assertTrue(controller.shouldWaitAfterTurn())
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
