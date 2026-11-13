package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.diagnostic.persistence.*
import java.time.Instant
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
class PowerHistoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings
        get() = HistorySettings(context)

    @After
    fun clear() {
        context.getSharedPreferences("power-history", 0).edit().clear().commit()
    }

    private fun finish(c: HistoryController) {
        repeat(50) { c.pump(true, null) }
    }

    @Test
    fun `history reads older and newer saved results chronologically without generating analyses`() {
        FieldFixture(context).use { f ->
            val a = f.calculate()
            val b = f.calculate(f.ids.last())
            val revision = f.inputs.state().revision
            val c = HistoryController(f.inputs, f.results, settings, { 0 }, { Instant.EPOCH })
            c.refresh(HistoryConfig(5))
            finish(c)
            val s = c.snapshot()
            assertEquals(HistoryStatus.READY, s.status)
            assertEquals(f.ids, s.entries.map { it.session })
            assertEquals(listOf(100.0, 150.0), s.entries.map { it.watts })
            assertEquals(listOf(a.generation, b.generation), s.entries.map { it.generation })
            assertEquals(Instant.EPOCH, s.refreshedAt)
            assertEquals(revision, f.inputs.state().revision)
            assertEquals(b.generation, f.results.newest(b.slot)!!.generation)
        }
    }

    @Test
    fun `newer pending generation during refresh suppresses older saved number`() {
        FieldFixture(context).use { f ->
            f.calculate()
            f.calculate(f.ids.last())
            var now = 0L
            val c = HistoryController(f.inputs, f.results, settings, { now })
            c.refresh(HistoryConfig(5))
            finish(c)
            now = 5_000_000_000L
            c.refresh()
            c.pump(true, null)
            c.pump(true, null)
            val newer = f.request()
            finish(c)
            val entry = c.snapshot().entries.single { it.session == f.ids.first() }
            assertEquals(HistoryReason.PENDING, entry.reason)
            assertNull(entry.watts)
            assertEquals(newer, entry.generation)
            assertEquals(150.0, c.snapshot().entries.last().watts!!, 0.0)
        }
    }

    @Test
    fun `enumeration bound precedes filtering and chronology ignores persistence order`() {
        FieldFixture(context).use { f ->
            val original = f.inputs.session(f.ids.first())!!
            android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(f.name).path,
                    null,
                    0,
                )
                .use { db ->
                    repeat(22) { i ->
                        val id = java.util.UUID(0, 1000L + i)
                        val date = Instant.EPOCH.plusSeconds((21 - i).toLong())
                        val session =
                            original.copy(
                                id = id,
                                firstReceipt = original.firstReceipt.copy(utc = date),
                                lastReceipt = original.lastReceipt.copy(utc = date.plusSeconds(6)),
                            )
                        db.execSQL(
                            "INSERT INTO sessions(id,snapshot) VALUES(?,?)",
                            arrayOf(id.toString(), CollectorCodec.session(session).toString()),
                        )
                    }
                }
            val c = HistoryController(f.inputs, f.results, settings, { 0 })
            c.refresh(HistoryConfig(5))
            finish(c)
            val snapshot = c.snapshot()
            assertEquals(HistoryStatus.READY, snapshot.status)
            assertTrue(snapshot.truncated)
            assertEquals(20, snapshot.entries.size)
            assertEquals(
                (1021L downTo 1002L).map { java.util.UUID(0, it) },
                snapshot.entries.map { it.session },
            )
            assertTrue(
                snapshot.entries.all { it.reason == HistoryReason.NO_RESULT && it.watts == null }
            )
        }
    }

    @Test
    fun `incomplete and foreign provenance cannot join numeric history and read failure clears success`() {
        FieldFixture(context).use { f ->
            f.calculate()
            f.calculate(f.ids.last())
            val original = f.inputs.session(f.ids.last())!!
            val fragment =
                original.copy(
                    origin = com.oxywatt.core.collection.SessionOrigin.COLD_ATTACH,
                    ambiguityGroupId = java.util.UUID(0, 9),
                )
            android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(f.name).path,
                    null,
                    0,
                )
                .use { db ->
                    db.execSQL(
                        "UPDATE sessions SET snapshot=? WHERE id=?",
                        arrayOf(
                            CollectorCodec.session(fragment).toString(),
                            fragment.id.toString(),
                        ),
                    )
                }
            var now = 0L
            val c = HistoryController(f.inputs, f.results, settings, { now })
            c.refresh(HistoryConfig(5))
            finish(c)
            assertEquals(HistoryReason.INCOMPLETE, c.snapshot().entries.last().reason)
            assertEquals(100.0, c.snapshot().entries.first().watts!!, 0.0)
            f.sql(
                "UPDATE analysis_runs SET provenance=replace(provenance,'power-range-means-v2','foreign-model')"
            )
            now += 5_000_000_000L
            finish(c)
            assertEquals(HistoryReason.INCOMPATIBLE, c.snapshot().entries.first().reason)
            f.sql("DROP TABLE analysis_slots")
            now += 5_000_000_000L
            finish(c)
            assertEquals(HistoryStatus.READ_ERROR, c.snapshot().status)
            assertTrue(c.snapshot().entries.isEmpty())
        }
    }

    @Test
    fun `five second starts coalesce configuration and hidden subscribers cancel without reading`() {
        FieldFixture(context).use { f ->
            f.calculate()
            f.calculate(f.ids.last())
            var now = 0L
            val c = HistoryController(f.inputs, f.results, settings, { now })
            c.refresh(HistoryConfig(5))
            finish(c)
            c.refresh(HistoryConfig(30))
            c.refresh(HistoryConfig(60, FieldAppearance.DARK))
            finish(c)
            assertEquals(HistoryStatus.LOADING, c.snapshot().status)
            assertEquals(HistoryConfig(60, FieldAppearance.DARK), settings.read())
            now = 4_999_999_999L
            finish(c)
            assertFalse(c.hasWork)
            now++
            c.pump(true, null)
            assertTrue(c.hasWork)
            c.pump(false, null)
            assertFalse(c.hasWork)
            f.sql("DROP TABLE sessions")
            now += 5_000_000_000L
            repeat(10) { c.pump(false, null) }
            assertEquals(HistoryStatus.LOADING, c.snapshot().status)
            c.pump(true, null)
            assertEquals(HistoryStatus.READ_ERROR, c.snapshot().status)
        }
    }

    @Test
    fun `invalidated success disappears immediately and removal cannot retarget another session`() {
        FieldFixture(context).use { f ->
            val result = f.calculate()
            f.calculate(f.ids.last())
            var now = 0L
            val c = HistoryController(f.inputs, f.results, settings, { now })
            c.refresh(HistoryConfig(5))
            finish(c)
            c.pump(true, null, f.ids.first())
            assertEquals(HistoryStatus.LOADING, c.snapshot().status)
            assertTrue(c.snapshot().entries.isEmpty())
            now += 5_000_000_000L
            c.pump(true, null)
            c.pump(true, null)
            f.sql("DELETE FROM sessions WHERE id='${f.ids.first()}'")
            finish(c)
            assertEquals(HistoryReason.REMOVED, c.snapshot().entries.first().reason)
            assertNull(c.snapshot().entries.first().watts)
            assertEquals(result.generation, f.results.newest(result.slot)!!.generation)
        }
    }

    @Test
    fun `settings restore independently of single binding and unknown versions require explicit replacement`() {
        settings.write(HistoryConfig(5, FieldAppearance.DARK))
        assertEquals(HistoryConfig(5, FieldAppearance.DARK), HistorySettings(context).read())
        context
            .getSharedPreferences("power-history", 0)
            .edit()
            .putString("config", "{\"version\":99}")
            .commit()
        FieldFixture(context).use { f ->
            val saved = f.calculate()
            val c = HistoryController(f.inputs, f.results, settings, { 0 })
            c.pump(true, saved)
            assertEquals(HistoryStatus.SETTINGS_ERROR, c.snapshot().status)
            c.refresh()
            finish(c)
            assertEquals(HistoryStatus.SETTINGS_ERROR, c.snapshot().status)
            c.refresh(HistoryConfig(5))
            finish(c)
            assertEquals(HistoryStatus.READY, c.snapshot().status)
        }
    }

    @Test
    fun `all nine literal stored durations preserve zero missing and limited distinctions`() {
        FieldFixture(context).use { f ->
            val old = f.calculate()
            val curve = old.terminal!!.curve!!
            val expected = listOf(0.0, 30.25, 60.5, 180.75, 300.0, 600.0, 1200.0, 1800.0, 3600.0)
            val durations = com.oxywatt.core.power.PowerCurveEngine.DURATIONS
            val literal =
                curve.copy(
                    snapshot =
                        curve.snapshot.copy(
                            durations =
                                durations.mapIndexed { index, duration ->
                                    com.oxywatt.core.power.PowerDurationResult(
                                        duration,
                                        com.oxywatt.core.power.PowerRecord(
                                            duration,
                                            expected[index],
                                            0,
                                            duration * 1_000_000_000L,
                                            curve.session.id,
                                            curve.session.epochId,
                                            0,
                                        ),
                                        null,
                                    )
                                }
                        )
                )
            val newest = f.request()
            f.results.complete(
                newest,
                AnalysisTerminal(AnalysisStatus.AVAILABLE, Instant.EPOCH, curve = literal),
            )
            for ((index, duration) in durations.withIndex()) {
                val c = HistoryController(f.inputs, f.results, settings, { 0 })
                c.refresh(HistoryConfig(duration))
                finish(c)
                val e = c.snapshot().entries.first()
                assertEquals(HistoryReason.VALUE, e.reason)
                assertEquals(expected[index], e.watts!!, 0.0)
                assertEquals(newest, e.generation)
            }
            val limited = f.request()
            f.results.complete(
                limited,
                AnalysisTerminal(
                    AnalysisStatus.AVAILABLE,
                    Instant.EPOCH,
                    curve = literal.copy(snapshot = literal.snapshot.copy(resourceLimited = true)),
                ),
            )
            val c = HistoryController(f.inputs, f.results, settings, { 0 })
            c.refresh(HistoryConfig(5))
            finish(c)
            assertEquals(HistoryReason.LIMITED, c.snapshot().entries.first().reason)
            assertNull(c.snapshot().entries.first().watts)
        }
    }

    @Test
    fun `newest failure or unavailable never revives older result and terminal event clears success`() {
        FieldFixture(context).use { f ->
            f.calculate()
            var now = 0L
            val c = HistoryController(f.inputs, f.results, settings, { now })
            c.refresh(HistoryConfig(5))
            finish(c)
            for (status in listOf(AnalysisStatus.FAILED, AnalysisStatus.UNAVAILABLE)) {
                val generation = f.request()
                f.results.complete(
                    generation,
                    AnalysisTerminal(status, Instant.EPOCH, reason = "synthetic refusal"),
                )
                val latest = f.results.newest(PowerCurveCalculation.realSlot(f.ids.first()))!!
                c.pump(true, latest)
                assertTrue(c.snapshot().entries.isEmpty())
                now += 5_000_000_000L
                finish(c)
                assertEquals(
                    if (status == AnalysisStatus.FAILED) HistoryReason.FAILED
                    else HistoryReason.UNAVAILABLE,
                    c.snapshot().entries.first().reason,
                )
                assertNull(c.snapshot().entries.first().watts)
            }
        }
    }

    @Test
    fun `ongoing calculation must not starve history refresh or expose old numbers`() {
        FieldFixture(context).use { f ->
            f.calculate()
            f.calculate(f.ids.last())
            val c = HistoryController(f.inputs, f.results, settings, { 0 })
            c.refresh(HistoryConfig(5))
            repeat(50) { c.pump(true, null, f.ids.first()) }
            assertEquals(HistoryStatus.READY, c.snapshot().status)
            assertNull(c.snapshot().entries.first().watts)
            assertEquals(150.0, c.snapshot().entries.last().watts!!, 0.0)
        }
    }

    @Test
    fun `typed curve policy incompatible with provenance cannot enter the supported series`() {
        FieldFixture(context).use { f ->
            val old = f.calculate()
            val original = old.terminal!!.curve!!
            val foreign =
                original.copy(
                    snapshot =
                        original.snapshot.copy(
                            policy =
                                com.oxywatt.core.power.PowerCurvePolicy("foreign-policy", 16384)
                        )
                )
            val generation = f.request()
            f.results.complete(
                generation,
                AnalysisTerminal(AnalysisStatus.AVAILABLE, Instant.EPOCH, curve = foreign),
            )
            val c = HistoryController(f.inputs, f.results, settings, { 0 })
            c.refresh(HistoryConfig(5))
            finish(c)
            assertEquals(HistoryReason.INCOMPATIBLE, c.snapshot().entries.first().reason)
            assertNull(c.snapshot().entries.first().watts)
        }
    }
}
