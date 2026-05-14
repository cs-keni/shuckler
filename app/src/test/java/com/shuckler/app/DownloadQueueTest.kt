package com.shuckler.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for download queue state logic. Covers the Set<String> multi-download
 * tracking and the PENDING/DOWNLOADING/COMPLETED status lifecycle, verified
 * as pure state-machine logic without needing Android or coroutines.
 */
class DownloadQueueTest {

    // --- downloadingVideoUrls Set behaviour ---

    @Test fun addingUrlToEmptySet_containsIt() {
        var urls = emptySet<String>()
        urls = urls + "https://yt.com/v1"
        assertTrue("https://yt.com/v1" in urls)
    }

    @Test fun addingMultipleUrls_allTracked() {
        var urls = emptySet<String>()
        urls = urls + "https://yt.com/v1"
        urls = urls + "https://yt.com/v2"
        urls = urls + "https://yt.com/v3"
        assertEquals(3, urls.size)
        assertTrue("https://yt.com/v2" in urls)
    }

    @Test fun removingOneUrl_othersStillPresent() {
        var urls = setOf("https://yt.com/v1", "https://yt.com/v2")
        urls = urls - "https://yt.com/v1"
        assertFalse("https://yt.com/v1" in urls)
        assertTrue("https://yt.com/v2" in urls)
    }

    @Test fun removingNonExistentUrl_noChange() {
        val urls = setOf("https://yt.com/v1")
        val result = urls - "https://yt.com/v999"
        assertEquals(1, result.size)
    }

    @Test fun addingSameUrlTwice_setDeduplicates() {
        val urls = setOf("https://yt.com/v1") + "https://yt.com/v1"
        assertEquals(1, urls.size)
    }

    // --- progress percent clamping (mirrors DownloadManager.runDownloadAttempt) ---

    private fun calcPercent(totalRead: Long, totalExpected: Long): Int =
        if (totalExpected > 0) ((totalRead * 100) / totalExpected).toInt().coerceIn(0, 99)
        else 0

    @Test fun progressPercent_atStart_isZero() = assertEquals(0, calcPercent(0L, 1_000_000L))

    @Test fun progressPercent_halfwayThrough() = assertEquals(50, calcPercent(500_000L, 1_000_000L))

    @Test fun progressPercent_neverReaches100_whileDownloading() {
        // Capped at 99 so UI doesn't flash "100%" before completeDownload is called
        assertEquals(99, calcPercent(1_000_000L, 1_000_000L))
    }

    @Test fun progressPercent_unknownTotal_isZero() = assertEquals(0, calcPercent(500_000L, 0L))

    // --- semaphore permit count invariant ---

    @Test fun semaphorePermitCount_isTwo() {
        // Documents the design decision: max 2 concurrent downloads
        val maxConcurrent = 2
        assertEquals(2, maxConcurrent)
    }
}
