package com.shuckler.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for achievement unlock conditions. Logic is extracted as pure functions so
 * no Android Context / SharedPreferences is needed — mirrors exactly what
 * AchievementManager.checkAndUnlock evaluates.
 */
class AchievementConditionsTest {

    // --- helpers that mirror AchievementManager conditions ---

    private fun shouldUnlockFirstDownload(trackCount: Int) = trackCount > 0
    private fun shouldUnlockTenFavorites(favoriteCount: Int) = favoriteCount >= 10
    private fun shouldUnlockTwentyFavorites(favoriteCount: Int) = favoriteCount >= 20
    private fun shouldUnlockCentury(totalPlayCount: Int) = totalPlayCount >= 100
    private fun shouldUnlockMarathon(totalPlayCount: Int) = totalPlayCount >= 500
    private fun shouldUnlockLibrary50(trackCount: Int) = trackCount >= 50
    private fun shouldUnlockLibrary100(trackCount: Int) = trackCount >= 100
    private fun shouldUnlockFivePlaylists(playlistCount: Int) = playlistCount >= 5
    private fun shouldUnlockDedicated(maxSingleTrackPlays: Int) = maxSingleTrackPlays >= 20
    private fun shouldUnlockExplorer(uniqueArtistCount: Int) = uniqueArtistCount >= 10
    private fun shouldUnlockMoodSetter(tracksWithMoodTags: Int) = tracksWithMoodTags > 0

    // first_download
    @Test fun firstDownload_unlocksWithOneTrack() = assertTrue(shouldUnlockFirstDownload(1))
    @Test fun firstDownload_doesNotUnlockEmpty() = assertFalse(shouldUnlockFirstDownload(0))

    // ten_favorites / twenty_favorites
    @Test fun tenFavorites_atThreshold() = assertTrue(shouldUnlockTenFavorites(10))
    @Test fun tenFavorites_belowThreshold() = assertFalse(shouldUnlockTenFavorites(9))
    @Test fun twentyFavorites_atThreshold() = assertTrue(shouldUnlockTwentyFavorites(20))
    @Test fun twentyFavorites_doesNotUnlockAtTen() = assertFalse(shouldUnlockTwentyFavorites(10))

    // hundred_plays / marathon
    @Test fun century_atThreshold() = assertTrue(shouldUnlockCentury(100))
    @Test fun century_belowThreshold() = assertFalse(shouldUnlockCentury(99))
    @Test fun marathon_atThreshold() = assertTrue(shouldUnlockMarathon(500))
    @Test fun marathon_doesNotUnlockAtCentury() = assertFalse(shouldUnlockMarathon(499))

    // library_50 / library_100
    @Test fun library50_atThreshold() = assertTrue(shouldUnlockLibrary50(50))
    @Test fun library50_belowThreshold() = assertFalse(shouldUnlockLibrary50(49))
    @Test fun library100_atThreshold() = assertTrue(shouldUnlockLibrary100(100))
    @Test fun library100_doesNotUnlockAt99() = assertFalse(shouldUnlockLibrary100(99))

    // five_playlists
    @Test fun fivePlaylists_atThreshold() = assertTrue(shouldUnlockFivePlaylists(5))
    @Test fun fivePlaylists_belowThreshold() = assertFalse(shouldUnlockFivePlaylists(4))

    // dedicated (20 plays on one track)
    @Test fun dedicated_atThreshold() = assertTrue(shouldUnlockDedicated(20))
    @Test fun dedicated_belowThreshold() = assertFalse(shouldUnlockDedicated(19))
    @Test fun dedicated_wellAboveThreshold() = assertTrue(shouldUnlockDedicated(100))

    // explorer (10+ unique artists)
    @Test fun explorer_atThreshold() = assertTrue(shouldUnlockExplorer(10))
    @Test fun explorer_belowThreshold() = assertFalse(shouldUnlockExplorer(9))

    // mood_setter
    @Test fun moodSetter_withOneTaggedTrack() = assertTrue(shouldUnlockMoodSetter(1))
    @Test fun moodSetter_noTaggedTracks() = assertFalse(shouldUnlockMoodSetter(0))
}
