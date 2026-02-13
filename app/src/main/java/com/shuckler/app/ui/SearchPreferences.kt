package com.shuckler.app.ui

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Persists recent search queries and their counts.
 * Used for recommendations: show queries searched 3+ times.
 */
object SearchPreferences {
    private const val PREFS_NAME = "search_prefs"
    private const val KEY_SEARCH_COUNTS = "search_counts"
    private const val KEY_RECENT_QUERIES = "recent_queries"
    private const val MAX_RECENT = 20

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordSearch(context: Context, query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val p = prefs(context)
        val counts = loadCounts(p)
        val count = (counts[q] ?: 0) + 1
        counts[q] = count
        saveCounts(p, counts)
        val recent = loadRecent(p).filter { it != q } + q
        p.edit().putString(KEY_RECENT_QUERIES, recent.takeLast(MAX_RECENT).joinToString("\n")).apply()
    }

    fun getFrequentSearches(context: Context, minCount: Int = 3): List<String> {
        val counts = loadCounts(prefs(context))
        return counts.entries
            .filter { it.value >= minCount }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(10)
    }

    fun getRecentSearches(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_RECENT_QUERIES, "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }.takeLast(10).reversed()
    }

    private fun loadCounts(p: SharedPreferences): MutableMap<String, Int> {
        val json = p.getString(KEY_SEARCH_COUNTS, "{}") ?: "{}"
        return try {
            JSONObject(json).let { obj ->
                obj.keys().asSequence().associateWith { obj.getInt(it) }.toMutableMap()
            }
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun saveCounts(p: SharedPreferences, counts: Map<String, Int>) {
        val json = JSONObject(counts)
        p.edit().putString(KEY_SEARCH_COUNTS, json.toString()).apply()
    }

    private fun loadRecent(p: SharedPreferences): List<String> {
        val raw = p.getString(KEY_RECENT_QUERIES, "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }
    }
}
