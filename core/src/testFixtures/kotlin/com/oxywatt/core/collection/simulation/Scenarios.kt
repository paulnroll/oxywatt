package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Random
import java.util.UUID

/** All values affecting replay are explicit, including virtual end during silence. */
data class RunManifest(
    val scenario: String,
    val seed: Long,
    val epoch: UUID,
    val origin: Instant,
    val endNanos: Long,
    val generatorVersion: String = "1",
    val normalizationVersion: String = "canonical-v1",
    val profileVersion: String = "none",
    val schedulerPolicy: String = "timers-before-input-v1",
    val fixtureDigest: String? = null,
) {
    init {
        require(endNanos >= 0)
        require(fixtureDigest == null || fixtureDigest.matches(Regex("[0-9a-f]{64}")))
        listOf(scenario, generatorVersion, normalizationVersion, profileVersion, schedulerPolicy)
            .forEach {
                require(it.isNotBlank())
            }
    }

    fun hash(): String {
        val fields =
            listOf(
                "simulated",
                scenario,
                seed.toString(),
                epoch.toString(),
                origin.toString(),
                endNanos.toString(),
                generatorVersion,
                normalizationVersion,
                profileVersion,
                schedulerPolicy,
            )
        val versionedFields =
            if (fixtureDigest == null) fields
            else listOf("descriptor-linked-run-v1") + fields + fixtureDigest
        val canonical =
            versionedFields.joinToString("") {
                "${it.toByteArray(StandardCharsets.UTF_8).size}:$it"
            }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

/** Half-open linear segments; constant and effort/interval tables use equal endpoints. */
data class SignalSegment(val start: Long, val end: Long, val from: Double, val to: Double = from) {
    init {
        require(start >= 0 && end > start)
        require(from.isFinite() && to.isFinite())
    }
}

fun signal(
    metric: Metric,
    offsets: Sequence<Long>,
    segments: List<SignalSegment>,
    seed: Long = 0,
    noiseAmplitude: Double = 0.0,
    dropouts: List<LongRange> = emptyList(),
    silence: List<LongRange> = emptyList(),
): Sequence<ScheduledObservation> {
    require(noiseAmplitude.isFinite() && noiseAmplitude >= 0)
    require(segments.zipWithNext().all { (a, b) -> a.end <= b.start })
    val shape = segments.toList()
    val missing = dropouts.toList()
    val silent = silence.toList()
    return sequence {
        val random = Random(seed)
        var previous = -1L
        for (offset in offsets) {
            require(offset >= 0 && offset >= previous)
            previous = offset
            val segment = shape.firstOrNull { offset >= it.start && offset < it.end } ?: continue
            val noise = (random.nextDouble() * 2 - 1) * noiseAmplitude
            if (silent.any { offset in it }) continue
            val payload =
                if (missing.any { offset in it })
                    InputPayload.MetricReading(metric, null, MissingReason.DROPOUT)
                else
                    InputPayload.MetricReading(
                        metric,
                        segment.from +
                            (segment.to - segment.from) *
                                ((offset - segment.start).toDouble() /
                                    (segment.end - segment.start)) +
                            noise,
                    )
            yield(ScheduledObservation(offset, payload))
        }
    }
}

/** One lookahead per channel; callers provide factories so each run has fresh iterators. */
fun mergeChannels(
    vararg channels: () -> Sequence<ScheduledObservation>
): Sequence<ScheduledObservation> = sequence {
    val iterators = channels.map { it().iterator() }
    val heads = iterators.map { if (it.hasNext()) it.next() else null }.toMutableList()
    while (true) {
        val index =
            heads.indices
                .filter { heads[it] != null }
                .minWithOrNull(compareBy<Int> { heads[it]!!.offsetNanos }.thenBy { it }) ?: break
        yield(heads[index]!!)
        heads[index] = if (iterators[index].hasNext()) iterators[index].next() else null
    }
}
