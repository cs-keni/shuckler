package com.shuckler.app.recommendation

import android.content.Context
import com.shuckler.app.download.DownloadedTrack
import com.shuckler.app.ui.SearchPreferences
import com.shuckler.app.youtube.YouTubeRepository
import com.shuckler.app.youtube.YouTubeSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Infers user preferences from library content (what they listen to, favorites)
 * and search behavior, then produces ranked search queries for recommendations.
 *
 * A proper recommendation system judges what the user likes based on:
 * - What they keep (library)
 * - What they repeatedly play (play counts)
 * - What they mark as favorite
 * - What they search for (especially frequently)
 *
 * Output: search queries to run on YouTube to surface "Recommended for you" content.
 */
object RecommendationEngine {

    private const val MAX_QUERIES = 5
    private const val MIN_ARTIST_PLAYS = 1
    private const val MIN_ARTIST_FAVORITE_WEIGHT = 2

    data class RecommendationQuery(
        val query: String,
        val reason: String,
        val score: Float
    )

    /**
     * Produces ranked search queries for "Recommended for you" content.
     * Combines signals from library and search history.
     */
    fun getRecommendationQueries(
        context: Context,
        completedTracks: List<DownloadedTrack>
    ): List<RecommendationQuery> {
        val candidates = mutableMapOf<String, Pair<String, Float>>() // query -> (reason, score)

        // 1. Artists from favorites (strongest signal)
        completedTracks
            .filter { it.isFavorite && it.artist.isNotBlank() }
            .forEach { track ->
                val artist = track.artist.trim()
                if (artist.length >= 2) {
                    val score = MIN_ARTIST_FAVORITE_WEIGHT + track.playCount * 0.5f
                    val existing = candidates[artist]
                    if (existing == null || existing.second < score) {
                        candidates[artist] = "From your favorites" to score
                    }
                }
            }

        // 2. Artists from high-play-count tracks (they keep replaying = they like it)
        completedTracks
            .filter { it.playCount >= MIN_ARTIST_PLAYS && it.artist.isNotBlank() }
            .sortedByDescending { it.playCount }
            .take(15)
            .forEach { track ->
                val artist = track.artist.trim()
                if (artist.length >= 2) {
                    val score = (if (track.isFavorite) 1.5f else 1f) * min(track.playCount.toFloat(), 10f)
                    val existing = candidates[artist]
                    if (existing == null || existing.second < score) {
                        val reason = if (track.isFavorite) "From your favorites" else "Based on your listening"
                        candidates[artist] = reason to score
                    }
                }
            }

        // 3. Frequent searches (searched 3+ times) - user is clearly interested
        SearchPreferences.getFrequentSearches(context, minCount = 3).forEach { query ->
            if (query.isNotBlank() && query.length >= 2) {
                val score = 3f // Strong signal: repeated intent
                val existing = candidates[query]
                if (existing == null || existing.second < score) {
                    candidates[query] = "You search this often" to score
                }
            }
        }

        // 4. Recent searches (weaker signal, but shows current interest)
        SearchPreferences.getRecentSearches(context)
            .take(5)
            .forEachIndexed { index, query ->
                if (query.isNotBlank() && query.length >= 2 && !candidates.containsKey(query)) {
                    val score = 1.5f - index * 0.2f // More recent = slightly higher
                    candidates[query] = "Recent search" to score
                }
            }

        return candidates.entries
            .sortedByDescending { it.value.second }
            .take(MAX_QUERIES)
            .map { (query, pair) ->
                RecommendationQuery(
                    query = query,
                    reason = pair.first,
                    score = pair.second
                )
            }
    }

    /**
     * Whether we have enough data to show meaningful recommendations.
     */
    fun hasRecommendationData(
        context: Context,
        completedTracks: List<DownloadedTrack>
    ): Boolean {
        val hasLibrarySignals = completedTracks.any { it.isFavorite || it.playCount > 0 }
        val hasSearchSignals = SearchPreferences.getRecentSearches(context).isNotEmpty() ||
            SearchPreferences.getFrequentSearches(context, minCount = 2).isNotEmpty()
        return hasLibrarySignals || hasSearchSignals
    }

    /**
     * Fetch YouTube search results for recommendation queries.
     * Runs searches in parallel, merges results, and excludes already-downloaded videos.
     */
    suspend fun fetchRecommendedYouTubeResults(
        context: Context,
        completedTracks: List<DownloadedTrack>,
        maxResults: Int = 12
    ): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        val queries = getRecommendationQueries(context, completedTracks)
        if (queries.isEmpty()) return@withContext emptyList()

        val downloadedUrls = completedTracks
            .mapNotNull { it.sourceUrl.takeIf { u -> u.isNotBlank() } }
            .map { normalizeVideoId(it) }
            .toSet()

        val resultsPerQuery = (maxResults / queries.size).coerceAtLeast(2).coerceAtMost(8)
        val allResults = kotlinx.coroutines.coroutineScope {
            queries.map { rq ->
                async {
                    YouTubeRepository.search(rq.query)
                        .take(resultsPerQuery)
                        .filter { normalizeVideoId(it.url) !in downloadedUrls }
                }
            }.awaitAll()
        }.flatten()

        // Dedupe by video ID, preserve order
        val seen = mutableSetOf<String>()
        allResults.filter { seen.add(normalizeVideoId(it.url)) }.take(maxResults)
    }

    private fun normalizeVideoId(url: String): String {
        val trimmed = url.trim()
        val shortMatch = Regex("youtu\\.be/([a-zA-Z0-9_-]{11})").find(trimmed)
        if (shortMatch != null) return shortMatch.groupValues[1]
        val idMatch = Regex("(?:watch\\?v=|/embed/|/v/)([a-zA-Z0-9_-]{11})").find(trimmed)
        if (idMatch != null) return idMatch.groupValues[1]
        return trimmed
    }
}
