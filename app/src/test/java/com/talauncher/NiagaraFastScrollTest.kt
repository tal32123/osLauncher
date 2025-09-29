package com.talauncher

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.talauncher.ui.appdrawer.AlphabetIndexEntry
import com.talauncher.ui.components.NiagaraFastScroll
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class NiagaraFastScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `NiagaraFastScroll renders correctly with entries`() {
        val entries = listOf(
            AlphabetIndexEntry("A", "A", 0, true, "Apple"),
            AlphabetIndexEntry("B", "B", 1, true, "Banana"),
            AlphabetIndexEntry("C", "C", null, false, null)
        )

        var entryFocusedCalled = false
        var scrollingChangedCalled = false

        composeTestRule.setContent {
            NiagaraFastScroll(
                entries = entries,
                isEnabled = true,
                onEntryFocused = { _, _ -> entryFocusedCalled = true },
                onScrollingChanged = { scrollingChangedCalled = true }
            )
        }

        // Verify that entries are rendered
        composeTestRule.onNodeWithTag("niagara_fast_scroll_entry_A").assertExists()
        composeTestRule.onNodeWithTag("niagara_fast_scroll_entry_B").assertExists()
        composeTestRule.onNodeWithTag("niagara_fast_scroll_entry_C").assertExists()
    }

    @Test
    fun `NiagaraFastScroll disabled state`() {
        val entries = listOf(
            AlphabetIndexEntry("A", "A", 0, true, "Apple")
        )

        composeTestRule.setContent {
            NiagaraFastScroll(
                entries = entries,
                isEnabled = false,
                onEntryFocused = { _, _ -> },
                onScrollingChanged = { }
            )
        }

        // Should still render the entry but in disabled state
        composeTestRule.onNodeWithTag("niagara_fast_scroll_entry_A").assertExists()
    }

    @Test
    fun `NiagaraFastScroll empty entries`() {
        composeTestRule.setContent {
            NiagaraFastScroll(
                entries = emptyList(),
                isEnabled = true,
                onEntryFocused = { _, _ -> },
                onScrollingChanged = { }
            )
        }

        // Should render without crashing
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `AlphabetIndexEntry data class properties`() {
        val entry = AlphabetIndexEntry(
            key = "A",
            displayLabel = "A",
            targetIndex = 5,
            hasApps = true,
            previewAppName = "AppName"
        )

        assertEquals("A", entry.key)
        assertEquals("A", entry.displayLabel)
        assertEquals(5, entry.targetIndex)
        assertTrue(entry.hasApps)
        assertEquals("AppName", entry.previewAppName)
    }

    @Test
    fun `AlphabetIndexEntry with no apps`() {
        val entry = AlphabetIndexEntry(
            key = "X",
            displayLabel = "X",
            targetIndex = null,
            hasApps = false,
            previewAppName = null
        )

        assertEquals("X", entry.key)
        assertEquals("X", entry.displayLabel)
        assertNull(entry.targetIndex)
        assertFalse(entry.hasApps)
        assertNull(entry.previewAppName)
    }
}