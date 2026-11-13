package com.oxywatt.diagnostic

import org.junit.Assert.*
import org.junit.Test

class SavedFieldPresentationTest {
    @Test
    fun `zero stays on the baseline and a missing duration breaks both neighboring connections`() {
        val points =
            listOf(
                FieldPower(5, 1000.0, false),
                FieldPower(30, null, false),
                FieldPower(60, 0.0, false),
                FieldPower(3600, 100.0, false),
            )
        val chart = fieldChart(points)
        assertEquals(3, chart.size)
        assertEquals(0.0, chart.first().x, 1e-12)
        assertEquals(0.0, chart.first().y, 1e-12)
        assertEquals(1.0, chart[1].y, 1e-12)
        assertFalse(chart[1].connected)
        assertTrue(chart.last().connected)
        assertEquals(1.0, chart.last().x, 1e-12)
        assertEquals(0.9, chart.last().y, 1e-12)
    }

    @Test
    fun `log duration geometry uses fixed endpoint span rather than equally spaced duration slots`() {
        val chart =
            fieldChart(
                listOf(
                    FieldPower(5, 100.0, false),
                    FieldPower(60, 50.0, false),
                    FieldPower(3600, 0.0, false),
                )
            )
        // Independent literal ln(12)/ln(720) reference, not a copy of production coordinate code.
        assertEquals(0.37768836752302826, chart[1].x, 0.000001)
        assertEquals(0.5, chart[1].y, 1e-12)
    }
}
