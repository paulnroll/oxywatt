package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

class SimulationTest {
    @Test
    fun timersRunThroughSilenceBeforeSameTimeInput() {
        val clock = VirtualTimeline()
        val events = mutableListOf<String>()
        clock.at(5) { events.add("flush") }
        clock.at(10) { events.add("timeout") }
        clock.advanceTo(10)
        events.add("input")
        assertEquals(listOf("flush", "timeout", "input"), events)
    }

    private val epoch = UUID(0, 1)
    private val origin = Instant.parse("2026-01-01T00:00:00Z")

    private fun recording(state: RecordingState) = InputPayload.Recording(state)

    private fun source(
        events: Sequence<ScheduledObservation>,
        clock: VirtualTimeline = VirtualTimeline(),
        epochId: UUID = epoch,
        end: Long = 100,
    ) = SimulatedRideDataSource(events, clock, epochId, origin, end)

    @Test
    fun literalSignalsFaultsAndIndependentChannels() {
        val power = {
            signal(
                Metric.POWER_W,
                sequenceOf(0L, 5L, 10L, 15L, 20L),
                listOf(
                    SignalSegment(0, 10, 250.0),
                    SignalSegment(10, 20, 100.0, 300.0),
                    SignalSegment(20, 21, 0.0),
                ),
            )
        }
        val hr = {
            signal(
                Metric.HEART_RATE_BPM,
                sequenceOf(0L, 5L, 10L, 15L),
                listOf(SignalSegment(0, 20, 140.0)),
                dropouts = listOf(5L..5L),
                silence = listOf(10L..15L),
            )
        }
        val events = mergeChannels(power, hr).toList()
        assertEquals(listOf(0L, 0L, 5L, 5L, 10L, 15L, 20L), events.map { it.offsetNanos })
        assertEquals(
            listOf(250.0, 140.0, 250.0, null, 100.0, 200.0, 0.0),
            events.map { (it.payload as InputPayload.MetricReading).value },
        )
        assertEquals(
            MissingReason.DROPOUT,
            (events[3].payload as InputPayload.MetricReading).missingReason,
        )
        val efforts =
            signal(
                Metric.POWER_W,
                sequenceOf(0L, 2L, 5L),
                listOf(
                    SignalSegment(0, 2, 500.0),
                    SignalSegment(2, 5, 300.0),
                    SignalSegment(5, 6, 200.0),
                ),
            )
        assertEquals(
            listOf(500.0, 300.0, 200.0),
            efforts.map { (it.payload as InputPayload.MetricReading).value }.toList(),
        )
    }

    @Test
    fun allPlaybackSpeedsPreserveTimelineAndTwoRideObservations() {
        val events =
            listOf(
                ScheduledObservation(0, recording(RecordingState.IDLE)),
                ScheduledObservation(1, recording(RecordingState.RECORDING)),
                ScheduledObservation(5, recording(RecordingState.PAUSED)),
                ScheduledObservation(5, recording(RecordingState.RECORDING)),
                ScheduledObservation(10, recording(RecordingState.IDLE), -100),
                ScheduledObservation(20, recording(RecordingState.RECORDING)),
                ScheduledObservation(30, recording(RecordingState.IDLE)),
            )
        fun replay(speed: Int?): Pair<List<SourceObservation>, List<Long>> {
            val clock = VirtualTimeline()
            val timers = mutableListOf<Long>()
            listOf(5L, 10L, 40L).forEach { time -> clock.at(time) { timers.add(clock.now) } }
            val output = mutableListOf<SourceObservation>()
            val deadlines = mutableListOf<Long>()
            val input =
                SimulatedRideDataSource(
                    events.asSequence(),
                    clock,
                    epoch,
                    origin,
                    50,
                    speed = speed,
                    awaitPlaybackDeadline = { deadlines.add(it) },
                )
            var ended = 0
            input.start(
                { output.add(it) },
                {
                    assertNull(it)
                    ended++
                },
            )
            var previousDeadline = 0L
            events.forEach {
                val deadline = playbackDeadline(it.offsetNanos, speed)
                assertTrue(deadline >= previousDeadline)
                previousDeadline = deadline
                assertTrue(input.pump())
            }
            assertFalse(input.pump())
            assertEquals(
                events.map { playbackDeadline(it.offsetNanos, speed) } +
                    playbackDeadline(50, speed),
                deadlines,
            )
            assertEquals(1, ended)
            assertEquals(50, clock.now)
            assertEquals(listOf(5L, 10L, 40L), timers)
            return output to timers
        }
        val expected = replay(1)
        assertEquals(expected, replay(10))
        assertEquals(expected, replay(null))
        assertEquals((0L..6L).toList(), expected.first.map { it.stamp.ingestSequence })
        assertTrue(expected.first[4].stamp.utc < expected.first[3].stamp.utc)
        assertEquals(10, expected.first[4].stamp.elapsedNanos)
        assertEquals(1, playbackDeadline(19, 10))
        assertEquals(2, playbackDeadline(20, 10))
    }

