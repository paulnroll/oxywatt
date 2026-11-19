package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.diagnostic.persistence.*
import java.io.File
import java.time.Instant
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

internal fun fitArchiveFixture(
    context: Context,
    power: Double = 100.0,
    seconds: Int = 6,
    gap: Boolean = false,
): File {
    val root = File(context.cacheDir, "fit-${UUID.randomUUID()}").apply { mkdirs() }
    val base = Instant.parse("2020-01-02T10:00:00Z")
    var index = 0
    val rows =
        StringBuilder(
            "message_index\tfile_timestamp\toffset_nanos\tkind\tvalue\treason\ttimer_type\n"
        )
    fun row(t: Int, kind: String, value: String, reason: String = "-", timer: String = "-") {
        rows.append(
            "${index++}\t${base.plusSeconds(t.toLong())}\t${t * 1_000_000_000L}\t$kind\t$value\t$reason\t$timer\n"
        )
    }
    row(0, "STATE", "RECORDING", timer = "start")
    repeat(seconds) { t ->
        if (!gap || t !in 2..4) {
            row(t, "POWER_W", power.toString())
            row(t, "HEART_RATE_BPM", "120")
            row(t, "CADENCE_RPM", "80")
        }
    }
    row(seconds, "STATE", "PAUSED", timer = "stop_all")
    val bytes = rows.toString().toByteArray()
    File(root, "observations.tsv").writeBytes(bytes)
    val hash =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }
    File(root, "manifest.json")
        .writeText(
            JSONObject()
                .put("schema", "fit-history-archive-v1")
                .put("decoder", "fitdecode")
                .put("decoder_version", "0.11.0")
                .put("normalization_version", "fit-normalization-v1")
                .put("provenance", "private-owner-input")
                .put("timestamp_policy", "ordered-original-file-time-v1")
                .put("scheduler_policy", "timers-before-input-v1")
                .put("input_sha256", "a".repeat(64))
                .put("projection_sha256", hash)
                .put("projected_rows", index)
                .put("file_origin", base.toString())
                .put("end_offset_nanos", seconds * 1_000_000_000L)
                .put("session_start", base.toString())
                .put("session_end", base.plusSeconds(seconds.toLong()).toString())
                .put("completion", "file-session-activity-stop-v1")
                .toString()
        )
    return root
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class FitHistoryImportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun archive(power: Double = 100.0, seconds: Int = 6, gap: Boolean = false) =
        fitArchiveFixture(context, power, seconds, gap)

    @Test
    fun `real FIT is saved with original date and exact literal curve without live relabelling`() {
        val name = "import-${UUID.randomUUID()}.db"
        val root = archive()
        val id = FitHistoryImporter(context, name).importArchive(root)
        InputRepository(context, name).use { inputs ->
            AnalysisRepository(context, name).use { analyses ->
                val saved =
                    analyses.newest(
                        AnalysisSlot(id, "observed-power-curve", "real-fit-file-power-hold-1s-v1")
                    )!!
                val curve = saved.terminal!!.curve!!
                assertEquals("fit-file", curve.session.source.adapterId)
                assertEquals(100.0, curve.snapshot.durations.first().record!!.meanWatts, 0.0)
                assertEquals(0L, curve.snapshot.durations.first().record!!.startNanos)
                assertEquals(5_000_000_000L, curve.snapshot.durations.first().record!!.endNanos)
                assertTrue(curve.snapshot.durations.drop(1).all { it.record == null })
                assertFalse(curve.session.trendEligible)
                assertEquals(Instant.parse("2020-01-02T10:00:00Z"), curve.session.firstReceipt.utc)
                assertNull(analyses.newest(PowerCurveCalculation.realSlot(id)))
                assertThrows(IllegalArgumentException::class.java) {
                    PowerCurveCalculation(inputs, analyses).prepareReal(id, "test")
                }
                assertThrows(IllegalArgumentException::class.java) {
                    PowerCurveCalculation(inputs, analyses)
                        .prepare(id, "SYNTHETIC:refused-fit", "test")
                }
                val encoded = saved.terminal!!.encode()
                assertEquals(saved.terminal, AnalysisTerminal.decode(encoded))
                val corrupt = JSONObject(encoded)
                corrupt.getJSONObject("curve").put("acquisitionOrigin", "real-karoo-diagnostic")
                assertThrows(IllegalArgumentException::class.java) {
                    AnalysisTerminal.decode(corrupt.toString())
                }
                corrupt.getJSONObject("curve").put("codec", 3)
                assertThrows(IllegalArgumentException::class.java) {
                    AnalysisTerminal.decode(corrupt.toString())
                }
                assertNull(analyses.newest(PowerCurveCalculation.realSlot(id)))
                assertEquals(20, inputs.observations().size)
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `duplicate file import preserves session inputs and result generation`() {
        val name = "duplicate-${UUID.randomUUID()}.db"
        val root = archive(0.0)
        val importer = FitHistoryImporter(context, name)
        val id = importer.importArchive(root)
        val revision = InputRepository(context, name).use { it.state().revision }
        assertEquals(id, importer.importArchive(root))
        InputRepository(context, name).use { assertEquals(revision, it.state().revision) }
        AnalysisRepository(context, name).use {
            val saved =
                it.newest(
                    AnalysisSlot(id, "observed-power-curve", "real-fit-file-power-hold-1s-v1")
                )!!
            assertEquals(1L, saved.generation)
            assertEquals(
                0.0,
                saved.terminal!!.curve!!.snapshot.durations.first().record!!.meanWatts,
                0.0,
            )
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `record gaps cannot be held as fabricated continuous power`() {
        val name = "gap-${UUID.randomUUID()}.db"
        val id = FitHistoryImporter(context, name).importArchive(archive(seconds = 9, gap = true))
        AnalysisRepository(context, name).use {
            val saved =
                it.newest(
                    AnalysisSlot(id, "observed-power-curve", "real-fit-file-power-hold-1s-v1")
                )!!
            assertNull(saved.terminal!!.curve!!.snapshot.durations.first().record)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `native FULL rolls back all imported batches without changing prior checkpoint`() {
        val name = "full-${UUID.randomUUID()}.db"
        val oldId = FitHistoryImporter(context, name).importArchive(archive())
        val before = InputRepository(context, name).use { it.state() }
        val prefix = InputRepository(context, name).use { it.observations() }
        val saved = AnalysisRepository(context, name).use { it.newest(FitSource.slot(oldId)) }
        val incoming = archive(seconds = 100)
        val manifest = File(incoming, "manifest.json")
        manifest.writeText(
            JSONObject(manifest.readText()).put("input_sha256", "b".repeat(64)).toString()
        )
        var commits = 0
        assertThrows(android.database.sqlite.SQLiteFullException::class.java) {
            FitHistoryImporter(context, name) { repo ->
                    repo.beforeCheckpoint = {
                        if (++commits == 1) repo.limitStorageForTest(1)
                    }
                }
                .importArchive(incoming)
        }
        assertTrue(commits >= 1)
        InputRepository(context, name).use {
            assertEquals(before, it.state())
            assertEquals(prefix, it.observations())
            assertEquals(listOf(oldId), it.recentSessions().sessions.map { s -> s.id })
        }
        AnalysisRepository(context, name).use {
            assertEquals(saved, it.newest(FitSource.slot(oldId)))
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `irregular file intervals integrate to literal 200 watts without declared boundary padding`() {
        val name = "irregular-${UUID.randomUUID()}.db"
        val root = archive()
        FitScript()
            .apply {
                state(0.0, true)
                power(0.0, 100.0)
                power(0.5, 300.0)
                power(1.5, 100.0)
                power(2.5, 300.0)
                power(3.5, 100.0)
                power(4.5, 300.0)
                state(5.0, false)
            }
            .write(root, leading = 10, trailing = 20)
        val id = FitHistoryImporter(context, name).importArchive(root)
        InputRepository(context, name).use { inputs ->
            AnalysisRepository(context, name).use { results ->
                val saved = results.newest(FitSource.slot(id))!!
                val record = saved.terminal!!.curve!!.snapshot.durations.first().record!!
                assertEquals(200.0, record.meanWatts, 1e-9)
                assertEquals(0L, record.startNanos)
                assertEquals(5_000_000_000L, record.endNanos)
                val controller = HistoryController(inputs, results, HistorySettings(context), { 0 })
                controller.refresh(HistoryConfig(5, source = HistorySource.FIT))
                repeat(40) { controller.pump(true, null) }
                val view = controller.snapshot()
                assertEquals(200.0, view.entries.single().watts!!, 1e-9)
                assertEquals(Instant.parse("2020-01-02T09:59:50Z"), view.entries.single().date)
                assertTrue(historySummary(context, view).contains("Imported FIT"))
                val live = HistoryController(inputs, results, HistorySettings(context), { 0 })
                live.refresh(HistoryConfig(5, source = HistorySource.LIVE))
                repeat(40) { live.pump(true, null) }
                assertTrue(live.snapshot().entries.isEmpty())
                assertEquals(1, live.snapshot().otherSources)
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `pauses and missing readings break windows and newer failed FIT analysis never falls back`() {
        val name = "pause-${UUID.randomUUID()}.db"
        val root = archive()
        FitScript()
            .apply {
                state(0.0, true)
                power(0.0, 600.0)
                power(1.0, 600.0)
                state(2.0, false)
                state(7.0, true)
                power(7.0, null)
                power(8.0, 100.0)
                power(9.0, 100.0)
                power(10.0, 100.0)
                power(11.0, 100.0)
                power(12.0, 100.0)
                state(13.0, false)
            }
            .write(root)
        val id = FitHistoryImporter(context, name).importArchive(root)
        InputRepository(context, name).use { inputs ->
            AnalysisRepository(context, name).use { results ->
                val old = results.newest(FitSource.slot(id))!!
                val record = old.terminal!!.curve!!.snapshot.durations.first().record!!
                assertEquals(100.0, record.meanWatts, 0.0)
                assertEquals(8_000_000_000L, record.startNanos)
                val fields = JSONObject(old.provenance).getJSONArray("fields")
                val provenance =
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
                        "test",
                    )
                val newer = results.request(FitSource.slot(id), UUID.randomUUID(), provenance)
                val session = inputs.session(id)!!
                assertEquals(
                    HistoryReason.PENDING,
                    loadFitHistory(results, session, HistoryConfig(5, source = HistorySource.FIT))
                        .reason,
                )
                results.complete(
                    newer,
                    AnalysisTerminal(AnalysisStatus.FAILED, Instant.EPOCH, reason = "TEST_FAILURE"),
                )
                val entry =
                    loadFitHistory(results, session, HistoryConfig(5, source = HistorySource.FIT))
                assertEquals(HistoryReason.FAILED, entry.reason)
                assertNull(entry.watts)
                assertEquals(old, results.run(old.generation))
            }
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `unsupported archive shape completion and bounds refuse before opening production storage`() {
        for ((key, value) in
            listOf<Pair<String, Any>>(
                "completion" to "eof-only",
                "schema" to "fit-history-archive-v2",
                "projected_rows" to 0,
                "projected_rows" to 131073,
                "end_offset_nanos" to 86_400_000_000_001L,
                "decoder_version" to "future",
                "provenance" to "generated-synthetic-v1",
            )) {
            val name = "invalid-${UUID.randomUUID()}.db"
            val root = archive()
            val file = File(root, "manifest.json")
            file.writeText(JSONObject(file.readText()).put(key, value).toString())
            assertThrows(IllegalArgumentException::class.java) {
                FitHistoryImporter(context, name).importArchive(root)
            }
            assertFalse(context.getDatabasePath(name).exists())
        }
    }

    @Test
    fun `a new live cold attach cannot inherit the imported file ambiguity group`() {
        val name = "source-boundary-${UUID.randomUUID()}.db"
        val importedId = FitHistoryImporter(context, name).importArchive(archive())
        InputRepository(context, name).use { inputs ->
            val archived = inputs.session(importedId)
            val live = LiveDiagnosticCoordinator(inputs, elapsed = { 0L })
            live.pump(true)
            synchronized(live.gate) {
                live.accept(
                    com.oxywatt.core.collection.SourceObservation(
                        live.stamp(),
                        com.oxywatt.core.collection.SourceRef("karoo", "1.1.9"),
                        "karoo-normalization-v1",
                        com.oxywatt.core.collection.InputPayload.Recording(
                            com.oxywatt.core.collection.RecordingState.RECORDING
                        ),
                    )
                )
            }
            live.pump(true)
            val session = inputs.checkpoint()!!.latestSession!!
            assertEquals(com.oxywatt.core.collection.SessionOrigin.COLD_ATTACH, session.origin)
            assertEquals(session.id, session.ambiguityGroupId)
            assertNotEquals(importedId, session.ambiguityGroupId)
            assertEquals(archived, inputs.session(importedId))
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `declared session span beyond 24 hours by a fraction is refused before database writes`() {
        val name = "fractional-bound-${UUID.randomUUID()}.db"
        val root = archive()
        val manifest = File(root, "manifest.json")
        manifest.writeText(
            JSONObject(manifest.readText()).put("session_end", "2020-01-03T10:00:00.5Z").toString()
        )
        assertThrows(IllegalArgumentException::class.java) {
            FitHistoryImporter(context, name).importArchive(root)
        }
        assertFalse(context.getDatabasePath(name).exists())
    }

    @Test
    fun `changed projection is rejected before any database write`() {
        val name = "corrupt-${UUID.randomUUID()}.db"
        val root = archive()
        File(root, "observations.tsv").appendText("corrupt\n")
        assertThrows(IllegalArgumentException::class.java) {
            FitHistoryImporter(context, name).importArchive(root)
        }
        InputRepository(context, name).use { assertEquals(0L, it.state().revision) }
        context.deleteDatabase(name)
    }
}

/**
 * Literal test timeline builder; no production parser/integrator is used to derive expected watts.
 */
internal class FitScript {
    private val events = mutableListOf<List<String>>()

    fun state(at: Double, recording: Boolean) {
        events +=
            listOf(
                (at * 1e9).toLong().toString(),
                "STATE",
                if (recording) "RECORDING" else "PAUSED",
                "-",
                if (recording) "start" else "stop_all",
            )
    }

    fun power(at: Double, value: Double?) {
        val time = (at * 1e9).toLong().toString()
        events +=
            listOf(
                time,
                "POWER_W",
                value?.toString() ?: "-",
                if (value == null) "UNAVAILABLE" else "-",
                "-",
            )
        events += listOf(time, "HEART_RATE_BPM", "120", "-", "-")
        events += listOf(time, "CADENCE_RPM", "80", "-", "-")
    }

    fun write(root: File, leading: Long = 0, trailing: Long = 0) {
        val base = Instant.parse("2020-01-02T10:00:00Z")
        val projection = File(root, "observations.tsv")
        projection.writeText(
            "message_index\tfile_timestamp\toffset_nanos\tkind\tvalue\treason\ttimer_type\n" +
                events
                    .mapIndexed { i, e ->
                        "$i\t${base.plusNanos(e[0].toLong())}\t${e.joinToString("\t")}\n"
                    }
                    .joinToString("")
        )
        val manifest = File(root, "manifest.json")
        val last = events.last()[0].toLong()
        manifest.writeText(
            JSONObject(manifest.readText())
                .put(
                    "projection_sha256",
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(projection.readBytes())
                        .joinToString("") { "%02x".format(it) },
                )
                .put("projected_rows", events.size)
                .put("end_offset_nanos", last)
                .put("session_start", base.minusSeconds(leading).toString())
                .put("session_end", base.plusNanos(last).plusSeconds(trailing).toString())
                .toString()
        )
    }
}
