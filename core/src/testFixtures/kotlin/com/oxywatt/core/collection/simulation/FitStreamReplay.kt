package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.*
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

fun replayFitStream(
    manifestText: String,
    openProjection: () -> java.io.InputStream,
    speed: Int?,
    accept: (SourceObservation) -> Unit,
    clock: VirtualTimeline = VirtualTimeline(),
    epoch: UUID = UUID(0, 101),
    awaitPlaybackDeadline: (Long) -> Unit = {},
): Long {
    val manifest = manifestText
    fun text(key: String): String =
        checkNotNull(Regex("\"$key\": \"([^\"]+)\"").find(manifest)) { "Missing manifest $key" }
            .groupValues[1]
    // This bridge accepts the canonical JSON layout emitted by the pinned developer exporter.
    require(text("schema") == "fit-replay-v1")
    require(text("decoder") == "fitdecode" && text("decoder_version") == "0.11.0")
    require(text("normalization_version") == "fit-normalization-v1")
    require(text("provenance") in setOf("private-owner-input", "generated-synthetic-v1"))
    require(text("timestamp_policy") == "ordered-original-file-time-v1")
    require(text("scheduler_policy") == "timers-before-input-v1")
    val digest = MessageDigest.getInstance("SHA-256")
    openProjection().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            digest.update(buffer, 0, size)
        }
    }
    require(digest.digest().joinToString("") { "%02x".format(it) } == text("projection_sha256")) {
        "Projection hash mismatch"
    }
    val end =
        checkNotNull(Regex("\"end_offset_nanos\": ([0-9]+)").find(manifest)).groupValues[1].toLong()
    val expectedRows =
        checkNotNull(Regex("\"projected_rows\": ([0-9]+)").find(manifest)).groupValues[1].toLong()
    require(expectedRows > 0)
    val fileOrigin = Instant.parse(text("file_origin"))
    val receiptOrigin = Instant.parse("2026-01-01T00:00:00Z")

    openProjection().bufferedReader().use { reader ->
        require(
            reader.readLine() ==
                "message_index\tfile_timestamp\toffset_nanos\tkind\tvalue\treason\ttimer_type"
        )
        var previousMessage = -1L
        val decoded =
            reader.lineSequence().map { line ->
                val cells = line.split('\t')
                require(cells.size == 7)
                val messageIndex = cells[0].toLong()
                require(messageIndex >= previousMessage)
                previousMessage = messageIndex
                val stamp = Instant.parse(cells[1])
                val offset = cells[2].toLong()
                require(Duration.between(fileOrigin, stamp).toNanos() == offset)
                val payload =
                    if (cells[3] == "STATE") {
                        val expected =
                            when (cells[6]) {
                                "start" -> RecordingState.RECORDING
                                "stop",
                                "stop_all" -> RecordingState.PAUSED
                                else -> error("Unsupported timer")
                            }
                        require(cells[4] == expected.name && cells[5] == "-")
                        InputPayload.Recording(expected)
                    } else {
                        require(cells[6] == "-")
                        InputPayload.MetricReading(
                            Metric.valueOf(cells[3]),
                            cells[4].takeUnless { it == "-" }?.toDouble(),
                            cells[5].takeUnless { it == "-" }?.let(MissingReason::valueOf),
                            SourceTime(stamp, SourceTimeMeaning.FILE_RECORD),
                        )
                    }
                ScheduledObservation(offset, payload)
            }
        val events = sequence {
            var rows = 0L
            var lastOffset = -1L
            for (event in decoded) {
                rows++
                lastOffset = event.offsetNanos
                yield(event)
            }
            require(rows == expectedRows) { "Projection row count mismatch" }
            require(lastOffset == end) { "Manifest end does not match the last file observation" }
        }
        val source =
            SimulatedRideDataSource(
                events,
                clock,
                epoch,
                receiptOrigin,
                end,
                text("normalization_version"),
                speed,
                awaitPlaybackDeadline,
            )
        var failure: Throwable? = null
        source.start(accept) { failure = it }.use { while (source.pump()) {} }
        failure?.let { throw it }
    }
    return clock.now
}