    @Test
    fun cancellationInsideCallbackAcknowledgesAfterExitAndIsIdempotent() {
        val input =
            source(
                sequenceOf(
                    ScheduledObservation(0, recording(RecordingState.RECORDING)),
                    ScheduledObservation(1, recording(RecordingState.IDLE)),
                )
            )
        val order = mutableListOf<String>()
        lateinit var handle: AutoCloseable
        handle =
            input.start(
                {
                    order.add("enter")
                    handle.close()
                    order.add("exit")
                },
                {
                    assertNull(it)
                    order.add("stopped")
                },
            )
        assertFalse(input.pump())
        handle.close()
        assertFalse(input.pump())
        assertEquals(listOf("enter", "exit", "stopped"), order)
        assertFailsWith<IllegalStateException> { input.start({}, {}) }
    }

    @Test
    fun failureAndEofDoNotInventIdle() {
        val failure = IllegalStateException("injected")
        val input =
            source(
                sequence {
                    yield(ScheduledObservation(0, recording(RecordingState.RECORDING)))
                    throw failure
                }
            )
        val received = mutableListOf<InputPayload>()
        val terminal = mutableListOf<Throwable?>()
        input.start({ received.add(it.payload) }, { terminal.add(it) })
        assertTrue(input.pump())
        assertFalse(input.pump())
        assertEquals(listOf<InputPayload>(recording(RecordingState.RECORDING)), received)
        assertSame(failure, terminal.single())
        val sink = source(sequenceOf(ScheduledObservation(0, recording(RecordingState.RECORDING))))
        sink.start({ throw failure }, { terminal.add(it) })
        assertFalse(sink.pump())
        assertSame(failure, terminal.last())
        val eof = source(sequenceOf(ScheduledObservation(0, recording(RecordingState.RECORDING))))
        eof.start({ received.add(it.payload) }, { terminal.add(it) })
        eof.pump()
        eof.pump()
        assertNull(terminal.last())
        assertTrue(received.all { it == recording(RecordingState.RECORDING) })
    }

    @Test
    fun failedStartAndNewEpochRestart() {
        val bad = source(Sequence { throw IllegalArgumentException("iterator") })
        var calls = 0
        assertFailsWith<IllegalArgumentException> { bad.start({}, { calls++ }) }
        assertEquals(0, calls)
        val stamps = mutableListOf<ReceiptStamp>()
        listOf(epoch, UUID(0, 2)).forEach {
            val input =
                source(
                    sequenceOf(ScheduledObservation(0, recording(RecordingState.RECORDING))),
                    epochId = it,
                )
            input.start({ stamps.add(it.stamp) }, {})
            input.pump()
            input.pump()
        }
        assertNull(stamps[1].elapsedSince(stamps[0]))
    }

    @Test
    fun seededGeneratorAndManifestAreReproducible() {
        fun noise(seed: Long) =
            signal(
                    Metric.POWER_W,
                    (0L..10L).asSequence(),
                    listOf(SignalSegment(0, 11, 250.0)),
                    seed,
                    5.0,
                )
                .toList()
        assertEquals(noise(7), noise(7))
        assertNotEquals(noise(7), noise(8))
        val manifest = RunManifest("constant-250-11ns-noise5-v1", 7, epoch, origin, 11)
        assertEquals(manifest.hash(), manifest.copy().hash())
        assertNotEquals(manifest.hash(), manifest.copy(seed = 8).hash())
        assertNotEquals(manifest.hash(), manifest.copy(profileVersion = "2").hash())
        val directory = Path.of("build", "simulation-evidence")
        Files.createDirectories(directory)
        Files.writeString(
            directory.resolve("manifest.txt"),
            "$manifest\nsha256=${manifest.hash()}\n",
        )
    }

