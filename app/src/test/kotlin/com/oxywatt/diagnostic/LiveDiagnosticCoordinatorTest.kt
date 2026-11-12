package com.oxywatt.diagnostic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oxywatt.core.collection.*
import com.oxywatt.diagnostic.persistence.InputRepository
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LiveDiagnosticCoordinatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var now = 0L

    private fun input(owner: LiveDiagnosticCoordinator, payload: InputPayload) =
        synchronized(owner.gate) {
            SourceObservation(
                    owner.stamp(),
                    SourceRef("karoo", "1.1.9"),
                    "karoo-normalization-v1",
                    payload,
                )
                .also(owner::accept)
        }

    @Test
    fun `received idle zero and missing inputs become saved only after real commit`() {
        val name = "live-test-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now }, utc = { Instant.EPOCH })
            input(owner, InputPayload.Recording(RecordingState.IDLE))
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 0.0))
            input(
                owner,
                InputPayload.MetricReading(Metric.HEART_RATE_BPM, null, MissingReason.UNAVAILABLE),
            )
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 0.0))
            input(owner, InputPayload.MetricReading(Metric.CADENCE_RPM, 0.0))
            input(owner, InputPayload.Profile(RiderProfileSnapshot("fixture-v1")))
            assertEquals(6L, owner.snapshot().received)
            assertEquals(0L, owner.snapshot().committed)
            assertTrue(repo.observations().isEmpty())
            now = 5_000_000_000
            owner.pump()
            assertEquals(6L, owner.snapshot().committed)
            assertEquals(0, owner.snapshot().pending)
            val rows = repo.observations()
            assertEquals(6, rows.size)
            assertEquals(6, rows.map { it.observation.stamp.ingestSequence }.toSet().size)
            assertTrue(rows.all { it.sessionId == null })
            assertNull((rows[2].observation.payload as InputPayload.MetricReading).value)
        }
        InputRepository(context, name).use { assertEquals(6, it.observations().size) }
        context.deleteDatabase(name)
    }

    @Test
    fun `failed writes retain capacity and retry only at the versioned deadline`() {
        val name = "retry-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            repo.beforeCheckpoint = { throw android.database.sqlite.SQLiteFullException() }
            owner.pump(true)
            assertEquals(LiveStorageStatus.RETRYING, owner.snapshot().status)
            assertEquals(0L, owner.snapshot().committed)
            assertEquals(1, owner.snapshot().pending)
            repo.beforeCheckpoint = {}
            owner.pump(true)
            assertEquals(0L, owner.snapshot().committed)
            now = 1_000_000_000
            owner.pump(true)
            assertEquals(1L, owner.snapshot().committed)
            assertEquals(1, repo.observations().size)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `equal flush and silence deadlines cut before the new input`() {
        val name = "cut-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.IDLE))
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            owner.pump(true)
            now = 1_000_000_000
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 123.0))
            now = 4_000_000_000
            input(owner, InputPayload.MetricReading(Metric.HEART_RATE_BPM, 100.0))
            now = 6_000_000_000
            val equal = input(owner, InputPayload.MetricReading(Metric.POWER_W, 234.0))
            owner.pump()
            assertEquals(4L, owner.snapshot().committed)
            assertFalse(repo.observations().any { it.observation == equal })
            val effects = repo.replay(repo.state().revision)
            assertEquals(
                listOf(6_000_000_000L),
                effects
                    .flatMap { it.update.coverageBreaks }
                    .filter { it.reason == CoverageBreakReason.SILENCE }
                    .map { it.detectedAtNanos },
            )
            owner.pump(true)
            assertEquals(5L, owner.snapshot().committed)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `uncertain commit response retries frozen input without double counting`() {
        val name = "uncertain-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 0.0))
            owner.afterCommitForTest = { throw android.database.sqlite.SQLiteDiskIOException() }
            owner.pump(true)
            assertEquals(1, repo.observations().size)
            assertEquals(0L, owner.snapshot().committed)
            assertEquals(1, owner.snapshot().pending)
            owner.afterCommitForTest = {}
            now = 1_000_000_000
            owner.pump(true)
            assertEquals(1L, owner.snapshot().committed)
            assertEquals(1, repo.observations().size)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `slow real transaction leaves callbacks bounded including in flight and control reserve`() {
        val name = "burst-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            val entered = java.util.concurrent.CountDownLatch(1)
            val release = java.util.concurrent.CountDownLatch(1)
            repo.beforeCheckpoint = {
                entered.countDown()
                check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
            }
            val failure = java.util.concurrent.atomic.AtomicReference<Throwable>()
            val writer = Thread {
                try {
                    owner.pump(true)
                } catch (t: Throwable) {
                    failure.set(t)
                }
            }
            writer.start()
            assertTrue(entered.await(10, java.util.concurrent.TimeUnit.SECONDS))
            try {
                repeat(100_001) { input(owner, InputPayload.MetricReading(Metric.POWER_W, 0.0)) }
                assertEquals(4033, owner.snapshot().pending)
                assertTrue(owner.snapshot().bytes <= 4 * 1024 * 1024)
                assertEquals(100_001L - 4032, owner.snapshot().loss)
                repeat(64) { input(owner, InputPayload.Recording(RecordingState.PAUSED)) }
                assertEquals(4096, owner.snapshot().pending)
                assertEquals(1L, owner.snapshot().loss - (100_001L - 4032))
                input(owner, InputPayload.Recording(RecordingState.IDLE))
                assertEquals(1L, owner.snapshot().rejected)
                owner.stopped()
            } finally {
                release.countDown()
                writer.join(15_000)
            }
            assertFalse(writer.isAlive)
            failure.get()?.let { throw AssertionError(it) }
            repo.beforeCheckpoint = {}
            repeat(4) { owner.pump(true) }
            assertEquals(0, owner.snapshot().pending)
            assertEquals(SessionPhase.INTERRUPTED, repo.checkpoint()!!.latestSession!!.phase)
            assertEquals(4096L, owner.snapshot().committed)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `restart discards unknown tail and preserves distinct ambiguous sessions`() {
        val name = "restart-${UUID.randomUUID()}.db"
        var old: UUID? = null
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.IDLE))
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            owner.pump(true)
            old = repo.checkpoint()!!.latestSession!!.id
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 234.0))
        }
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            repo.beforeCheckpoint = { throw android.database.sqlite.SQLiteFullException() }
            owner.pump()
            assertFalse(owner.ready)
            assertEquals(SessionPhase.RECORDING, repo.session(old!!)!!.phase)
            repo.beforeCheckpoint = {}
            now += 1_000_000_000
            owner.pump()
            assertTrue(owner.ready)
            assertEquals(
                InterruptionReason.PROCESS_RESTART,
                repo.session(old!!)!!.interruptionReason,
            )
            assertEquals(0L, owner.snapshot().received)
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            owner.pump(true)
            val fragment = repo.checkpoint()!!.latestSession!!
            assertNotEquals(old, fragment.id)
            assertEquals(old, fragment.ambiguityGroupId)
            assertFalse(fragment.trendEligible)
            assertEquals(3, repo.observations().size)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `standalone silence and terminal writes retry without losing effects`() {
        val name = "effects-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 123.0))
            owner.pump(true)
            now = 5_000_000_000
            repo.beforeCheckpoint = { throw android.database.sqlite.SQLiteFullException() }
            owner.pump()
            assertEquals(LiveStorageStatus.RETRYING, owner.snapshot().status)
            repo.beforeCheckpoint = {}
            now += 1_000_000_000
            owner.pump()
            assertEquals(
                1,
                repo
                    .replay(repo.state().revision)
                    .flatMap { it.update.coverageBreaks }
                    .count { it.reason == CoverageBreakReason.SILENCE },
            )
            repo.beforeCheckpoint = { throw android.database.sqlite.SQLiteFullException() }
            owner.stopped(InterruptionReason.TRANSPORT_LOSS)
            owner.pump()
            assertFalse(owner.finished)
            repo.beforeCheckpoint = {}
            now += 1_000_000_000
            owner.pump()
            assertTrue(owner.finished)
            assertEquals(
                InterruptionReason.TRANSPORT_LOSS,
                repo.checkpoint()!!.latestSession!!.interruptionReason,
            )
            assertEquals(2L, owner.snapshot().committed)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `two observed rides and pause resume retain independent sessions and segments`() {
        val name = "rides-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            listOf(
                    RecordingState.IDLE,
                    RecordingState.RECORDING,
                    RecordingState.PAUSED,
                    RecordingState.RECORDING,
                    RecordingState.IDLE,
                )
                .forEach { input(owner, InputPayload.Recording(it)) }
            owner.pump(true)
            val first = repo.checkpoint()!!.latestSession!!
            assertEquals(SessionPhase.ENDED_LOCALLY, first.phase)
            assertEquals(1L, first.segmentIndex)
            assertTrue(first.trendEligible)
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            input(owner, InputPayload.Recording(RecordingState.IDLE))
            owner.pump(true)
            assertNotEquals(first.id, repo.checkpoint()!!.latestSession!!.id)
            assertEquals(7L, owner.snapshot().committed)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `timer cannot overtake a receipt stamped inside the admission gate`() {
        val name = "stamp-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val clock = java.util.concurrent.atomic.AtomicLong(1)
            val owner = LiveDiagnosticCoordinator(repo, elapsed = clock::get)
            val stamped = java.util.concurrent.CountDownLatch(1)
            val release = java.util.concurrent.CountDownLatch(1)
            val timerStarted = java.util.concurrent.CountDownLatch(1)
            val errors = java.util.concurrent.atomic.AtomicReference<Throwable>()
            val callback = Thread {
                try {
                    synchronized(owner.gate) {
                        val stamp = owner.stamp()
                        stamped.countDown()
                        check(release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                        owner.accept(
                            SourceObservation(
                                stamp,
                                SourceRef("karoo", "1.1.9"),
                                "v1",
                                InputPayload.MetricReading(Metric.POWER_W, 0.0),
                            )
                        )
                    }
                } catch (t: Throwable) {
                    errors.set(t)
                }
            }
            callback.start()
            assertTrue(stamped.await(10, java.util.concurrent.TimeUnit.SECONDS))
            clock.set(6_000_000_000)
            val timer = Thread {
                try {
                    timerStarted.countDown()
                    owner.pump(true)
                } catch (t: Throwable) {
                    errors.set(t)
                }
            }
            timer.start()
            assertTrue(timerStarted.await(10, java.util.concurrent.TimeUnit.SECONDS))
            release.countDown()
            callback.join(15_000)
            timer.join(15_000)
            assertFalse(callback.isAlive || timer.isAlive)
            errors.get()?.let { throw AssertionError(it) }
            assertEquals(1L, owner.snapshot().committed)
            assertEquals(1L, repo.observations().single().observation.stamp.elapsedNanos)
            assertNotEquals(LiveStorageStatus.FATAL, owner.snapshot().status)
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `prepare failure stops the epoch without replaying partially applied lifecycle`() {
        val name = "fatal-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            // Another writer changes revision after the owner's acknowledged state.
            val other = CollectorLifecycle(UUID.randomUUID(), LifecyclePolicy("test", 1))
            repo.commit(repo.prepareEffect(other.advanceTime(0), AdmissionPolicy(), 0))
            owner.pump(true)
            assertEquals(LiveStorageStatus.FATAL, owner.snapshot().status)
            assertTrue(owner.shouldStopSource)
            input(owner, InputPayload.Recording(RecordingState.IDLE))
            assertEquals(1L, owner.snapshot().rejected)
            owner.pump(true)
            assertTrue(repo.observations().isEmpty())
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `oversized receipt is lost explicitly and breaks subsequent continuity`() {
        val name = "oversize-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner = LiveDiagnosticCoordinator(repo, elapsed = { now })
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            owner.pump(true)
            synchronized(owner.gate) {
                owner.accept(
                    SourceObservation(
                        owner.stamp(),
                        SourceRef("karoo", "1.1.9"),
                        "x".repeat(2048),
                        InputPayload.MetricReading(Metric.POWER_W, 123.0),
                    )
                )
            }
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            owner.pump(true)
            assertEquals(1L, owner.snapshot().loss)
            assertEquals(2L, owner.snapshot().committed)
            assertEquals(
                SessionOrigin.RECOVERY_FRAGMENT,
                repo.checkpoint()!!.latestSession!!.origin,
            )
            assertFalse(repo.checkpoint()!!.latestSession!!.trendEligible)
            assertTrue(
                repo.replay(repo.state().revision).any {
                    it.metadata.loss?.reasons?.contains(AdmissionLossReason.OVERSIZE) == true
                }
            )
        }
        context.deleteDatabase(name)
    }

    @Test
    fun `terminal drain persists losses accumulated while all capacity was in flight`() {
        val name = "loss-only-${UUID.randomUUID()}.db"
        InputRepository(context, name).use { repo ->
            val owner =
                LiveDiagnosticCoordinator(
                    repo,
                    elapsed = { now },
                    policy =
                        AdmissionPolicy(
                            "tiny",
                            events = 2,
                            bytes = 4096,
                            reservedEvents = 1,
                            reservedBytes = 1024,
                        ),
                )
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            input(owner, InputPayload.Recording(RecordingState.RECORDING))
            repo.beforeCheckpoint = { throw android.database.sqlite.SQLiteFullException() }
            owner.pump(true)
            input(owner, InputPayload.MetricReading(Metric.POWER_W, 0.0))
            input(owner, InputPayload.Recording(RecordingState.IDLE))
            owner.stopped()
            repo.beforeCheckpoint = {}
            now = 1_000_000_000
            repeat(4) { owner.pump() }
            assertTrue(owner.finished)
            assertEquals(2L, owner.snapshot().committed)
            val replay = repo.replay(repo.state().revision)
            assertEquals(2L, replay.mapNotNull { it.metadata.loss }.distinct().sumOf { it.count })
            assertTrue(replay.any { it.metadata.terminal == InterruptionReason.CONTROL_OVERFLOW })
        }
        context.deleteDatabase(name)
    }
}
