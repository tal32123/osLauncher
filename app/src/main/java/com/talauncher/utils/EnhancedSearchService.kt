package com.talauncher.utils

import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Enhanced search service that provides improved fuzzy matching and recency-based ranking
 * for both apps and contacts.
 *
 * Key improvements over basic search:
 * - Better fuzzy matching algorithms (Jaro-Winkler + enhanced Levenshtein)
 * - Advanced recency scoring with multiple time decay curves
 * - Unified scoring system for apps and contacts
 * - Efficient async processing
 * - Contact interaction tracking support
 */
class EnhancedSearchService {

    companion object {
        // Scoring weights
        private const val EXACT_MATCH_SCORE = 100
        private const val STARTS_WITH_SCORE = 85
        private const val WORD_BOUNDARY_SCORE = 70
        private const val CONTAINS_SCORE = 50
        private const val MAX_FUZZY_SCORE = 45
        private const val MAX_RECENCY_BOOST = 25
        private const val MAX_FREQUENCY_BOOST = 15

        // Fuzzy matching thresholds
        private const val MIN_FUZZY_SIMILARITY = 0.65
        private const val JARO_WINKLER_PREFIX_SCALE = 0.1

        // Recency decay parameters (in hours)
        private const val SHORT_TERM_DECAY = 6.0    // Fast decay for very recent usage
        private const val MEDIUM_TERM_DECAY = 24.0  // Medium decay for daily usage
        private const val LONG_TERM_DECAY = 168.0   // Slow decay for weekly patterns

        // Contact interaction scoring (placeholder for future implementation)
        private const val CONTACT_CALL_WEIGHT = 1.5
        private const val CONTACT_MESSAGE_WEIGHT = 1.0
        private const val CONTACT_WHATSAPP_WEIGHT = 1.2
    }

    /**
     * Represents a searchable item with unified scoring
     */
    sealed class SearchableItem {
        abstract val id: String
        abstract val name: String
        abstract val relevanceScore: Int
        abstract val recencyScore: Int
        abstract val finalScore: Int

        data class App(
            val appInfo: AppInfo,
            override val relevanceScore: Int,
            override val recencyScore: Int
        ) : SearchableItem() {
            override val id = appInfo.packageName
            override val name = appInfo.appName
            override val finalScore = relevanceScore + recencyScore
        }

        data class Contact(
            val contactInfo: ContactInfo,
            override val relevanceScore: Int,
            override val recencyScore: Int
        ) : SearchableItem() {
            override val id = contactInfo.id
            override val name = contactInfo.name
            override val finalScore = relevanceScore + recencyScore
        }
    }

    /**
     * Enhanced search algorithm that combines fuzzy matching with recency scoring
     */
    suspend fun searchUnified(
        query: String,
        apps: List<AppInfo>,
        contacts: List<ContactInfo>,
        usageStats: Map<String, AppUsage> = emptyMap(),
        contactInteractions: Map<String, ContactInteractionStats> = emptyMap()
    ): List<SearchableItem> = withContext(Dispatchers.Default) {

        if (query.isBlank()) return@withContext emptyList()

        val normalizedQuery = query.trim().lowercase()
        val results = mutableListOf<SearchableItem>()

        // Search apps
        apps.forEach { app ->
            val relevanceScore = calculateEnhancedRelevanceScore(normalizedQuery, app.appName)
            if (relevanceScore > 0) {
                val recencyScore = calculateAppRecencyScore(app.packageName, usageStats)
                results.add(SearchableItem.App(app, relevanceScore, recencyScore))
            }
        }

        // Search contacts
        contacts.forEach { contact ->
            val relevanceScore = calculateEnhancedRelevanceScore(normalizedQuery, contact.name)
            if (relevanceScore > 0) {
                val recencyScore = calculateContactRecencyScore(contact.id, contactInteractions)
                results.add(SearchableItem.Contact(contact, relevanceScore, recencyScore))
            }
        }

        // Sort by final score (relevance + recency), then by name for stable sorting
        results.sortedWith(
            compareByDescending<SearchableItem> { it.finalScore }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Enhanced relevance scoring using multiple fuzzy matching algorithms
     */
    private fun calculateEnhancedRelevanceScore(query: String, name: String): Int {
        val normalizedName = name.lowercase()

        // Exact and partial matching (fast path)
        when {
            normalizedName == query -> return EXACT_MATCH_SCORE
            normalizedName.startsWith(query) -> return STARTS_WITH_SCORE
            normalizedName.contains(" $query") -> return WORD_BOUNDARY_SCORE
            normalizedName.contains(query) -> return CONTAINS_SCORE
        }

        // Advanced fuzzy matching
        return calculateAdvancedFuzzyScore(query, normalizedName)
    }

    /**
     * Advanced fuzzy matching combining Jaro-Winkler and enhanced Levenshtein
     */
    private fun calculateAdvancedFuzzyScore(query: String, target: String): Int {
        if (query.isEmpty() || target.isEmpty()) return 0

        // Use Jaro-Winkler for better prefix matching
        val jaroWinklerSimilarity = calculateJaroWinkler(query, target)

        // Use enhanced Levenshtein with character position weighting
        val levenshteinSimilarity = calculateEnhancedLevenshtein(query, target)

        // Combine both algorithms with slight preference for Jaro-Winkler
        val combinedSimilarity = (jaroWinklerSimilarity * 0.6) + (levenshteinSimilarity * 0.4)

        return if (combinedSimilarity >= MIN_FUZZY_SIMILARITY) {
            (combinedSimilarity * MAX_FUZZY_SCORE).toInt()
        } else {
            0
        }
    }

    /**
     * Jaro-Winkler similarity implementation optimized for app/contact names
     */
    private fun calculateJaroWinkler(s1: String, s2: String): Double {
        val jaro = calculateJaro(s1, s2)
        if (jaro < 0.7) return jaro

        // Calculate common prefix length (up to 4 characters)
        val prefixLength = min(4, s1.zip(s2).takeWhile { it.first == it.second }.size)

        return jaro + (prefixLength * JARO_WINKLER_PREFIX_SCALE * (1 - jaro))
    }

    private fun calculateJaro(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val matchWindow = max(s1.length, s2.length) / 2 - 1
        if (matchWindow < 1) return 0.0

        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)

        var matches = 0

        // Find matches
        for (i in s1.indices) {
            val start = max(0, i - matchWindow)
            val end = min(i + matchWindow + 1, s2.length)

            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        // Calculate transpositions
        var transpositions = 0
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        return (matches.toDouble() / s1.length +
                matches.toDouble() / s2.length +
                (matches - transpositions / 2.0) / matches) / 3.0
    }

    /**
     * Enhanced Levenshtein with position weighting (early characters matter more)
     */
    private fun calculateEnhancedLevenshtein(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val dp = Array(s1.length + 1) { DoubleArray(s2.length + 1) }

        // Initialize with position weights
        for (i in 0..s1.length) dp[i][0] = i.toDouble()
        for (j in 0..s2.length) dp[0][j] = j.toDouble()

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0.0 else {
                    // Position weighting: early positions have higher cost
                    val positionWeight = 1.0 + (1.0 / (min(i, j) + 1))
                    positionWeight
                }

                dp[i][j] = min(
                    dp[i - 1][j] + 1.0,
                    min(dp[i][j - 1] + 1.0, dp[i - 1][j - 1] + cost)
                )
            }
        }

        val maxLength = max(s1.length, s2.length)
        return 1.0 - (dp[s1.length][s2.length] / maxLength)
    }

