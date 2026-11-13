package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.*
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

/** Test-owned canonical traces with real-shaped metadata, never private device data. */
internal class FieldFixture(
    val context: Context,
    val name: String = "field-${UUID.randomUUID()}.db",
) : AutoCloseable {
    val epoch = UUID(0, 801)
    val ids = listOf(UUID(0, 802), UUID(0, 803))
    val metadata =
        JSONObject()
            .put("version", 1)
            .put("origin", "real-karoo-diagnostic")
            .put("appVersion", "test-fixture")
            .put("codeRevision", "test-only")
            .put("dirty", true)
            .put("epoch", epoch.toString())
            .put("ordering", "karoo-diagnostic-order-v1")
            .put("retry", "karoo-diagnostic-retry-v1")
            .put("firstRetryNanos", 1_000_000_000L)
            .put("subsequentRetryNanos", 5_000_000_000L)
            .put("storage", "karoo-diagnostic-storage-v1")
            .put("maximumBytes", 32L * 1024 * 1024)
            .put("silence", "karoo-diagnostic-silence-v1")
            .put("silenceNanos", 5_000_000_000L)
            .toString()
    val inputs = InputRepository(context, name, acquisition = metadata)
    val results = AnalysisRepository(context, name)

    init {
        var next = 0
        val lifecycle =
            CollectorLifecycle(
                epoch,
                LifecyclePolicy("karoo-diagnostic-silence-v1", 5_000_000_000),
                { ids[next++] },
            )
        val q = BoundedAdmission()
        var sequence = 0L
        repeat(2) { ride ->
            fun offer(t: Long, payload: InputPayload) {
                q.offer(
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
            val base = ride * 10L
            offer(base, InputPayload.Recording(RecordingState.IDLE))
            offer(base, InputPayload.Recording(RecordingState.RECORDING))
            repeat(6) {
                offer(base + it, InputPayload.MetricReading(Metric.POWER_W, 100.0 + ride * 50))
            }
            offer(base + 6, InputPayload.Recording(RecordingState.IDLE))
            val batch = q.takeBatch()!!
            inputs.commit(inputs.prepare(batch, lifecycle, inputs.state().revision))
            q.committed(batch)
        }
    }

    fun calculate(id: UUID = ids.first()): StoredAnalysis {
        val completion = PowerCurveCalculation(inputs, results).prepareReal(id, "test-field")
        results.complete(completion.generation, completion.terminal)
        return results.newest(PowerCurveCalculation.realSlot(id))!!
    }

    fun request(): Long {
        val old = results.newest(PowerCurveCalculation.realSlot(ids.first()))!!
        val p = JSONObject(old.provenance)
        val fields = p.getJSONArray("fields")
        return results.request(
            old.slot,
            UUID.randomUUID(),
            AnalysisProvenance(
                fields.getString(0),
                fields.getString(1),
                fields.getString(2),
                fields.getString(3),
                fields.getString(4),
                fields.getString(5),
                fields.getString(6),
                fields.getString(7),
                Instant.EPOCH,
                "new-test-code",
            ),
        )
    }

    fun sql(command: String) {
        android.database.sqlite.SQLiteDatabase.openDatabase(
                context.getDatabasePath(name).path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            )
            .use { it.execSQL(command) }
    }

    override fun close() {
        results.close()
        inputs.close()
        context.deleteDatabase(name)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class SavedFieldControllerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings = SavedFieldSettings(context)

    @After
    fun clear() {
        settings.write(null)
    }

    @Test
    fun `explicit older saved session survives restart independently of latest session without calculation`() {
        FieldFixture(context).use { f ->
            val result = f.calculate()
            val revision = f.inputs.state().revision
            val binding = SavedFieldBinding(f.ids.first(), 5, FieldAppearance.DARK)
            val controller = SavedFieldController(f.inputs, f.results, settings, { 0 })
            controller.bind(binding)
            controller.pump(false, null)
            assertEquals(SavedFieldStatus.SAVED, controller.snapshot().status)
            assertEquals(binding, controller.snapshot().binding)
            assertEquals(100.0, controller.snapshot().points.first().watts!!, 1e-9)
            assertEquals(result.generation, controller.snapshot().generation)
            val restarted =
                SavedFieldController(f.inputs, f.results, SavedFieldSettings(context), { 0 })
            assertEquals(SavedFieldStatus.LOADING, restarted.snapshot().status)
            assertTrue(restarted.snapshot().points.isEmpty())
            restarted.pump(false, null)
            assertEquals(controller.snapshot(), restarted.snapshot())
            assertEquals(revision, f.inputs.state().revision)
            assertEquals(f.ids.last(), f.inputs.checkpoint()!!.latestSession!!.id)
            assertNull(f.results.newest(PowerCurveCalculation.realSlot(f.ids.last())))
        }
    }

    @Test
    fun `newest pending failed and unavailable suppress old numbers and preserve exact binding`() {
        FieldFixture(context).use { f ->
            f.calculate()
            var now = 0L
            val c = SavedFieldController(f.inputs, f.results, settings, { now })
            val binding = SavedFieldBinding(f.ids.first(), 5)
            c.bind(binding)
            c.pump(true, null)
            assertEquals(SavedFieldStatus.SAVED, c.snapshot().status)
            for (status in
                listOf(AnalysisStatus.PENDING, AnalysisStatus.FAILED, AnalysisStatus.UNAVAILABLE)) {
                val generation = f.request()
                if (status != AnalysisStatus.PENDING)
                    f.results.complete(
                        generation,
                        AnalysisTerminal(status, Instant.EPOCH, reason = "test-only-reason"),
                    )
                now += 5_000_000_000
                c.pump(true, null)
                assertEquals(
                    when (status) {
                        AnalysisStatus.PENDING -> SavedFieldStatus.PENDING
                        AnalysisStatus.FAILED -> SavedFieldStatus.FAILED
                        else -> SavedFieldStatus.UNAVAILABLE
                    },
                    c.snapshot().status,
                )
                assertTrue(c.snapshot().points.isEmpty())
                assertEquals(binding, c.snapshot().binding)
                assertEquals(binding, settings.read())
            }
        }
    }

    @Test
    fun `deleted bound session and foreign slot pointer never substitute another saved session`() {
        FieldFixture(context).use { f ->
            f.calculate()
            val other = f.calculate(f.ids.last())
            var now = 0L
            val c = SavedFieldController(f.inputs, f.results, settings, { now })
            c.bind(SavedFieldBinding(f.ids.first(), 5))
            c.pump(true, null)
            f.sql(
                "UPDATE analysis_slots SET newest=${other.generation} WHERE session='${f.ids.first()}'"
            )
            now += 5_000_000_000
            c.pump(true, null)
            assertEquals(SavedFieldStatus.INVALID, c.snapshot().status)
            assertTrue(c.snapshot().points.isEmpty())
            f.sql("DELETE FROM sessions WHERE id='${f.ids.first()}'")
            now += 5_000_000_000
            c.pump(true, null)
            assertEquals(SavedFieldStatus.REMOVED, c.snapshot().status)
            assertEquals(f.ids.first(), settings.read()!!.session)
        }
    }

    @Test
    fun `stale checks are five second bounded and cease with no subscribers while explicit clear still works`() {
        FieldFixture(context).use { f ->
            f.calculate()
            var now = 0L
            val c = SavedFieldController(f.inputs, f.results, settings, { now })
            c.bind(SavedFieldBinding(f.ids.first(), 5))
            c.pump(true, null)
            val before = c.snapshot()
            f.sql("DROP TABLE analysis_slots")
            now = 4_999_999_999
            repeat(50) { c.pump(true, null) }
            assertEquals(before, c.snapshot())
            now = 5_000_000_000
            c.pump(false, null)
            assertEquals(before, c.snapshot())
            c.pump(true, null)
            assertEquals(SavedFieldStatus.READ_ERROR, c.snapshot().status)
            assertTrue(c.snapshot().points.isEmpty())
            c.clear()
            c.pump(false, null)
            assertEquals(SavedFieldStatus.SETUP, c.snapshot().status)
            assertNull(settings.read())
        }
    }

    @Test
    fun `refused candidate preserves bound session date and generation with a separate configuration error`() {
        FieldFixture(context).use { f ->
            f.calculate()
            val c = SavedFieldController(f.inputs, f.results, settings, { 0 })
            c.bind(SavedFieldBinding(f.ids.first(), 5))
            c.pump(false, null)
            val before = c.snapshot()
            c.bind(SavedFieldBinding(f.ids.last(), 5))
            c.pump(false, null)
            assertEquals(before, c.snapshot().copy(bindingError = null))
            assertEquals(SavedFieldStatus.UNAVAILABLE, c.snapshot().bindingError)
            val startup = SavedFieldController(f.inputs, f.results, settings, { 0 })
            startup.bind(SavedFieldBinding(f.ids.last(), 5))
            startup.pump(false, null)
            assertEquals(f.ids.first(), startup.snapshot().binding!!.session)
            startup.pump(false, null)
            assertEquals(before, startup.snapshot().copy(bindingError = null))
        }
    }

    @Test
    fun `uncomputed selection cannot replace a binding and coalesced clear discards an unstarted bind`() {
        FieldFixture(context).use { f ->
            f.calculate()
            val c = SavedFieldController(f.inputs, f.results, settings, { 0 })
            val binding = SavedFieldBinding(f.ids.first(), 300)
            c.bind(binding)
            c.pump(false, null)
            assertNull(c.snapshot().points.single { it.seconds == 300 }.watts)
            c.bind(SavedFieldBinding(f.ids.last(), 5))
            c.pump(false, null)
            assertEquals(SavedFieldStatus.SAVED, c.snapshot().status)
            assertEquals(SavedFieldStatus.UNAVAILABLE, c.snapshot().bindingError)
            assertEquals(binding, settings.read())
            c.bind(binding)
            c.clear()
            c.pump(false, null)
            assertNull(settings.read())
            assertTrue(c.snapshot().points.isEmpty())
            assertNull(f.results.newest(PowerCurveCalculation.realSlot(f.ids.last())))
        }
    }
}
