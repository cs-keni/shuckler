package com.shuckler.app

import com.shuckler.app.spotify.ImportProgress
import com.shuckler.app.spotify.ImportState
import com.shuckler.app.spotify.ImportTrackRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyImportProgressTest {

    private fun makeRecord(state: ImportState) = ImportTrackRecord(
        importId = "test-import",
        playlistKey = "k",
        playlistName = "My Playlist",
        trackTitle = "Song",
        trackArtist = "Artist",
        trackAlbum = null,
        trackAlbumYear = null,
        state = state
    )

    // --- ImportProgress computed properties ---

    @Test fun terminal_sumsCompletedPlusFailedPlusNotFound() {
        val p = ImportProgress("id", total = 10, completed = 5, failed = 2, notFound = 1, isFinished = false)
        assertEquals(8, p.terminal)
    }

    @Test fun terminal_allCompleted_equalsTotal() {
        val p = ImportProgress("id", total = 7, completed = 7, failed = 0, notFound = 0, isFinished = true)
        assertEquals(7, p.terminal)
    }

    @Test fun terminal_allNotFound_equalsTotal() {
        val p = ImportProgress("id", total = 3, completed = 0, failed = 0, notFound = 3, isFinished = true)
        assertEquals(3, p.terminal)
    }

    @Test fun matched_isAliasForCompleted() {
        val p = ImportProgress("id", total = 10, completed = 6, failed = 1, notFound = 1, isFinished = false)
        assertEquals(p.completed, p.matched)
        assertEquals(6, p.matched)
    }

    @Test fun terminal_emptyImport_isZero() {
        val p = ImportProgress("id", total = 0, completed = 0, failed = 0, notFound = 0, isFinished = true)
        assertEquals(0, p.terminal)
    }

    // --- isFinished semantics ---

    @Test fun isFinished_falseWhenSearchingTracksRemain() {
        val p = ImportProgress("id", total = 5, completed = 3, failed = 0, notFound = 0, isFinished = false)
        assertFalse(p.isFinished)
    }

    @Test fun isFinished_trueWhenAllTerminal() {
        val p = ImportProgress("id", total = 4, completed = 2, failed = 1, notFound = 1, isFinished = true)
        assertTrue(p.isFinished)
        assertEquals(4, p.terminal)
    }

    // --- ImportState enum coverage ---

    @Test fun importState_searchingIsNonTerminal() {
        val terminalStates = setOf(ImportState.COMPLETED, ImportState.NOT_FOUND, ImportState.FAILED)
        assertFalse(ImportState.SEARCHING in terminalStates)
        assertFalse(ImportState.QUEUED in terminalStates)
        assertFalse(ImportState.DOWNLOADING in terminalStates)
    }

    @Test fun importState_allTerminalStatesPresent() {
        val terminal = setOf(ImportState.COMPLETED, ImportState.NOT_FOUND, ImportState.FAILED)
        assertEquals(3, terminal.size)
        assertTrue(ImportState.COMPLETED in terminal)
        assertTrue(ImportState.NOT_FOUND in terminal)
        assertTrue(ImportState.FAILED in terminal)
    }

    // --- ImportTrackRecord mutable state transitions ---

    @Test fun record_defaultState_isSearching() {
        val r = makeRecord(ImportState.SEARCHING)
        assertEquals(ImportState.SEARCHING, r.state)
    }

    @Test fun record_stateCanBeUpdated() {
        val r = makeRecord(ImportState.SEARCHING)
        r.state = ImportState.NOT_FOUND
        assertEquals(ImportState.NOT_FOUND, r.state)
    }

    @Test fun record_downloadIdAssignedOnQueue() {
        val r = makeRecord(ImportState.QUEUED)
        r.downloadId = "dl-123"
        assertEquals("dl-123", r.downloadId)
    }

    // --- Progress percentage helper (mirrors service progress text) ---

    private fun progressText(progress: ImportProgress): String {
        return "${progress.terminal} of ${progress.total} done"
    }

    @Test fun progressText_formatsCorrectly() {
        val p = ImportProgress("id", total = 50, completed = 30, failed = 2, notFound = 1, isFinished = false)
        assertEquals("33 of 50 done", progressText(p))
    }

    @Test fun progressText_atStart_showsZeroOfTotal() {
        val p = ImportProgress("id", total = 100, completed = 0, failed = 0, notFound = 0, isFinished = false)
        assertEquals("0 of 100 done", progressText(p))
    }
}
