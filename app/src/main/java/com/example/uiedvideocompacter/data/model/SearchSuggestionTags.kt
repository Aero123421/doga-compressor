package com.example.uiedvideocompacter.data.model

import android.util.LruCache
import java.util.Locale

data class SearchTag(
    val text: String,
    val score: Int = 99,
    val isCode: Boolean = false
)

object SearchSuggestionTags {
    private val normalizationCache = LruCache<String, String>(500)

    // Regex constants
    private val JP_TOKEN = Regex("[\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\uFF66-\\uFF9D]+")
    private val LATIN_TOKEN = Regex("[A-Za-z][A-Za-z0-9]{1,15}")
    private val PRODUCT_CODE = Regex("([A-Za-z]+)-([0-9]+)")

    fun extractTags(videoName: String): Set<String> {
        val tags = mutableSetOf<String>()
        
        // 1. タイトル全体 (80文字以下)
        if (videoName.length <= 80) {
            tags.add(videoName)
        }

        // 2. 日本語トークン (2-16字)
        JP_TOKEN.findAll(videoName).forEach { 
            val s = it.value
            if (s.length in 2..16) tags.add(s)
        }

        // 3. ラテン文字トークン (2-16字)
        LATIN_TOKEN.findAll(videoName).forEach {
            val s = it.value
            if (s.length in 2..16) tags.add(s)
        }

        // 4. 製品コード解析 (ABC-123 -> ABC-123, ABC)
        PRODUCT_CODE.findAll(videoName).forEach {
            val full = it.value
            val key = it.groupValues[1]
            tags.add(full)
            tags.add(key)
        }

        return tags
    }

    fun generateSuggestions(
        query: String,
        allTags: Set<String>,
        maxResults: Int = 10
    ): List<String> {
        if (query.isEmpty()) return emptyList()

        val normalizedQuery = getNormalized(query)
        val suggestions = mutableListOf<SearchTag>()
        var count = 0

        for (tag in allTags) {
            val score = calculateScore(query, normalizedQuery, tag)
            if (score <= 4) {
                suggestions.add(SearchTag(tag, score, isProductCode(tag)))
                count++
                // 早期終了（完全一致系が十分見つかれば等、今回は簡易的に最大数で制御）
                if (count > 200) break 
            }
        }

        return suggestions
            .sortedWith(
                compareBy<SearchTag> { it.score }
                    .thenBy { it.text.length }
                    .thenBy { it.text.lowercase(Locale.ROOT) }
            )
            .map { it.text }
            .distinct()
            .take(maxResults)
    }

    private fun calculateScore(query: String, normalizedQuery: String, tag: String): Int {
        val normalizedTag = getNormalized(tag)
        val isCode = isProductCode(tag)

        // 0: 大文字小文字無視の前方一致
        if (tag.startsWith(query, ignoreCase = true)) return 0
        
        // 1: 正規化済みテキストの前方一致
        if (normalizedTag.startsWith(normalizedQuery)) return 1
        
        // 2: コードモード時の正規化済みコードの前方一致 (isCode かつ 前方一致)
        if (isCode && normalizedTag.startsWith(normalizedQuery)) return 2
        
        // 3: 正規化済みテキストの部分一致
        if (normalizedTag.contains(normalizedQuery)) return 3
        
        // 4: コードモード時の正規化済みコードの部分一致
        if (isCode && normalizedTag.contains(normalizedQuery)) return 4

        return 99
    }

    private fun getNormalized(text: String): String {
        val cached = normalizationCache.get(text)
        if (cached != null) return cached
        
        val normalized = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\uFF66-\\uFF9D]"), "")
        
        normalizationCache.put(text, normalized)
        return normalized
    }

    private fun isProductCode(text: String): Boolean {
        return PRODUCT_CODE.matches(text) || (text.length in 2..6 && text.all { it.isLetter() })
    }
}
