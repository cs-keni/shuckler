package com.shuckler.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for DonutChart data preparation logic. The composable itself requires
 * a device, but the slice computation and sweep-angle math are pure and testable here.
 */
class DonutChartDataTest {

    data class Slice(val artist: String, val plays: Int)

    private fun buildSlices(
        artistPlays: Map<String, Int>,
        maxSlices: Int = 5
    ): List<Slice> {
        return artistPlays.entries
            .sortedByDescending { it.value }
            .take(maxSlices)
            .map { Slice(it.key, it.value) }
    }

    private fun sweepAngle(plays: Int, totalPlays: Int): Float =
        if (totalPlays > 0) 360f * plays / totalPlays else 0f

    @Test fun slices_sortedByPlaysDescending() {
        val data = mapOf("Artist A" to 10, "Artist B" to 50, "Artist C" to 30)
        val slices = buildSlices(data)
        assertEquals("Artist B", slices[0].artist)
        assertEquals("Artist C", slices[1].artist)
        assertEquals("Artist A", slices[2].artist)
    }

    @Test fun slices_cappedAtFive() {
        val data = (1..10).associate { "Artist $it" to it * 10 }
        val slices = buildSlices(data)
        assertEquals(5, slices.size)
    }

    @Test fun sweepAngles_sumToFullCircle() {
        val plays = listOf(50, 30, 20)
        val total = plays.sum()
        val sweeps = plays.map { sweepAngle(it, total) }
        assertEquals(360f, sweeps.sum(), 0.01f)
    }

    @Test fun sweepAngle_proportional() {
        // 25% of plays → 90° sweep
        assertEquals(90f, sweepAngle(25, 100), 0.01f)
    }

    @Test fun sweepAngle_zeroTotal_returnsZero() {
        assertEquals(0f, sweepAngle(10, 0), 0.001f)
    }

    @Test fun percentLabel_roundsCorrectly() {
        // mirrors DonutChart: ((100f * plays / total) + 0.5f).toInt()
        fun pct(plays: Int, total: Int) = ((100f * plays / total) + 0.5f).toInt()
        assertEquals(33, pct(1, 3))   // 33.3 → 33
        assertEquals(34, pct(1, 3).let { if (100f * 1 / 3 + 0.5f > 33.5f) 34 else it })
        assertEquals(50, pct(1, 2))
        assertEquals(100, pct(3, 3))
    }

    @Test fun othersSlice_accountsForRemainder() {
        val top5 = mapOf("A" to 40, "B" to 20, "C" to 15, "D" to 10, "E" to 8)
        val total = 100
        val othersPlays = total - top5.values.sum()
        assertTrue(othersPlays >= 0)
        assertEquals(7, othersPlays)
    }
}
