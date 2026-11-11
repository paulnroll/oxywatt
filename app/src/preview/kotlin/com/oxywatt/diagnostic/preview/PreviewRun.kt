package com.oxywatt.diagnostic.preview

import android.content.Context
import com.oxywatt.core.collection.*
import com.oxywatt.core.collection.simulation.*
import com.oxywatt.diagnostic.R
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Application lifetime only; no promise of process survival. No per-event UI messages. */
object PreviewRun {
    private val running = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    @Volatile
    var snapshot: String? = null
        private set

    fun stop() {
        cancelled.set(true)
    }

    fun start(context: Context, fit: Boolean, restoreOnly: Boolean = false): Boolean {
        if (!running.compareAndSet(false, true)) return false
        cancelled.set(false)
        Thread(
                {
                    val clock = VirtualTimeline()
                    val epoch = UUID.randomUUID()
                    val name =
                        if (fit) "oxywatt-synthetic-fit-demo.db" else "oxywatt-synthetic-demo.db"
                    val origin = if (fit) "FIT REPLAY" else "DEMO"
                    try {
                        PreviewPipeline(context, name, epoch).use { pipeline ->
                            if (!restoreOnly) {
                                val manifest =
                                    RunManifest(
                                        if (fit) "synthetic-fit-v1" else "preview-v1",
                                        0,
                                        epoch,
                                        Instant.EPOCH,
                                        if (fit) 9_000_000_000 else 10_000_000_000,
                                        normalizationVersion =
                                            if (fit) "fit-normalization-v1" else "canonical-v1",
                                    )
                                val projection =
                                    if (fit)
                                        context.assets
                                            .open("fit-scenario/manifest.json")
                                            .bufferedReader()
                                            .use { it.readText() }
                                    else ""
                                check(
                                    context
                                        .getSharedPreferences(
                                            "synthetic-run-manifest",
                                            Context.MODE_PRIVATE,
                                        )
                                        .edit()
                                        .putString(
                                            epoch.toString(),
                                            "$origin:generated-synthetic-v1:database=$name:$manifest:hash=${manifest.hash()}:$projection",
                                        )
                                        .commit()
                                )
                            }
                            val began = System.nanoTime()
                            var lastPublish = Long.MIN_VALUE
                            fun publish(finished: Boolean = false) {
                                val wall = System.nanoTime()
                                if (
                                    !finished &&
                                        lastPublish != Long.MIN_VALUE &&
                                        wall - lastPublish < 500_000_000
                                )
                                    return
                                lastPublish = wall
                                val values =
                                    Metric.entries.joinToString("") { metric ->
                                        val observation = pipeline.readings[metric]
                                        val reading =
                                            observation?.payload as? InputPayload.MetricReading
                                        val value =
                                            reading?.value?.toString()
                                                ?: reading?.missingReason?.name
                                                ?: context.getString(R.string.preview_unknown)
                                        val age =
                                            observation
                                                ?.stamp
                                                ?.takeIf { it.epochId == epoch }
                                                ?.let {
                                                    "%.1f s"
                                                        .format(
                                                            java.util.Locale.US,
                                                            (clock.now - it.elapsedNanos) / 1e9,
                                                        )
                                                } ?: context.getString(R.string.preview_unknown)
                                        context.getString(
                                            R.string.preview_metric,
                                            metric.name,
                                            value,
                                            age,
                                        )
                                    }
                                snapshot =
                                    context.getString(
                                        R.string.preview_summary,
                                        origin,
                                        context.getString(
                                            if (finished) R.string.preview_finished
                                            else R.string.preview_running
                                        ),
                                        clock.now / 1e9,
                                        pipeline.checkpoint.latestSession?.phase?.name
                                            ?: context.getString(R.string.preview_unknown),
                                        pipeline.committedCheckpoint?.latestSession?.phase?.name
                                            ?: context.getString(R.string.preview_unknown),
                                        values,
                                        pipeline.saved,
                                        pipeline.pending,
                                        pipeline.loss,
                                        pipeline.lastCommit?.let { "${it/1e9} s" }
                                            ?: context.getString(R.string.preview_unknown),
                                        pipeline.error
                                            ?: context.getString(R.string.preview_healthy),
                                        pipeline.committedReadings.entries.joinToString {
                                            (metric, reading) ->
                                            "${metric.name}: ${reading.value ?: reading.missingReason}"
                                        },
                                        pipeline.gaps,
                                    )
                            }
                            fun waitUntil(deadline: Long) {
                                while (!cancelled.get() && System.nanoTime() - began < deadline) {
                                    Thread.sleep(50)
                                    clock.advanceTo(minOf(deadline, System.nanoTime() - began))
                                    publish()
                                }
                                if (cancelled.get())
                                    throw java.util.concurrent.CancellationException()
                            }
                            fun tick(time: Long) {
                                clock.at(time) {
                                    pipeline.advance(time)
                                    publish()
                                    if (time < 10_000_000_000) tick(time + 500_000_000)
                                }
                            }
                            if (restoreOnly) {
                                publish(true)
                                return@use
                            }
                            tick(0)
                            if (fit) {
                                val manifest =
                                    context.assets
                                        .open("fit-scenario/manifest.json")
                                        .bufferedReader()
                                        .use { it.readText() }
                                require(manifest.contains("generated-synthetic-v1"))
                                try {
                                    replayFitStream(
                                        manifest,
                                        { context.assets.open("fit-scenario/observations.tsv") },
                                        1,
                                        { pipeline.accept(it) },
                                        clock,
                                        epoch,
                                        ::waitUntil,
                                    )
                                } catch (_: java.util.concurrent.CancellationException) {}
                            } else {
                                val source =
                                    SimulatedRideDataSource(
                                        previewScenario(),
                                        clock,
                                        epoch,
                                        Instant.EPOCH,
                                        10_000_000_000,
                                        speed = 1,
                                        awaitPlaybackDeadline = ::waitUntil,
                                    )
                                source.start({ pipeline.accept(it) }, {}).use {
                                    while (source.pump()) {}
                                }
                            }
                            pipeline.stopped()
                            publish(true)
                        }
                    } catch (failure: Exception) {
                        snapshot =
                            context.getString(
                                R.string.preview_failure,
                                origin,
                                failure.javaClass.simpleName.take(80),
                            )
                    } finally {
                        running.set(false)
                    }
                },
                "oxywatt-demo-worker",
            )
            .start()
        return true
    }
}
