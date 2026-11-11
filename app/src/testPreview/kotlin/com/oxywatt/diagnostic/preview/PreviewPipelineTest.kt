package com.oxywatt.diagnostic.preview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class PreviewPipelineTest {
    @Test
    fun receivedIsNotSavedUntilRealTransactionCommits() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
        PreviewPipeline(context, name).use { pipeline ->
            val input =
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, pipeline.epoch, 0, 0),
                    SourceRef("simulated", "1"),
                    "canonical-v1",
                    InputPayload.Recording(RecordingState.RECORDING),
                )
            pipeline.accept(input)
            assertEquals(input, pipeline.received)
            assertEquals(0, pipeline.saved)
            pipeline.flush()
            assertEquals(1, pipeline.saved)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun databaseNameCannotTargetRealHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThrows(IllegalArgumentException::class.java) {
            PreviewPipeline(context, "history.db").close()
        }
    }

    @Test
    fun reopeningDisplaysCommittedPartialSessionWithoutRestartingSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
        PreviewPipeline(context, name).use { p ->
            p.accept(
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, p.epoch, 0, 0),
                    SourceRef("simulated", "1"),
                    "canonical-v1",
                    InputPayload.Recording(RecordingState.RECORDING),
                )
            )
            p.flush()
        }
        PreviewPipeline(context, name).use { p ->
            assertEquals(1, p.saved)
            assertEquals(SessionPhase.INTERRUPTED, p.committedCheckpoint?.latestSession?.phase)
            assertNull(p.received)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun failedCommitRetainsPendingAndExactRetryDoesNotDuplicate() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
        PreviewPipeline(context, name).use { p ->
            p.accept(
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, p.epoch, 0, 0),
                    SourceRef("simulated", "1"),
                    "canonical-v1",
                    InputPayload.Recording(RecordingState.RECORDING),
                )
            )
            p.failWrites { throw android.database.sqlite.SQLiteFullException() }
            p.flush()
            assertEquals(0, p.saved)
            assertEquals(1, p.pending)
            assertEquals("SQLiteFullException", p.error)
            assertNull(p.committedCheckpoint)
            p.failWrites {}
            p.flush()
            p.flush()
            assertEquals(1, p.saved)
            assertEquals(0, p.pending)
            assertNull(p.error)
            p.stopped()
            assertEquals(SessionPhase.INTERRUPTED, p.committedCheckpoint?.latestSession?.phase)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun fixedScenarioUsesRealRepositoryAtBothSpeedsAndPreservesMissingAndZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (speed in listOf(1, 10)) {
            val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
            PreviewPipeline(context, name).use { p ->
                val clock = com.oxywatt.core.collection.simulation.VirtualTimeline()
                val source =
                    com.oxywatt.core.collection.simulation.SimulatedRideDataSource(
                        com.oxywatt.core.collection.simulation.previewScenario(),
                        clock,
                        p.epoch,
                        Instant.EPOCH,
                        10_000_000_000,
                        speed = speed,
                    )
                source.start({ p.accept(it) }, { assertNull(it) }).use { while (source.pump()) {} }
                p.flush()
                assertEquals(7, p.saved)
                assertEquals(180.0, p.committedReadings[Metric.POWER_W]?.value)
                assertEquals(
                    MissingReason.UNAVAILABLE,
                    p.committedReadings[Metric.HEART_RATE_BPM]?.missingReason,
                )
                assertEquals(SessionPhase.RECORDING, p.committedCheckpoint?.latestSession?.phase)
                p.stopped()
                assertEquals(SessionPhase.INTERRUPTED, p.committedCheckpoint?.latestSession?.phase)
                assertFalse(p.committedCheckpoint!!.latestSession!!.trendEligible)
            }
            context.deleteDatabase(name)
        }
    }

    @Test
    fun overloadIsVisibleAndDoesNotPretendLostReadingsWereSaved() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
        PreviewPipeline(context, name, policy = AdmissionPolicy(events = 2, reservedEvents = 1))
            .use { p ->
                fun input(seq: Long, payload: InputPayload) =
                    SourceObservation(
                        ReceiptStamp(Instant.EPOCH, p.epoch, seq, seq),
                        SourceRef("simulated", "1"),
                        "canonical-v1",
                        payload,
                    )
                p.accept(input(0, InputPayload.Recording(RecordingState.RECORDING)))
                repeat(10000) {
                    p.accept(
                        input(it + 1L, InputPayload.MetricReading(Metric.POWER_W, it.toDouble()))
                    )
                }
                assertEquals(2, p.pending)
                assertEquals(9999, p.loss)
                p.flush()
                assertEquals(2, p.saved)
                assertEquals(0, p.pending)
                assertEquals(SessionPhase.INTERRUPTED, p.committedCheckpoint?.latestSession?.phase)
            }
        context.deleteDatabase(name)
    }

    @Test
    fun androidAssetStreamPreservesSyntheticFitTimingAtBothSpeeds() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manifest =
            context.assets.open("fit-scenario/manifest.json").bufferedReader().use { it.readText() }
        val outcomes = mutableListOf<List<Pair<Long, InputPayload>>>()
        for (speed in listOf(1, 10)) {
            val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
            PreviewPipeline(context, name).use { p ->
                val values = mutableListOf<Pair<Long, InputPayload>>()
                val end =
                    com.oxywatt.core.collection.simulation.replayFitStream(
                        manifest,
                        { context.assets.open("fit-scenario/observations.tsv") },
                        speed,
                        {
                            p.accept(it)
                            values += it.stamp.elapsedNanos to it.payload
                        },
                        epoch = p.epoch,
                    )
                assertEquals(9_000_000_000, end)
                assertEquals(15, values.size)
                p.flush()
                assertEquals(15, p.saved)
                p.stopped()
                assertEquals(SessionPhase.INTERRUPTED, p.committedCheckpoint?.latestSession?.phase)
                outcomes += values
            }
            context.deleteDatabase(name)
        }
        assertEquals(outcomes[0], outcomes[1])
    }

    @Test
    fun failedStandaloneStopRetriesTheFrozenInterruption() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
        PreviewPipeline(context, name).use { p ->
            p.accept(
                SourceObservation(
                    ReceiptStamp(Instant.EPOCH, p.epoch, 0, 0),
                    SourceRef("simulated", "1"),
                    "canonical-v1",
                    InputPayload.Recording(RecordingState.RECORDING),
                )
            )
            p.flush()
            p.failWrites { throw android.database.sqlite.SQLiteFullException() }
            p.stopped()
            assertEquals(SessionPhase.RECORDING, p.committedCheckpoint?.latestSession?.phase)
            p.failWrites {}
            p.flush()
            assertEquals(SessionPhase.INTERRUPTED, p.committedCheckpoint?.latestSession?.phase)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun persistedCurveIsIdenticalAtAllReplaySpeedsAndReopenReadsTheSameGeneration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val semantic =
            mutableListOf<
                Triple<String, List<com.oxywatt.core.power.PowerDurationResult>, Boolean>
            >()
        for (speed in listOf(1, 10, null)) {
            val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
            val fixedEpoch = UUID(0, 42)
            var generation = 0L
            // A fixed ID provider tests raw semantic hash equality across persisted replays.
            com.oxywatt.diagnostic.persistence.InputRepository(context, name).use { repo ->
                val l =
                    CollectorLifecycle(
                        fixedEpoch,
                        LifecyclePolicy("preview-silence-v1", 2_000_000_000),
                        { UUID(0, 41) },
                    )
                val queue = BoundedAdmission()
                val clock = com.oxywatt.core.collection.simulation.VirtualTimeline()
                val source =
                    com.oxywatt.core.collection.simulation.SimulatedRideDataSource(
                        com.oxywatt.core.collection.simulation.powerCurvePreviewScenario(),
                        clock,
                        fixedEpoch,
                        Instant.EPOCH,
                        17_000_000_000,
                        speed = speed,
                    )
                source.start({ queue.offer(it) }, { assertNull(it) }).use {
                    while (source.pump()) {}
                }
                val batch = queue.takeBatch()!!
                repo.commit(repo.prepare(batch, l, 0))
                queue.committed(batch)
                com.oxywatt.diagnostic.persistence.AnalysisRepository(context, name).use { results
                    ->
                    val c =
                        com.oxywatt.diagnostic.persistence
                            .PowerCurveCalculation(repo, results)
                            .prepare(UUID(0, 41), "SYNTHETIC:preview-curve-v1:seed=0", "test")
                    assertEquals(
                        com.oxywatt.diagnostic.persistence.AnalysisStatus.AVAILABLE,
                        c.terminal.status,
                    )
                    generation = c.generation
                    results.complete(c.generation, c.terminal)
                    val curve =
                        results
                            .current(
                                com.oxywatt.diagnostic.persistence.PowerCurveCalculation.slot(
                                    UUID(0, 41)
                                )
                            )!!
                            .terminal!!
                            .curve!!
                    semantic.add(
                        Triple(
                            curve.inputHash,
                            curve.snapshot.durations,
                            curve.snapshot.resourceLimited,
                        )
                    )
                    assertEquals(200.0, curve.snapshot.durations.first().record!!.meanWatts, 1e-8)
                    assertEquals(
                        10_000_000_000L,
                        curve.snapshot.durations.first().record!!.startNanos,
                    )
                }
            }
            PreviewPipeline(context, name).use { p ->
                p.calculateCurve("SYNTHETIC:ignored-on-reopen", "test")
                assertEquals(generation, p.storedCurve()!!.generation)
                val text = curveText(context, p)
                assertTrue(text, text.contains("200.00 W"))
                assertTrue(text, text.contains("SYNTHETIC"))
                assertTrue(text, text.contains("ENDED_LOCALLY"))
            }
            context.deleteDatabase(name)
        }
        assertEquals(semantic[0], semantic[1])
        assertEquals(semantic[0], semantic[2])
    }

    @Test
    fun terminalSyntheticFitSavesPartialUnavailableCurveWithoutInventingCoverage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "oxywatt-synthetic-demo-${UUID.randomUUID()}.db"
        PreviewPipeline(context, name).use { p ->
            val manifest =
                context.assets.open("fit-scenario/manifest.json").bufferedReader().use {
                    it.readText()
                }
            com.oxywatt.core.collection.simulation.replayFitStream(
                manifest,
                { context.assets.open("fit-scenario/observations.tsv") },
                null,
                { p.accept(it) },
                epoch = p.epoch,
            )
            p.flush()
            p.stopped()
            p.calculateCurve("SYNTHETIC:generated-synthetic-v1:seed=0", "test")
            val run = p.storedCurve()!!
            assertEquals(com.oxywatt.diagnostic.persistence.AnalysisStatus.AVAILABLE, run.status)
            assertEquals(SessionPhase.INTERRUPTED, run.terminal!!.curve!!.session.phase)
            assertTrue(run.terminal!!.curve!!.snapshot.durations.all { it.record == null })
            assertTrue(curveText(context, p).contains("INTERRUPTED"))
        }
        context.deleteDatabase(name)
    }
}
