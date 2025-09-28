package com.talauncher

import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppUsage
import com.talauncher.utils.ContactInfo
import com.talauncher.utils.EnhancedSearchService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EnhancedSearchServiceTest {

    private lateinit var searchService: EnhancedSearchService
    private lateinit var testApps: List<AppInfo>
    private lateinit var testContacts: List<ContactInfo>
    private lateinit var testUsageStats: Map<String, AppUsage>

    @Before
    fun setUp() {
        searchService = EnhancedSearchService()

        testApps = listOf(
            AppInfo(packageName = "com.google.android.gm", appName = "Gmail"),
            AppInfo(packageName = "com.whatsapp", appName = "WhatsApp"),
            AppInfo(packageName = "com.spotify.music", appName = "Spotify"),
            AppInfo(packageName = "com.google.android.calendar", appName = "Google Calendar"),
            AppInfo(packageName = "com.github.android", appName = "GitHub"),
            AppInfo(packageName = "com.calculator", appName = "Calculator")
        )

        testContacts = listOf(
            ContactInfo(id = "1", name = "Gilbert Johnson", phoneNumber = "+1234567890"),
            ContactInfo(id = "2", name = "Gary Smith", phoneNumber = "+1234567891"),
            ContactInfo(id = "3", name = "Alice Graham", phoneNumber = "+1234567892"),
            ContactInfo(id = "4", name = "George Wilson", phoneNumber = "+1234567893")
        )

        // Mock usage stats with different recency patterns
        val now = System.currentTimeMillis()
        testUsageStats = mapOf(
            "com.google.android.gm" to AppUsage(
                packageName = "com.google.android.gm",
                timeInForeground = 3600000L, // 1 hour
                lastTimeUsed = now - (2 * 60 * 60 * 1000L) // 2 hours ago
            ),
            "com.whatsapp" to AppUsage(
                packageName = "com.whatsapp",
                timeInForeground = 1800000L, // 30 minutes
                lastTimeUsed = now - (30 * 60 * 1000L) // 30 minutes ago (more recent)
            ),
            "com.spotify.music" to AppUsage(
                packageName = "com.spotify.music",
                timeInForeground = 7200000L, // 2 hours
                lastTimeUsed = now - (24 * 60 * 60 * 1000L) // 24 hours ago
            )
        )
    }

    @Test
    fun testExactMatch_shouldReturnHighestScore() = runTest {
        val results = searchService.searchUnified(
            query = "Gmail",
            apps = testApps,
            contacts = emptyList(),
            usageStats = emptyMap()
        )

        assertTrue("Should find Gmail", results.isNotEmpty())
        val gmailResult = results.first { it.name == "Gmail" }
        assertTrue("Gmail should have high relevance score", gmailResult.relevanceScore >= 90)
    }

    @Test
    fun testStartsWithMatch_shouldScoreHighly() = runTest {
        val results = searchService.searchUnified(
            query = "G",
            apps = testApps,
            contacts = testContacts,
            usageStats = emptyMap()
        )

        val gResults = results.filter { it.name.startsWith("G", ignoreCase = true) }
        assertTrue("Should find items starting with G", gResults.isNotEmpty())

        // Gmail, Google Calendar, GitHub, Gilbert, Gary, George should all be found
        val expectedNames = setOf("Gmail", "Google Calendar", "GitHub", "Gilbert Johnson", "Gary Smith", "George Wilson")
        val foundNames = gResults.map { it.name }.toSet()
        assertTrue("Should find all G entries", expectedNames.intersect(foundNames).size >= 4)
    }

    @Test
    fun testFuzzyMatching_shouldFindSimilarNames() = runTest {
        val results = searchService.searchUnified(
            query = "Gmai", // Slightly misspelled Gmail
            apps = testApps,
            contacts = emptyList(),
            usageStats = emptyMap()
        )

        assertTrue("Should find Gmail with fuzzy search",
            results.any { it.name == "Gmail" })
    }

    @Test
    fun testRecencyScoring_shouldBoostRecentlyUsedApps() = runTest {
        val results = searchService.searchUnified(
            query = "G",
            apps = testApps,
            contacts = emptyList(),
            usageStats = testUsageStats
        )

        val gmailResult = results.firstOrNull { it.name == "Gmail" }
        assertNotNull("Should find Gmail", gmailResult)
        assertTrue("Gmail should have recency score > 0", gmailResult!!.recencyScore > 0)
    }

    @Test
    fun testMixedAppAndContactSearch() = runTest {
        val results = searchService.searchUnified(
            query = "G",
            apps = testApps,
            contacts = testContacts,
            usageStats = testUsageStats
        )

        val appResults = results.filterIsInstance<EnhancedSearchService.SearchableItem.App>()
        val contactResults = results.filterIsInstance<EnhancedSearchService.SearchableItem.Contact>()

        assertTrue("Should find apps", appResults.isNotEmpty())
        assertTrue("Should find contacts", contactResults.isNotEmpty())

        // Results should be sorted by final score
        for (i in 0 until results.size - 1) {
            assertTrue("Results should be sorted by score",
                results[i].finalScore >= results[i + 1].finalScore)
        }
    }

    @Test
    fun testEmptyQuery_shouldReturnEmptyResults() = runTest {
        val results = searchService.searchUnified(
            query = "",
            apps = testApps,
            contacts = testContacts,
            usageStats = testUsageStats
        )

        assertTrue("Empty query should return empty results", results.isEmpty())
    }

    @Test
    fun testBlankQuery_shouldReturnEmptyResults() = runTest {
        val results = searchService.searchUnified(
            query = "   ",
            apps = testApps,
            contacts = testContacts,
            usageStats = testUsageStats
        )

        assertTrue("Blank query should return empty results", results.isEmpty())
    }

    @Test
    fun testNoMatches_shouldReturnEmptyResults() = runTest {
        val results = searchService.searchUnified(
            query = "XYZ123NonExistent",
            apps = testApps,
            contacts = testContacts,
            usageStats = testUsageStats
        )

        assertTrue("No matches should return empty results", results.isEmpty())
    }

    @Test
    fun testCaseInsensitiveSearch() = runTest {
        val upperCaseResults = searchService.searchUnified(
            query = "GMAIL",
            apps = testApps,
            contacts = emptyList(),
            usageStats = emptyMap()
        )

        val lowerCaseResults = searchService.searchUnified(
            query = "gmail",
            apps = testApps,
            contacts = emptyList(),
            usageStats = emptyMap()
        )

        assertEquals("Case should not matter", upperCaseResults.size, lowerCaseResults.size)
        assertTrue("Should find Gmail with uppercase query",
            upperCaseResults.any { it.name == "Gmail" })
    }

    @Test
    fun testRecencyVsFuzzyMatching_scenario() = runTest {
        // Test the scenario from Issue #72: "g" matching both Gilbert and Gmail
        val results = searchService.searchUnified(
            query = "g",
            apps = testApps,
            contacts = testContacts,
            usageStats = testUsageStats
        )

        val gilbert = results.firstOrNull { it.name == "Gilbert Johnson" }
        val gmail = results.firstOrNull { it.name == "Gmail" }

        assertNotNull("Should find Gilbert", gilbert)
        assertNotNull("Should find Gmail", gmail)

        // Gmail should rank higher due to recent usage (it has usage stats)
        if (gilbert != null && gmail != null) {
            assertTrue("Gmail should rank higher due to recent usage",
                gmail.finalScore >= gilbert.finalScore)
        }
    }

    @Test
    fun testWordBoundaryMatching() = runTest {
        val results = searchService.searchUnified(
            query = "Calendar",
            apps = testApps,
            contacts = emptyList(),
            usageStats = emptyMap()
        )

        val googleCalendar = results.firstOrNull { it.name == "Google Calendar" }
        assertNotNull("Should find Google Calendar", googleCalendar)
        assertTrue("Should have high relevance for word boundary match",
            googleCalendar!!.relevanceScore >= 60)
    }

    @Test
    fun testPartialWordMatching() = runTest {
        val results = searchService.searchUnified(
            query = "Cal",
            apps = testApps,
            contacts = emptyList(),
            usageStats = emptyMap()
        )

        val calculator = results.firstOrNull { it.name == "Calculator" }
        assertNotNull("Should find Calculator", calculator)

        // Note: "Google Calendar" should also be found but with different score
        val googleCalendar = results.firstOrNull { it.name == "Google Calendar" }
        assertNotNull("Should find Google Calendar", googleCalendar)
    }
}