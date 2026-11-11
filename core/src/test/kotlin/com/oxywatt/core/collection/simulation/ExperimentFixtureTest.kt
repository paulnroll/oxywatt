package com.oxywatt.core.collection.simulation

import kotlin.test.*
import org.junit.Test

class ExperimentFixtureTest {
    @Test
    fun permissionIsCheckedBeforeReplay() {
        val fixture = experimentFixture("constant")
        var delivered = 0
        assertFailsWith<IllegalArgumentException> {
            fixture
                .copy(rights = fixture.rights.copy(processing = UsePermission.UNKNOWN))
                .observations()
                .forEach { delivered++ }
        }
        assertEquals(0, delivered)
        assertEquals(63, fixture.observations().count())
    }

    @Test
    fun manifestMutationsAndMalformedDescriptorsAreDetected() {
        val f = experimentFixture("constant")
        val baseline = f.digest()
        val changed =
            listOf(
                f.copy(manifest = f.manifest.copy(seed = 1)),
                f.copy(manifest = f.manifest.copy(origin = java.time.Instant.ofEpochSecond(1))),
                f.copy(
                    events =
                        f.events.mapIndexed { i, e ->
                            if (i == 3) e.copy(offsetNanos = e.offsetNanos + 1) else e
                        }
                ),
                f.copy(
                    events =
                        f.events.mapIndexed { i, e -> if (i == 3) e.copy(utcShiftNanos = 1) else e }
                ),
                f.copy(
                    events = f.events.toMutableList().also { java.util.Collections.swap(it, 0, 1) }
                ),
                f.copy(recipe = f.recipe + ";noise=1"),
                f.copy(policies = f.policies + ("budget" to "999")),
                f.copy(policies = f.policies + ("preprocessing" to "different")),
                f.copy(
                    expectedEnd = f.expectedEnd + 1,
                    manifest = f.manifest.copy(endNanos = f.expectedEnd + 1),
                    events =
                        f.events.dropLast(1) +
                            f.events.last().copy(offsetNanos = f.expectedEnd + 1),
                ),
                f.copy(rights = f.rights.copy(evidence = "revised evidence")),
            )
        changed.forEach { assertNotEquals(baseline, it.digest()) }
        listOf(
                f.copy(expectedCount = 1),
                f.copy(expectedEnd = 1),
                f.copy(schema = "unknown"),
                f.copy(units = "kW"),
                f.copy(profiles = emptyList()),
            )
            .forEach {
                assertFailsWith<IllegalArgumentException> { it.observations().toList() }
            }
        assertFailsWith<IllegalArgumentException> {
            f.copy(rights = f.rights.copy(redistribution = UsePermission.UNKNOWN)).validate(true)
        }
        assertNotEquals(f.manifest.hash(), f.linkedManifest().hash())
    }

