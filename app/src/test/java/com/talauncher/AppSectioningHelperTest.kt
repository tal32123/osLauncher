package com.talauncher

import com.talauncher.data.model.AppInfo
import com.talauncher.utils.AppSectioningHelper
import org.junit.Test
import org.junit.Assert.*
import java.text.Collator
import java.util.Locale

class AppSectioningHelperTest {

    @Test
    fun `generateSectionKey returns correct key for English apps`() {
        val locale = Locale.ENGLISH

        assertEquals("A", AppSectioningHelper.generateSectionKey("Apple", locale))
        assertEquals("B", AppSectioningHelper.generateSectionKey("Banana", locale))
        assertEquals("Z", AppSectioningHelper.generateSectionKey("Zebra", locale))
        assertEquals("#", AppSectioningHelper.generateSectionKey("123", locale))
        assertEquals("#", AppSectioningHelper.generateSectionKey("", locale))
        assertEquals("#", AppSectioningHelper.generateSectionKey("   ", locale))
    }

    @Test
    fun `generateSectionKey handles Hebrew apps correctly`() {
        val locale = Locale.ENGLISH // Using English locale to test Hebrew characters

        assertEquals("א", AppSectioningHelper.generateSectionKey("אפליקציה", locale))
        assertEquals("ב", AppSectioningHelper.generateSectionKey("בית", locale))
        assertEquals("ג", AppSectioningHelper.generateSectionKey("גוגל", locale))
    }

    @Test
    fun `createAppComparator sorts apps case-insensitively`() {
        val collator = Collator.getInstance(Locale.ENGLISH).apply {
            strength = Collator.PRIMARY
        }
        val comparator = AppSectioningHelper.createAppComparator(collator)

        val app1 = AppInfo("com.app1", "apple", false)
        val app2 = AppInfo("com.app2", "BANANA", false)
        val app3 = AppInfo("com.app3", "Cherry", false)
        val app4 = AppInfo("com.app4", "apple", false) // Same name, different package

        val apps = listOf(app2, app3, app1, app4)
        val sorted = apps.sortedWith(comparator)

        assertEquals("apple", sorted[0].appName)
        assertEquals("apple", sorted[1].appName)
        assertEquals("BANANA", sorted[2].appName)
        assertEquals("Cherry", sorted[3].appName)
    }

    @Test
    fun `groupAppsBySection creates correct sections`() {
        val locale = Locale.ENGLISH
        val apps = listOf(
            AppInfo("com.app1", "Apple", false),
            AppInfo("com.app2", "Banana", false),
            AppInfo("com.app3", "Android", false),
            AppInfo("com.app4", "Zebra", false),
            AppInfo("com.app5", "123App", false)
        )
        val recentApps = listOf(apps[0]) // Apple is recent

        val sections = AppSectioningHelper.groupAppsBySection(
            apps = apps,
            locale = locale,
            recentApps = recentApps,
            includeRecentSection = true
        )

        assertEquals(5, sections.size)

        // Check recent section
        assertEquals("recent", sections[0].key)
        assertEquals("Recently Used", sections[0].label)
        assertEquals(1, sections[0].apps.size)
        assertEquals("Apple", sections[0].apps[0].appName)
        assertFalse(sections[0].isIndexable)

        // Check # section (numbers)
        assertEquals("#", sections[1].key)
        assertEquals(1, sections[1].apps.size)
        assertEquals("123App", sections[1].apps[0].appName)
        assertTrue(sections[1].isIndexable)

        // Check alphabetical sections
        assertEquals("A", sections[2].key)
        assertEquals(2, sections[2].apps.size) // Apple, Android (123App goes to # section)
        assertTrue(sections[2].isIndexable)

        assertEquals("B", sections[3].key)
        assertEquals(1, sections[3].apps.size)
        assertEquals("Banana", sections[3].apps[0].appName)

        assertEquals("Z", sections[4].key)
        assertEquals(1, sections[4].apps.size)
        assertEquals("Zebra", sections[4].apps[0].appName)
    }

    @Test
    fun `createAlphabetIndexEntries creates correct entries`() {
        val sections = listOf(
            AppSectioningHelper.groupAppsBySection(
                apps = listOf(
                    AppInfo("com.app1", "Apple", false),
                    AppInfo("com.app2", "Zebra", false)
                ),
                locale = Locale.ENGLISH,
                recentApps = emptyList(),
                includeRecentSection = false
            )
        ).flatten()

        val entries = AppSectioningHelper.createAlphabetIndexEntries(sections)

        assertTrue(entries.size >= 26) // At least A-Z

        // Check that A and Z have apps
        val aEntry = entries.find { it.key == "A" }
        val zEntry = entries.find { it.key == "Z" }
        val bEntry = entries.find { it.key == "B" }

        assertNotNull(aEntry)
        assertTrue(aEntry!!.hasApps)
        assertEquals("Apple", aEntry.previewAppName)

        assertNotNull(zEntry)
        assertTrue(zEntry!!.hasApps)
        assertEquals("Zebra", zEntry.previewAppName)

        assertNotNull(bEntry)
        assertFalse(bEntry!!.hasApps)
        assertNull(bEntry.previewAppName)
    }
}