    @Test
    fun boundedLazyConsumptionAndInvalidOrdering() {
        var generated = 0
        val input =
            source(
                sequence {
                    while (true) {
                        generated++
                        yield(ScheduledObservation(0, recording(RecordingState.IDLE)))
                    }
                }
            )
        val handle = input.start({}, {})
        repeat(10) { input.pump() }
        handle.close()
        assertEquals(10, generated)
        val invalid =
            source(
                sequenceOf(
                    ScheduledObservation(5, recording(RecordingState.IDLE)),
                    ScheduledObservation(4, recording(RecordingState.IDLE)),
                )
            )
        var failure: Throwable? = null
        invalid.start({}, { failure = it })
        invalid.pump()
        invalid.pump()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun cancellationFromAnotherThreadWaitsForCallbackExit() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val input = source(sequenceOf(ScheduledObservation(0, recording(RecordingState.RECORDING))))
        val handle =
            input.start(
                {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                },
                { stopped.countDown() },
            )
        val pumping = Thread { input.pump() }
        pumping.start()
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val closing = Thread { handle.close() }
        closing.start()
        assertEquals(1, stopped.count)
        release.countDown()
        pumping.join(5000)
        closing.join(5000)
        assertFalse(pumping.isAlive)
        assertFalse(closing.isAlive)
        assertEquals(0, stopped.count)
    }

    @Test
    fun profileAndAutomaticPauseMetadataStayExplicit() {
        // Canonical v1 records PAUSED; auto/manual belongs to the scenario manifest.
        val profile = InputPayload.Profile(RiderProfileSnapshot("profile-1"))
        val events =
            sequenceOf(
                ScheduledObservation(0, profile),
                ScheduledObservation(1, recording(RecordingState.PAUSED)),
            )
        val input = source(events)
        val output = mutableListOf<InputPayload>()
        input.start({ output.add(it.payload) }, {})
        while (input.pump()) {}
        assertEquals(listOf<InputPayload>(profile, recording(RecordingState.PAUSED)), output)
        assertTrue(
            RunManifest("automatic-pause-v1", 0, epoch, origin, 100).hash() !=
                RunManifest("manual-pause-v1", 0, epoch, origin, 100).hash()
        )
    }

    @Test
    fun hourOfConstantPowerAndExactFlushBoundary() {
        val second = 1_000_000_000L
        val clock = VirtualTimeline()
        val order = mutableListOf<String>()
        val values = mutableListOf<Double?>()
        clock.at(5 * second) { order.add("flush") }
        val trace =
            signal(
                Metric.POWER_W,
                (0L..3600L).asSequence().map { it * second },
                listOf(SignalSegment(0, 3600 * second + 1, 250.0)),
            )
        val input = source(trace, clock, end = 3600 * second)
        input.start(
            { observation ->
                values.add((observation.payload as InputPayload.MetricReading).value)
                if (observation.stamp.elapsedNanos == 5 * second) order.add("sample")
            },
            { assertNull(it) },
        )
        while (input.pump()) {}
        assertEquals(3601, values.size)
        assertTrue(values.all { it == 250.0 })
        assertEquals(3600 * second, clock.now)
        assertEquals(listOf("flush", "sample"), order)
    }

    @Test
    fun nestedClockAdvanceIsRejectedWithoutRewindingTime() {
        val clock = VirtualTimeline()
        clock.at(5) { assertFailsWith<IllegalStateException> { clock.advanceTo(20) } }
        clock.advanceTo(10)
        assertEquals(10, clock.now)
        clock.advanceTo(20)
        assertEquals(20, clock.now)
    }

    @Test
    fun timerFailureReleasesAdvanceGuardAndCancelledTimerNeverFires() {
        val clock = VirtualTimeline()
        val cancelled = clock.at(3) { fail("cancelled timer fired") }
        cancelled.close()
        cancelled.close()
        clock.at(5) { throw IllegalArgumentException("injected") }
        assertFailsWith<IllegalArgumentException> { clock.advanceTo(10) }
        assertEquals(5, clock.now)
        clock.advanceTo(10)
        assertEquals(10, clock.now)
    }
}
