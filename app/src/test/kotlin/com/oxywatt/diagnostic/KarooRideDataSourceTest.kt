package com.oxywatt.diagnostic

import android.content.ComponentName
import com.oxywatt.core.collection.*
import fi.nikosavola.karooext.testing.robolectric.FakeKarooRule
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.*
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32, 34])
class KarooRideDataSourceTest {
    @get:Rule val karoo = FakeKarooRule()
    private val observed = CopyOnWriteArrayList<SourceObservation>()
    private var next = 0L
    private val epoch = UUID(0, 1)

    private fun source() =
        KarooRideDataSource(
            KarooSystemService(karoo.app),
            stamp = {
                val n = next++
                ReceiptStamp(Instant.EPOCH.plusNanos(n), epoch, n, n)
            },
        )

    private fun awaitCount(count: Int) = karoo.awaitValue { true.takeIf { observed.size >= count } }

    private fun point(type: String, value: Double) {
        val field =
            when (type) {
                DataType.Type.POWER -> DataType.Field.POWER
                DataType.Type.HEART_RATE -> DataType.Field.HEART_RATE
                else -> DataType.Field.CADENCE
            }
        karoo.system.setDataPoint(DataPoint(type, mapOf(field to value)))
    }

    private fun ready() = karoo.awaitValue { true.takeIf { karoo.system.consumerCount == 5 } }

    private fun metric(metric: Metric, value: Double?, reason: MissingReason? = null) =
        InputPayload.MetricReading(metric, value, reason)

    @Test
    fun `SDK stream matches independent direct canonical trace with distinct receipt keys`() {
        val handle = source().start(observed::add) { assertNull(it) }
        ready()
        observed.clear()
        val firstSequence = next
        val expected =
            listOf(
                metric(Metric.POWER_W, 0.0),
                metric(Metric.POWER_W, 0.0),
                metric(Metric.HEART_RATE_BPM, null, MissingReason.INVALID_SOURCE_VALUE),
                metric(Metric.CADENCE_RPM, 0.0),
                metric(Metric.POWER_W, null, MissingReason.DROPOUT),
                metric(Metric.POWER_W, null, MissingReason.UNAVAILABLE),
            )
        point(DataType.Type.POWER, 0.0)
        point(DataType.Type.POWER, 0.0)
        point(DataType.Type.HEART_RATE, 0.0)
        point(DataType.Type.CADENCE, 0.0)
        karoo.system.setStreamState(DataType.Type.POWER, StreamState.Searching)
        karoo.system.setStreamState(DataType.Type.POWER, StreamState.NotAvailable)
        awaitCount(expected.size)
        val direct = mutableListOf<SourceObservation>()
        val directSource = RideDataSource { sink, stopped ->
            expected.forEachIndexed { index, payload ->
                val n = firstSequence + index
                sink(
                    SourceObservation(
                        ReceiptStamp(Instant.EPOCH.plusNanos(n), epoch, n, n),
                        SourceRef("karoo", "1.1.9"),
                        "karoo-normalization-v1",
                        payload,
                    )
                )
            }
            stopped(null)
            AutoCloseable {}
        }
        directSource.start(direct::add) { assertNull(it) }.close()
        assertEquals(direct, observed.toList())
        assertEquals(observed.size, observed.map { it.stamp }.toSet().size)
        observed.zipWithNext().forEach { (a, b) ->
            assertTrue(b.stamp.ingestSequence > a.stamp.ingestSequence)
        }
        assertTrue(
            observed.all {
                it.source == SourceRef("karoo", "1.1.9") &&
                    it.normalizationVersion == "karoo-normalization-v1" &&
                    it.stamp.epochId == epoch
            }
        )
        handle.close()
        assertEquals(0, karoo.system.consumerCount)
    }

    @Test
    fun `initial state and both pause forms are mapped without native save inference`() {
        karoo.system.setRideState(RideState.Recording)
        val handle = source().start(observed::add) { assertNull(it) }
        ready()
        karoo.awaitValue {
            observed.firstOrNull { it.payload == InputPayload.Recording(RecordingState.RECORDING) }
        }
        observed.clear()
        karoo.system.setRideState(RideState.Paused(false))
        karoo.system.setRideState(RideState.Paused(true))
        karoo.system.setRideState(RideState.Idle)
        awaitCount(3)
        assertEquals(
            listOf(RecordingState.PAUSED, RecordingState.PAUSED, RecordingState.IDLE).map {
                InputPayload.Recording(it)
            },
            observed.map { it.payload },
        )
        handle.close()
    }

