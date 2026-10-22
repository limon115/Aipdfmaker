package com.example.ui.components

import androidx.compose.ui.text.TextRange

/**
 * In-memory search index caching full document text and query results.
 * Restored as a standalone component for global access.
 */
class LatexSearchIndex(val sourceText: String) {
    private val queryCache = mutableMapOf<String, List<TextRange>>()

    val length: Int get() = sourceText.length

    fun search(query: String): List<TextRange> {
        if (query.isBlank() || sourceText.isEmpty()) return emptyList()
        val normalized = query.lowercase()
        val cached = queryCache[normalized]
        if (cached != null) return cached

        val matches = ArrayList<TextRange>()
        val qLen = query.length
        var idx = 0
        while (idx < sourceText.length) {
            val found = sourceText.indexOf(query, idx, ignoreCase = true)
            if (found == -1) break
            val end = (found + qLen).coerceAtMost(sourceText.length)
            matches.add(TextRange(found, end))
            idx = maxOf(idx + 1, end)
        }
        queryCache[normalized] = matches
        return matches
    }
}
