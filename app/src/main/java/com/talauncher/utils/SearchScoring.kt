package com.talauncher.utils

import kotlin.math.max
import kotlin.math.min

object SearchScoring {
    fun calculateRelevanceScore(query: String, name: String): Int {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) {
            return 0
        }
        val normalizedName = name.lowercase()

        return when {
            normalizedName == normalizedQuery -> 100 // Exact match
            normalizedName.startsWith(normalizedQuery) -> 80 // Starts with
            normalizedName.contains(" $normalizedQuery") -> 60 // Word boundary match
            normalizedName.contains(normalizedQuery) -> 40 // Contains
            else -> calculateFuzzyScore(normalizedQuery, normalizedName) // Fuzzy match
        }
    }

    private fun calculateFuzzyScore(query: String, target: String): Int {
        if (query.isEmpty() || target.isEmpty()) return 0

        val normalizedTarget = target.lowercase()
        val tokens = normalizedTarget.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        var bestSimilarity = 0.0

        fun updateBestSimilarity(candidate: String) {
            if (candidate.isEmpty()) return
            val distance = calculateLevenshteinDistance(query, candidate)
            val maxLength = max(query.length, candidate.length)
            val similarity = 1.0 - (distance.toDouble() / maxLength)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
            }
        }

        // Compare against tokens within the target (names, words, etc.)
        tokens.forEach(::updateBestSimilarity)

        // Compare against sliding windows to handle substrings inside longer tokens
        val minWindow = max(1, query.length - 1)
        val maxWindow = min(normalizedTarget.length, query.length + 2)
        for (windowSize in minWindow..maxWindow) {
            if (windowSize > normalizedTarget.length) break
            for (start in 0..normalizedTarget.length - windowSize) {
                val substring = normalizedTarget.substring(start, start + windowSize)
                updateBestSimilarity(substring)
            }
        }

        return if (bestSimilarity >= 0.6) {
            (bestSimilarity * 35).toInt()
        } else {
            0
        }
    }

    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