    @Test
    fun profilesPreserveMissingInputsAndNeverApproveModels() {
        val profiles = experimentProfiles()
        assertNotEquals(profiles[0].digest(), profiles[1].digest())
        profiles.forEach { p ->
            assertEquals("model_not_approved", p.automaticResult)
            assertNull(p.protocol)
            val missing =
                p.copy(
                    snapshot = p.snapshot.copy(massKg = null, configuredMaxHeartRateBpm = null),
                    evidence = p.evidence + mapOf("mass" to "missing", "hrmax" to "missing"),
                )
            assertNull(missing.snapshot.massKg)
            assertNull(missing.snapshot.configuredMaxHeartRateBpm)
            assertNotEquals(p.digest(), missing.digest())
            assertNotEquals(p.digest(), p.copy(snapshot = p.snapshot.copy(version = "2")).digest())
            assertNotEquals(
                p.digest(),
                p.copy(snapshot = p.snapshot.copy(effectiveAt = java.time.Instant.ofEpochSecond(1)))
                    .digest(),
            )
        }
        for (v in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                com.oxywatt.core.collection.ProfileSetting(
                    v,
                    com.oxywatt.core.collection.SettingOrigin.MANUAL,
                )
            }
        }
    }

    @Test
    fun seededRecipesRegenerateFreshSequencesAtEverySpeed() {
        fun make(): ExperimentFixture {
            val f = experimentFixture("constant")
            val generated =
                signal(
                        com.oxywatt.core.collection.Metric.POWER_W,
                        (0L until 60).map { it * 1_000_000_000 }.asSequence(),
                        listOf(SignalSegment(0, 60_000_000_000, 200.0)),
                        seed = 42,
                        noiseAmplitude = 3.0,
                    )
                    .toList()
            return f.copy(
                manifest = f.manifest.copy(seed = 42),
                events = f.events.take(2) + generated + f.events.last(),
                recipe =
                    "signal-v1;java.util.Random;seed=42;offsets=0..59 seconds;segment=[0,60)200W;uniform[-3,3);dropouts=[];silence=[];channelOrder=POWER_W",
            )
        }
        val f = make()
        assertEquals(f, make())
        assertEquals(f.digest(), make().digest())
        val outputs =
            listOf(1, 10, null).map { speed ->
                val result = mutableListOf<com.oxywatt.core.collection.SourceObservation>()
                val source =
                    SimulatedRideDataSource(
                        f.observations(),
                        VirtualTimeline(),
                        f.manifest.epoch,
                        f.manifest.origin,
                        f.expectedEnd,
                        speed = speed,
                    )
                source.start(result::add) { it?.let { throw it } }.use { while (source.pump()) {} }
                result
            }
        assertEquals(outputs[0], outputs[1])
        assertEquals(outputs[0], outputs[2])
        assertEquals(63, outputs[0].size)
    }

    @Test
    fun hashesHaveIndependentEncodingVectorsAndLegacyIdentity() {
        // SHA-256 values computed from literal UTF-8 preimages, independent of the encoder.
        fun sha(s: String) =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(s.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        assertEquals(sha("S1:xN;S0:S2:é"), semanticDigest("x", listOf(null, "", "é")))
        assertNotEquals(semanticDigest("x", listOf(null)), semanticDigest("x", listOf("null")))
        assertNotEquals(
            semanticDigest("x", listOf("a", "b")),
            semanticDigest("x", listOf("b", "a")),
        )
        val m = experimentFixture("constant").manifest
        // Legacy preimage is checked independently below using literal field lengths.
        val fields =
            listOf(
                "simulated",
                "constant",
                "0",
                "00000000-0000-0000-0000-0000000001f6",
                "1970-01-01T00:00:00Z",
                "60000000000",
                "1",
                "canonical-v1",
                "invented-profile-v1",
                "timers-before-input-v1",
            )
        assertEquals("f59ee3169b52007a2d50b708ede5926ad81e8248b250b872392760d57c8a336c", m.hash())
        assertEquals(sha(fields.joinToString("") { "${it.length}:$it" }), m.hash())
        assertNotEquals(m.hash(), m.copy(epoch = java.util.UUID(0, 999)).hash())
        assertNotEquals(m.hash(), m.copy(profileVersion = "2").hash())
    }

    @Test
    fun generatedFitDescriptorReferencesPinnedExportAndValidatesBeforeDelivery() {
        val path = java.nio.file.Path.of("src/test/resources/fit-scenario")
        val manifest = java.nio.file.Files.readString(path.resolve("manifest.json"))
        val projection = java.nio.file.Files.readAllBytes(path.resolve("observations.tsv"))
        val sha =
            java.security.MessageDigest.getInstance("SHA-256").digest(projection).joinToString("") {
                "%02x".format(it)
            }
        assertTrue(manifest.contains("\"projection_sha256\": \"$sha\""))
        assertTrue(manifest.contains("\"decoder_version\": \"0.11.0\""))
        val outputs =
            listOf(1, 10, null).map { speed ->
                val result = mutableListOf<com.oxywatt.core.collection.SourceObservation>()
                replayFitStream(manifest, { projection.inputStream() }, speed, result::add)
                result
            }
        val base = experimentFixture("constant")
        val descriptor =
            base.copy(
                id = "generated-fit",
                manifest =
                    base.manifest.copy(
                        scenario = "generated-fit",
                        normalizationVersion = "fit-normalization-v1",
                        endNanos = 9_000_000_000,
                    ),
                events = outputs[0].map { ScheduledObservation(it.stamp.elapsedNanos, it.payload) },
                expectedCount = 15,
                expectedEnd = 9_000_000_000,
                recipe =
                    "fit-replay-v1: existing generated export; original FILE_RECORD time retained",
                expectation =
                    "15 canonical rows; power=200,0,210,missing; timer start/pause/start; EOF recording interrupted",
                sourceReferences =
                    listOf(
                        "generated-synthetic-v1:fit-replay-v1:fitdecode:0.11.0",
                        "input:4c47c1a8988224bc56c72e5b642c005e3955554cb33dfcc8daaf03beb07d8e15",
                        "projection:$sha",
                    ),
            )
        descriptor.validate(true)
        assertEquals(15, descriptor.observations(descriptor.digest()).count())
        assertNotEquals(base.digest(), descriptor.digest())
        assertEquals(15, outputs[0].size)
        assertEquals(outputs[0], outputs[1])
        assertEquals(outputs[0], outputs[2])
        var delivered = 0
        assertFailsWith<IllegalArgumentException> {
            replayFitStream(manifest, { "corrupted".byteInputStream() }, null, { delivered++ })
        }
        assertEquals(0, delivered)
    }

    @Test
    fun alteredDigestAndUnresolvedProfileAreRejectedBeforeDelivery() {
        val f = experimentFixture("constant")
        var count = 0
        assertFailsWith<IllegalArgumentException> { f.observations("wrong").forEach { count++ } }
        val changed =
            f.copy(
                events =
                    f.events.take(2) +
                        ScheduledObservation(
                            0,
                            com.oxywatt.core.collection.InputPayload.Profile(
                                f.profiles[0].snapshot.copy(version = "unknown")
                            ),
                        ) +
                        f.events.drop(3)
            )
        assertFailsWith<IllegalArgumentException> { changed.observations().forEach { count++ } }
        assertFailsWith<IllegalArgumentException> {
            f.copy(policies = f.policies + ("budget" to "999")).observations().forEach { count++ }
        }
        assertEquals(0, count)
    }
}
