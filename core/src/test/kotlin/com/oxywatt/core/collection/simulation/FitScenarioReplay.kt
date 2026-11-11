package com.oxywatt.core.collection.simulation

import com.oxywatt.core.collection.SourceObservation
import java.nio.file.Files
import java.nio.file.Path

fun replayFitScenario(directory: Path, speed: Int?, accept: (SourceObservation) -> Unit): Long =
    replayFitStream(
        Files.readString(directory.resolve("manifest.json")),
        { Files.newInputStream(directory.resolve("observations.tsv")) },
        speed,
        accept,
    )