    @Test
    fun `profile is configured stable and invalid settings stay absent`() {
        val profile =
            UserProfile(
                75f,
                UserProfile.PreferredUnit(
                    UserProfile.PreferredUnit.UnitType.METRIC,
                    UserProfile.PreferredUnit.UnitType.METRIC,
                    UserProfile.PreferredUnit.UnitType.METRIC,
                    UserProfile.PreferredUnit.UnitType.METRIC,
                ),
                190,
                50,
                emptyList(),
                260,
                emptyList(),
            )
        karoo.system.setUserProfile(profile)
        val handle = source().start(observed::add) { assertNull(it) }
        ready()
        val first =
            karoo
                .awaitValue {
                    observed
                        .map { it.payload }
                        .filterIsInstance<InputPayload.Profile>()
                        .firstOrNull()
                }
                .snapshot
        assertEquals(ProfileSetting(75.0, SettingOrigin.CONFIGURED), first.massKg)
        assertEquals(
            ProfileSetting(190.0, SettingOrigin.CONFIGURED),
            first.configuredMaxHeartRateBpm,
        )
        assertEquals(ProfileSetting(260.0, SettingOrigin.CONFIGURED), first.configuredFtpWatts)
        assertNull(first.effectiveAt)
        assertEquals(first, KarooMapping.profile(profile).snapshot)
        observed.clear()
        karoo.system.setUserProfile(profile.copy(weight = 0f, maxHr = 0, ftp = -1))
        val invalid =
            karoo
                .awaitValue {
                    observed
                        .map { it.payload }
                        .filterIsInstance<InputPayload.Profile>()
                        .firstOrNull()
                }
                .snapshot
        assertNull(invalid.massKg)
        assertNull(invalid.configuredMaxHeartRateBpm)
        assertNull(invalid.configuredFtpWatts)
        assertNotEquals(first.version, invalid.version)
        handle.close()
    }

    @Test
    fun `cancellation from callback acknowledges after delivery and is single use`() {
        val source = source()
        var stopped = 0
        var inside = false
        lateinit var handle: AutoCloseable
        handle =
            source.start({
                observed.add(it)
                if (it.payload == metric(Metric.POWER_W, 123.0)) {
                    inside = true
                    handle.close()
                    assertEquals(0, stopped)
                    inside = false
                }
            }) {
                assertFalse(inside)
                assertNull(it)
                stopped++
            }
        ready()
        point(DataType.Type.POWER, 123.0)
        karoo.awaitValue { true.takeIf { stopped == 1 } }
        handle.close()
        assertEquals(1, stopped)
        assertEquals(0, karoo.system.consumerCount)
        assertThrows(IllegalStateException::class.java) { source.start({}) {} }
    }

    @Test
    fun `sink failure stops without manufacturing Idle and releases consumers`() {
        val failure = IllegalArgumentException("test sink")
        var stopped: Throwable? = null
        val handle =
            source().start({ if (it.payload == metric(Metric.POWER_W, 123.0)) throw failure }) {
                stopped = it
            }
        ready()
        point(DataType.Type.POWER, 123.0)
        karoo.awaitValue { stopped }
        assertSame(failure, stopped)
        assertEquals(0, karoo.system.consumerCount)
        handle.close()
    }

    @Test
    fun `living process reconnect registers exactly once and final close removes all`() {
        var stopped = 0
        val handle =
            source().start(observed::add) {
                assertNull(it)
                stopped++
            }
        ready()
        val connection = shadowOf(karoo.app).boundServiceConnections.single()
        val component =
            ComponentName(
                "io.hammerhead.appstore",
                "io.hammerhead.appstore.service.AppStoreService",
            )
        repeat(3) {
            // The dead host loses its registrations; Android retains the living client binding.
            connection.onServiceDisconnected(component)
            karoo.system.reset()
            observed.clear()
            point(DataType.Type.POWER, 140.0)
            assertTrue(observed.isEmpty())
            connection.onServiceConnected(component, karoo.system)
            ready()
            karoo.awaitValue {
                observed.firstOrNull { it.payload == metric(Metric.POWER_W, 140.0) }
            }
            observed.clear()
            point(DataType.Type.POWER, 150.0)
            awaitCount(1)
            assertEquals(listOf(metric(Metric.POWER_W, 150.0)), observed.map { it.payload })
            assertEquals(5, karoo.system.consumerCount)
            assertEquals(0, stopped)
        }
        handle.close()
        assertEquals(1, stopped)
        assertEquals(0, karoo.system.consumerCount)
    }

    @Test
    fun `binding failure reports termination and removes partial registrations`() {
        shadowOf(karoo.app).setThrowInBindService(SecurityException("test binding"))
        var stopped: Throwable? = null
        val handle = source().start(observed::add) { stopped = it }
        assertTrue(stopped is SecurityException)
        assertTrue(observed.isEmpty())
        assertEquals(0, karoo.system.consumerCount)
        handle.close()
    }

