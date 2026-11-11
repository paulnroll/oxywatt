package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*
import org.junit.Assume.assumeTrue

class FitScenarioReplayTest {
    private val fixture = Path.of("src/test/resources/fit-scenario")

    @Test
    fun syntheticProjectionHasLiteralExpectedInputsAtAllSpeeds() {
        val outputs =
            listOf(1, 10, null).map { speed ->
                val result = mutableListOf<SourceObservation>()
                assertEquals(9_000_000_000L, replayFitScenario(fixture, speed, result::add))
                result
            }
        assertEquals(outputs[0], outputs[1])
        assertEquals(outputs[0], outputs[2])
        val result = outputs[0]
        assertEquals(
            listOf(0L, 0L, 0L, 0L, 2L, 2L, 2L, 3L, 8L, 8L, 8L, 8L, 9L, 9L, 9L),
            result.map { it.stamp.elapsedNanos / 1_000_000_000L },
        )
        assertEquals(
            listOf(RecordingState.RECORDING, RecordingState.PAUSED, RecordingState.RECORDING),
            result.mapNotNull { (it.payload as? InputPayload.Recording)?.state },
        )
        val power = result.mapNotNull {
            (it.payload as? InputPayload.MetricReading)?.takeIf { it.metric == Metric.POWER_W }
        }
        assertEquals(listOf(200.0, 0.0, 210.0, null), power.map { it.value })
        assertEquals(MissingReason.INVALID_SOURCE_VALUE, power.last().missingReason)
        assertTrue(
            result
                .mapNotNull { it.payload as? InputPayload.MetricReading }
                .all { it.sourceTime?.meaning == SourceTimeMeaning.FILE_RECORD }
        )
    }

    @Test
    fun projectionHashIsValidatedBeforeAnyDelivery() {
        val directory = Files.createTempDirectory("oxywatt-replay-corruption")
        try {
            Files.copy(fixture.resolve("manifest.json"), directory.resolve("manifest.json"))
            Files.writeString(
                directory.resolve("observations.tsv"),
                Files.readString(fixture.resolve("observations.tsv")).replace("200", "201"),
            )
            var delivered = 0
            assertFailsWith<IllegalArgumentException> {
                replayFitScenario(directory, null) { delivered++ }
            }
            assertEquals(0, delivered)
        } finally {
            Files.deleteIfExists(directory.resolve("observations.tsv"))
            Files.deleteIfExists(directory.resolve("manifest.json"))
            Files.delete(directory)
        }
    }

    @Test
    fun privateOwnerProjectionReplaysAtEverySpeedWhenExplicitlyEnabled() {
        val location = System.getenv("OXYWATT_PRIVATE_FIT_REPLAY_DIR")
        assumeTrue("Private FIT verification is opt-in", location != null)
        val summaries =
            listOf(1, 10, null).map { speed ->
                val digest = MessageDigest.getInstance("SHA-256")
                var count = 0
                val end =
                    replayFitScenario(Path.of(location), speed) {
                        digest.update((it.toString() + "\n").toByteArray(Charsets.UTF_8))
                        count++
                    }
                Triple(count, end, digest.digest().toList())
            }
        assertTrue(summaries[0].first > 0)
        assertEquals(summaries[0], summaries[1])
        assertEquals(summaries[0], summaries[2])
    }
}
