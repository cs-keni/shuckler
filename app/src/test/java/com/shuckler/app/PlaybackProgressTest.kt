package com.shuckler.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {

    private fun calcProgress(positionMs: Long, durationMs: Long): Float =
        if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    @Test
    fun `returns 0 when duration is zero`() {
        assertEquals(0f, calcProgress(0L, 0L), 0.001f)
        assertEquals(0f, calcProgress(5000L, 0L), 0.001f)
    }

    @Test
    fun `returns correct fraction mid-track`() {
        assertEquals(0.5f, calcProgress(30_000L, 60_000L), 0.001f)
        assertEquals(0.25f, calcProgress(15_000L, 60_000L), 0.001f)
    }

    @Test
    fun `clamps to 0 when position is negative`() {
        assertEquals(0f, calcProgress(-1L, 60_000L), 0.001f)
    }

    @Test
    fun `clamps to 1 when position exceeds duration`() {
        assertEquals(1f, calcProgress(70_000L, 60_000L), 0.001f)
    }

    @Test
    fun `returns 1 at exact end of track`() {
        assertEquals(1f, calcProgress(60_000L, 60_000L), 0.001f)
    }

    @Test
    fun `returns 0 at start of track`() {
        assertEquals(0f, calcProgress(0L, 60_000L), 0.001f)
    }
}