    @Test
    fun `completed lifecycle subscription interrupts source instead of running without states`() {
        var stopped: Throwable? = null
        val handle = source().start(observed::add) { stopped = it }
        ready()
        val id =
            karoo.system.consumerIds
                .zip(karoo.system.consumerParams)
                .single { it.second == RideState.Params }
                .first
        karoo.system.completeConsumer(id)
        assertNotNull(stopped)
        assertEquals(0, karoo.system.consumerCount)
        handle.close()
    }

    @Test
    fun `external cancellation waits for in-flight delivery before acknowledgement`() {
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val closeStarted = java.util.concurrent.CountDownLatch(1)
        val stopped = java.util.concurrent.atomic.AtomicInteger()
        val workers = java.util.concurrent.Executors.newFixedThreadPool(2)
        val handle =
            source().start({
                if (it.payload == metric(Metric.POWER_W, 123.0)) {
                    entered.countDown()
                    check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                }
            }) {
                assertNull(it)
                stopped.incrementAndGet()
            }
        try {
            ready()
            val publish = workers.submit { point(DataType.Type.POWER, 123.0) }
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
            val close = workers.submit {
                closeStarted.countDown()
                handle.close()
            }
            assertTrue(closeStarted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(0, stopped.get())
            release.countDown()
            publish.get(5, java.util.concurrent.TimeUnit.SECONDS)
            close.get(5, java.util.concurrent.TimeUnit.SECONDS)
            assertEquals(1, stopped.get())
            assertEquals(0, karoo.system.consumerCount)
        } finally {
            release.countDown()
            workers.shutdownNow()
            handle.close()
        }
    }

    @Test
    fun `reentrant SDK delivery fails closed and acknowledges after outer callback exits`() {
        var inside = false
        var nested = false
        var stopped: Throwable? = null
        val handle =
            source().start({ observation ->
                if (observation.payload == metric(Metric.POWER_W, 123.0)) {
                    inside = true
                    point(DataType.Type.CADENCE, 42.0)
                    assertNull(stopped)
                    inside = false
                } else if (inside) {
                    nested = true
                }
            }) {
                assertFalse(inside)
                stopped = it
            }
        ready()
        point(DataType.Type.POWER, 123.0)
        assertFalse(nested)
        assertTrue(stopped is IllegalStateException)
        assertEquals(0, karoo.system.consumerCount)
        handle.close()
    }

    @Test
    fun `real SDK adapter feeds bounded coordinator and file backed canonical inputs`() {
        val name = "sdk-live-${UUID.randomUUID()}.db"
        val gateChecks = java.util.concurrent.atomic.AtomicInteger()
        com.oxywatt.diagnostic.persistence.InputRepository(karoo.app, name).use { repo ->
            val clock = java.util.concurrent.atomic.AtomicLong()
            val owner = LiveDiagnosticCoordinator(repo, elapsed = clock::get)
            val adapter =
                KarooRideDataSource(
                    KarooSystemService(karoo.app),
                    stamp = {
                        check(Thread.holdsLock(owner.gate))
                        gateChecks.incrementAndGet()
                        owner.stamp()
                    },
                    deliveryGate = owner.gate,
                )
            val handle = adapter.start(owner::accept) { owner.stopped() }
            try {
                ready()
                val baseline = owner.snapshot().received
                point(DataType.Type.POWER, 0.0)
                point(DataType.Type.POWER, 0.0)
                point(DataType.Type.HEART_RATE, 0.0)
                point(DataType.Type.CADENCE, 0.0)
                karoo.system.setStreamState(DataType.Type.POWER, StreamState.Searching)
                karoo.system.setStreamState(DataType.Type.POWER, StreamState.NotAvailable)
                karoo.awaitValue { true.takeIf { owner.snapshot().received == baseline + 6 } }
                assertEquals(0L, owner.snapshot().committed)
                owner.pump(true)
                assertEquals(baseline + 6, owner.snapshot().committed)
                val payloads = repo.observations().takeLast(6).map { it.observation.payload }
                assertEquals(
                    listOf(
                        metric(Metric.POWER_W, 0.0),
                        metric(Metric.POWER_W, 0.0),
                        metric(Metric.HEART_RATE_BPM, null, MissingReason.INVALID_SOURCE_VALUE),
                        metric(Metric.CADENCE_RPM, 0.0),
                        metric(Metric.POWER_W, null, MissingReason.DROPOUT),
                        metric(Metric.POWER_W, null, MissingReason.UNAVAILABLE),
                    ),
                    payloads,
                )
                assertEquals(owner.snapshot().received.toInt(), gateChecks.get())
                assertTrue(
                    repo.observations().all { it.observation.source == SourceRef("karoo", "1.1.9") }
                )
            } finally {
                handle.close()
                owner.pump(true)
            }
            assertEquals(0, karoo.system.consumerCount)
        }
        com.oxywatt.diagnostic.persistence.InputRepository(karoo.app, name).use {
            assertTrue(it.observations().size >= 6)
        }
        karoo.app.deleteDatabase(name)
    }
}
