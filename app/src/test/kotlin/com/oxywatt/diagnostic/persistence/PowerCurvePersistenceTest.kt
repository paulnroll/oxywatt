package com.oxywatt.diagnostic.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
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
class PowerCurvePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val names = mutableListOf<String>()
    private val id = UUID(0, 41)
    private val epoch = UUID(0, 42)

    private fun name() = "power-${UUID.randomUUID()}.db".also(names::add)

    @After
    fun clean() {
        names.forEach { context.deleteDatabase(it) }
    }

    private fun input(t: Long, seq: Long, payload: InputPayload) =
        SourceObservation(
            ReceiptStamp(Instant.EPOCH, epoch, t * 1_000_000_000, seq),
            SourceRef("simulated", "1"),
            "canonical-v1",
            payload,
        )

    private fun record(s: RecordingState) = InputPayload.Recording(s)

    private fun power(w: Double) = InputPayload.MetricReading(Metric.POWER_W, w)

    private fun fill(
        repo: InputRepository,
        watts: Double = 200.0,
        ttl: Long = 2_000_000_000,
        seconds: Long = 6,
    ): CollectorLifecycle {
        val l = CollectorLifecycle(epoch, LifecyclePolicy("preview-silence-v1", ttl), { id })
        val q = BoundedAdmission()
        q.offer(input(0, 0, record(RecordingState.IDLE)))
        q.offer(input(0, 1, record(RecordingState.RECORDING)))
        for (t in 0L until seconds) q.offer(input(t, t + 2, power(watts)))
        q.offer(input(seconds, seconds + 2, record(RecordingState.IDLE)))
        val b = q.takeBatch()!!
        repo.commit(repo.prepare(b, l, repo.state().revision))
        q.committed(b)
        return l
    }

    private fun prepare(repo: InputRepository, results: AnalysisRepository) =
        PowerCurveCalculation(repo, results)
            .prepare(id, "SYNTHETIC:constant-v1:seed=0:sha256=fixture-v1", "test-revision")

    @Test
    fun `terminal curve round trips scalar v1 remains readable and all durations publish atomically`() {
        val n = name()
        lateinit var completion: PreparedPowerCompletion
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                completion = prepare(inputs, results)
                assertEquals(AnalysisStatus.AVAILABLE, completion.terminal.status)
                val curve = completion.terminal.curve!!
                assertEquals(200.0, curve.snapshot.durations.first().record!!.meanWatts, 1e-8)
                assertEquals(9, curve.snapshot.durations.size)
                assertTrue(curve.snapshot.durations.drop(1).all { it.record == null })
                assertEquals(SessionPhase.ENDED_LOCALLY, curve.session.phase)
                results.complete(completion.generation, completion.terminal)
                assertThrows(IllegalStateException::class.java) {
                    results.replaceMembership("forbidden", setOf(completion.generation))
                }
                val scalar =
                    AnalysisTerminal(
                        AnalysisStatus.AVAILABLE,
                        Instant.EPOCH,
                        12.0,
                        "W",
                        "synthetic",
                    )
                val legacy = JSONObject(scalar.encode()).apply { remove("curve") }.toString()
                assertEquals(scalar, AnalysisTerminal.decode(legacy))
                val scalarSlot = AnalysisSlot(id, "legacy-scalar", "synthetic")
                val scalarGeneration =
                    results.request(
                        scalarSlot,
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
                            "c",
                        ),
                    )
                results.complete(scalarGeneration, scalar)
                assertEquals(scalar, results.current(scalarSlot)!!.terminal)
                val legacyCurve =
                    completion.terminal.copy(
                        curve =
                            curve.copy(
                                snapshot =
                                    curve.snapshot.copy(
                                        policy =
                                            com.oxywatt.core.power.PowerCurvePolicy(
                                                "power-intervals-v1"
                                            )
                                    ),
                                reservedWork = 19_773_567,
                            )
                    )
                assertEquals(legacyCurve, AnalysisTerminal.decode(legacyCurve.encode()))
                val bad = JSONObject(completion.terminal.encode()).put("codec", 3)
                assertThrows(IllegalArgumentException::class.java) {
                    AnalysisTerminal.decode(bad.toString())
                }
                bad.put("codec", 2).getJSONObject("curve").put("codec", 99)
                assertThrows(IllegalArgumentException::class.java) {
                    AnalysisTerminal.decode(bad.toString())
                }
            }
        }
        AnalysisRepository(context, n).use { results ->
            assertEquals(
                completion.terminal,
                results.current(PowerCurveCalculation.slot(id))!!.terminal,
            )
        }
    }

    @Test
    fun `completion rollback exact retry and newer failure cannot resurrect old curve`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val a = prepare(inputs, results)
                results.beforeCommit = { throw IllegalStateException("fault") }
                assertThrows(IllegalStateException::class.java) {
                    results.complete(a.generation, a.terminal)
                }
                assertNull(results.current(PowerCurveCalculation.slot(id)))
                results.beforeCommit = {}
                results.complete(a.generation, a.terminal)
                results.complete(a.generation, a.terminal)
                val b = prepare(inputs, results)
                assertNull(results.current(PowerCurveCalculation.slot(id)))
                results.complete(a.generation, a.terminal)
                results.complete(
                    b.generation,
                    AnalysisTerminal(AnalysisStatus.FAILED, Instant.EPOCH, reason = "test"),
                )
                assertNull(results.current(PowerCurveCalculation.slot(id)))
                assertEquals(
                    b.generation,
                    results.newest(PowerCurveCalculation.slot(id))!!.generation,
                )
            }
        }
    }

    @Test
    fun `paged upper revision excludes later writes and rollback excludes uncommitted effects`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            val l = fill(inputs, seconds = 300)
            val r = inputs.state().revision
            val first = inputs.replay(r, limit = 1).single()
            val late = l.advanceTime(400_000_000_000)
            val write = inputs.prepareEffect(late, AdmissionPolicy(), r)
            inputs.beforeCheckpoint = { throw IllegalStateException("rollback") }
            assertThrows(IllegalStateException::class.java) { inputs.commit(write) }
            assertEquals(r, inputs.state().revision)
            inputs.beforeCheckpoint = {}
            InputRepository(context, n).use { other -> other.commit(write) }
            val tail = inputs.replay(r, first.revision, first.index)
            assertEquals(256, tail.size)
            val end = inputs.replay(r, tail.last().revision, tail.last().index)
            assertEquals(46, end.size)
            assertTrue((tail + end).all { it.revision <= r })
            assertTrue(inputs.replay(inputs.state().revision, r, Int.MAX_VALUE).isNotEmpty())
            AnalysisRepository(context, n).use { results ->
                val a = prepare(inputs, results)
                assertEquals(
                    200.0,
                    a.terminal.curve!!.snapshot.durations.first().record!!.meanWatts,
                    1e-8,
                )
            }
        }
    }

    @Test
    fun `envelope free expiry and process recovery retain only durable old epoch coverage`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            val l =
                CollectorLifecycle(
                    epoch,
                    LifecyclePolicy("preview-silence-v1", 2_000_000_000),
                    { id },
                )
            val q = BoundedAdmission()
            q.offer(input(0, 0, record(RecordingState.RECORDING)))
            q.offer(input(0, 1, power(200.0)))
            val b = q.takeBatch()!!
            inputs.commit(inputs.prepare(b, l, 0))
            inputs.commit(inputs.prepareEffect(l.advanceTime(2_000_000_000), AdmissionPolicy(), 1))
            val newer =
                CollectorLifecycle(
                    UUID(0, 99),
                    LifecyclePolicy("preview-silence-v1", 2_000_000_000),
                    recovered = inputs.checkpoint(),
                )
            inputs.commit(
                inputs.prepareEffect(newer.advanceTime(100_000_000_000), AdmissionPolicy(), 2)
            )
            AnalysisRepository(context, n).use { results ->
                val c = prepare(inputs, results).terminal.curve!!
                assertEquals(SessionPhase.INTERRUPTED, c.session.phase)
                assertEquals(InterruptionReason.PROCESS_RESTART, c.session.interruptionReason)
                assertTrue(c.snapshot.durations.all { it.record == null })
                assertEquals(1, c.intervalCount)
            }
        }
    }

    @Test
    fun `semantic hash changes with power or policy but not unrelated later session`() {
        val hashes =
            listOf(200.0 to 2_000_000_000L, 201.0 to 2_000_000_000L, 200.0 to 3_000_000_000L).map {
                (w, ttl) ->
                val n = name()
                InputRepository(context, n).use { inputs ->
                    fill(inputs, w, ttl)
                    AnalysisRepository(context, n).use { results ->
                        prepare(inputs, results).terminal.curve!!.inputHash
                    }
                }
            }
        assertEquals(3, hashes.toSet().size)
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val a = prepare(inputs, results).terminal.curve!!.inputHash
                val l =
                    CollectorLifecycle(
                        UUID(0, 99),
                        LifecyclePolicy("preview-silence-v1", 2_000_000_000),
                        recovered = inputs.checkpoint(),
                    )
                inputs.commit(
                    inputs.prepareEffect(
                        l.advanceTime(0),
                        AdmissionPolicy(),
                        inputs.state().revision,
                    )
                )
                assertEquals(a, prepare(inputs, results).terminal.curve!!.inputHash)
            }
        }
    }

    @Test
    fun `hash has independent UTF8 null length vector and JSON order is irrelevant`() {
        val hash = PowerReplayHash()
        hash.token("ż")
        hash.token(null)
        hash.token("7")
        assertEquals(
            "b8cfa110cd59649313a8371909daec3b176bf37bcfb2bb1093324edfc5ab928d",
            hash.finish(),
        )
        val a =
            PowerReplayHash()
                .apply { document(JSONObject().put("z", 1).put("a", JSONObject.NULL)) }
                .finish()
        val b =
            PowerReplayHash()
                .apply { document(JSONObject().put("a", JSONObject.NULL).put("z", 1)) }
                .finish()
        assertEquals(a, b)
    }

    @Test
    fun `unsupported policy and cancellation persist failed newest generation`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val good = prepare(inputs, results)
                results.complete(good.generation, good.terminal)
                var calls = 0
                val cancelled =
                    PowerCurveCalculation(inputs, results).prepare(
                        id,
                        "SYNTHETIC:constant-v1:seed=0:sha256=fixture-v1",
                        "test",
                    ) {
                        ++calls > 2
                    }
                assertEquals(AnalysisStatus.FAILED, cancelled.terminal.status)
                assertEquals("CANCELLED", cancelled.terminal.reason)
                results.complete(cancelled.generation, cancelled.terminal)
                assertNull(results.current(PowerCurveCalculation.slot(id)))
                android.database.sqlite.SQLiteDatabase.openDatabase(
                        context.getDatabasePath(n).path,
                        null,
                        0,
                    )
                    .use { db ->
                        db.rawQuery("SELECT batch,item,data FROM effects", null).use { c ->
                            while (c.moveToNext()) {
                                val j = JSONObject(c.getString(2))
                                val checkpoint =
                                    j.getJSONObject("update").getJSONObject("checkpoint")
                                checkpoint.getJSONObject("policy").put("version", "unknown")
                                j.getJSONObject("update").getJSONArray("sessions").let { a ->
                                    for (k in 0 until a.length()) a.getJSONObject(k)
                                        .getJSONObject("policy")
                                        .put("version", "unknown")
                                }
                                db.execSQL(
                                    "UPDATE effects SET data=? WHERE batch=? AND item=?",
                                    arrayOf(j.toString(), c.getString(0), c.getInt(1)),
                                )
                            }
                        }
                    }
                val failed = prepare(inputs, results)
                assertEquals(AnalysisStatus.FAILED, failed.terminal.status)
                results.complete(failed.generation, failed.terminal)
                assertNull(results.current(PowerCurveCalculation.slot(id)))
            }
        }
    }

    @Test
    fun `corrupt effect envelope cannot bypass canonical observation cross check`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(n).path,
                    null,
                    0,
                )
                .use { db ->
                    db.rawQuery("SELECT batch,item,data FROM effects WHERE item=2", null).use { c ->
                        assertTrue(c.moveToFirst())
                        val j = JSONObject(c.getString(2))
                        j.getJSONObject("update")
                            .getJSONObject("envelope")
                            .getJSONObject("input")
                            .getJSONObject("payload")
                            .put("value", "0x1.0p0")
                        db.execSQL(
                            "UPDATE effects SET data=? WHERE batch=? AND item=?",
                            arrayOf(j.toString(), c.getString(0), c.getInt(1)),
                        )
                    }
                }
            assertThrows(IllegalStateException::class.java) {
                inputs.replay(inputs.state().revision)
            }
        }
    }

    @Test
    fun `unrelated later batch loss does not change ended session semantic hash`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val before = prepare(inputs, results).terminal.curve!!.inputHash
                android.database.sqlite.SQLiteDatabase.openDatabase(
                        context.getDatabasePath(n).path,
                        null,
                        0,
                    )
                    .use { db ->
                        val first = input(100, 100, power(0.0)).stamp
                        val m =
                            CollectorCodec.metadata(
                                AdmissionPolicy(),
                                AdmissionLoss(first, first, 1, setOf(AdmissionLossReason.CAPACITY)),
                                InterruptionReason.CONTROL_OVERFLOW,
                            )
                        db.execSQL("UPDATE batches SET document=?", arrayOf(m.toString()))
                    }
                assertEquals(before, prepare(inputs, results).terminal.curve!!.inputHash)
            }
        }
    }

    @Test
    fun `request storage retry freezes request identity and provenance before allocation`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            AnalysisRepository(context, n).use { results ->
                val calc = PowerCurveCalculation(inputs, results)
                var frozen: StoredAnalysis? = null
                results.beforeCommit = {
                    frozen = results.newest(PowerCurveCalculation.slot(id))
                    throw IllegalStateException("request fault")
                }
                assertThrows(IllegalStateException::class.java) {
                    calc.prepare(id, "SYNTHETIC:retry-v1", "test")
                }
                assertTrue(calc.hasPreparedRequest)
                assertNull(results.newest(PowerCurveCalculation.slot(id)))
                results.beforeCommit = {}
                val retry = calc.prepare(id, "SYNTHETIC:retry-v1", "test")
                assertEquals(frozen!!.provenance, results.run(retry.generation)!!.provenance)
                assertEquals(frozen!!.generation, retry.generation)
                assertFalse(calc.hasPreparedRequest)
                results.complete(retry.generation, retry.terminal)
            }
        }
    }

    @Test
    fun `curve terminal cannot be attributed to another sessions generation`() {
        val n = name()
        InputRepository(context, n).use { inputs ->
            fill(inputs)
            val otherId = UUID(0, 99)
            val newEpoch = UUID(0, 100)
            val lifecycle =
                CollectorLifecycle(
                    newEpoch,
                    LifecyclePolicy("preview-silence-v1", 2_000_000_000),
                    { otherId },
                    inputs.checkpoint(),
                )
            val queue = BoundedAdmission()
            queue.offer(
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, newEpoch, 0, 0),
                    SourceRef("simulated", "1"),
                    "canonical-v1",
                    record(RecordingState.RECORDING),
                )
            )
            inputs.commit(inputs.prepare(queue.takeBatch()!!, lifecycle, inputs.state().revision))
            AnalysisRepository(context, n).use { results ->
                val original = prepare(inputs, results)
                val other =
                    results.request(
                        PowerCurveCalculation.slot(otherId),
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
                            "c",
                        ),
                    )
                assertThrows(IllegalStateException::class.java) {
                    results.complete(other, original.terminal)
                }
                assertNull(results.current(PowerCurveCalculation.slot(otherId)))
            }
        }
    }
}
