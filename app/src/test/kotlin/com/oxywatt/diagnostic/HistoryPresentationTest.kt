package com.oxywatt.diagnostic

import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class HistoryPresentationTest {
    private fun entry(day: Long, watts: Double?, id: Long = day) =
        HistoryEntry(
            UUID(0, id),
            Instant.EPOCH.plusSeconds(day * 86400),
            if (watts == null) HistoryReason.NO_WINDOW else HistoryReason.VALUE,
            watts,
        )

    @Test
    fun `calendar gaps remain spaced and zero is a point while absence is not`() {
        val entries = listOf(entry(0, 0.0), entry(1, null), entry(3, 400.0), entry(10, 200.0))
        val points = historyPoints(HistorySnapshot(HistoryStatus.READY, entries = entries))
        assertEquals(3, points.size)
        assertEquals(listOf(0.0, 0.3, 1.0), points.map { it.x })
        assertEquals(listOf(1.0, 0.0, 0.5), points.map { it.y })
    }

    @Test
    fun `one date is centered duplicate workouts are distinct and hidden snapshots have no numbers`() {
        val entries = listOf(entry(0, 0.0, 1), entry(0, 0.0, 2))
        val state = HistorySnapshot(HistoryStatus.READY, entries = entries)
        assertEquals(listOf(0.5, 0.5), historyPoints(state).map { it.x })
        assertEquals(listOf(1.0, 1.0), historyPoints(state).map { it.y })
        assertTrue(historyPoints(state.copy(status = HistoryStatus.READ_ERROR)).isEmpty())
        assertTrue(historyPoints(HistorySnapshot(HistoryStatus.READY)).isEmpty())
    }
}