    /**
     * Advanced app recency scoring with multiple decay curves
     */
    private fun calculateAppRecencyScore(
        packageName: String,
        usageStats: Map<String, AppUsage>
    ): Int {
        val usage = usageStats[packageName] ?: return 0

        val now = System.currentTimeMillis()
        val lastUsedHours = TimeUnit.MILLISECONDS.toHours(now - usage.lastTimeUsed).toDouble()
        val usageTimeHours = TimeUnit.MILLISECONDS.toHours(usage.timeInForeground).toDouble()

        // Multi-curve recency decay
        val shortTermBoost = exp(-lastUsedHours / SHORT_TERM_DECAY) * 0.4
        val mediumTermBoost = exp(-lastUsedHours / MEDIUM_TERM_DECAY) * 0.4
        val longTermBoost = exp(-lastUsedHours / LONG_TERM_DECAY) * 0.2

        val recencyFactor = shortTermBoost + mediumTermBoost + longTermBoost

        // Frequency factor with diminishing returns
        val frequencyFactor = min(1.0, usageTimeHours / 10.0) // Cap at 10 hours

        val recencyScore = (recencyFactor * MAX_RECENCY_BOOST).toInt()
        val frequencyScore = (frequencyFactor * MAX_FREQUENCY_BOOST).toInt()

        return recencyScore + frequencyScore
    }

    /**
     * Contact recency scoring based on interaction history
     * Note: ContactInteractionStats is a placeholder for future implementation
     */
    private fun calculateContactRecencyScore(
        contactId: String,
        interactions: Map<String, ContactInteractionStats>
    ): Int {
        val interaction = interactions[contactId] ?: return 0

        val now = System.currentTimeMillis()
        val hoursSinceLastInteraction = TimeUnit.MILLISECONDS.toHours(
            now - interaction.lastInteractionTime
        ).toDouble()

        // Contact-specific decay (slower than apps due to less frequent interaction)
        val recencyFactor = exp(-hoursSinceLastInteraction / (MEDIUM_TERM_DECAY * 2))

        // Weight by interaction type
        val interactionWeight = when (interaction.lastInteractionType) {
            ContactInteractionType.CALL -> CONTACT_CALL_WEIGHT
            ContactInteractionType.MESSAGE -> CONTACT_MESSAGE_WEIGHT
            ContactInteractionType.WHATSAPP -> CONTACT_WHATSAPP_WEIGHT
            else -> 1.0
        }

        return (recencyFactor * interactionWeight * MAX_RECENCY_BOOST * 0.8).toInt()
    }
}

/**
 * Represents contact interaction statistics for recency scoring
 * This is a placeholder for future contact interaction tracking
 */
data class ContactInteractionStats(
    val lastInteractionTime: Long,
    val lastInteractionType: ContactInteractionType,
    val totalInteractions: Int,
    val callCount: Int = 0,
    val messageCount: Int = 0,
    val whatsAppCount: Int = 0
)

enum class ContactInteractionType {
    CALL, MESSAGE, WHATSAPP, OPEN
}