package com.talauncher.infrastructure.fastscroll

import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.talauncher.domain.fastscroll.A11yAnnouncer

/**
 * Android implementation of accessibility announcements for TalkBack support.
 *
 * Architecture:
 * - Implements A11yAnnouncer interface (Dependency Inversion)
 * - Handles Android-specific accessibility APIs
 * - Debounces announcements to prevent TalkBack spam
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles accessibility announcements
 * - Dependency Inversion: Implements abstract A11yAnnouncer interface
 * - Open/Closed: Can be extended for different announcement strategies
 *
 * Performance:
 * - Debounced to max 1 announcement per 300ms
 * - Uses efficient View.announceForAccessibility API
 * - No allocations in hot path
 *
 * Accessibility:
 * - Provides clear, contextual information for TalkBack users
 * - Announces both letter and app name for full context
 * - Respects Android accessibility best practices
 *
 * @param view The view to use for announcements (typically the fast scroll rail)
 */
class A11yAnnouncerImpl(private val view: View) : A11yAnnouncer {

    // Debouncing for announcements
    private var lastAnnouncementTime = 0L
    private val announcementDebounceMs = 300L

    /**
     * Announces the current section letter and app name.
     * Format: "Letter A, App Chrome"
     * Debounced to max 1 announcement per 300ms.
     *
     * @param letter Current section letter (e.g., "A", "B", "#")
     * @param appName Current app name (e.g., "Chrome", "Gmail")
     */
    override fun announce(letter: String, appName: String) {
        val now = System.currentTimeMillis()
        if (now - lastAnnouncementTime < announcementDebounceMs) {
            return // Debounce
        }
        lastAnnouncementTime = now

        val announcement = buildAnnouncement(letter, appName)
        view.announceForAccessibility(announcement)
    }

    /**
     * Announces only the section letter.
     * Format: "Letter A"
     * Debounced to max 1 announcement per 300ms.
     *
     * @param letter Current section letter
     */
    override fun announceLetter(letter: String) {
        val now = System.currentTimeMillis()
        if (now - lastAnnouncementTime < announcementDebounceMs) {
            return // Debounce
        }
        lastAnnouncementTime = now

        val announcement = buildLetterAnnouncement(letter)
        view.announceForAccessibility(announcement)
    }

    /**
     * Builds a complete announcement with letter and app name.
     */
    private fun buildAnnouncement(letter: String, appName: String): String {
        val letterLabel = if (letter == "#") "Symbol" else "Letter $letter"
        return "$letterLabel, $appName"
    }

    /**
     * Builds a letter-only announcement.
     */
    private fun buildLetterAnnouncement(letter: String): String {
        return if (letter == "#") "Symbol section" else "Letter $letter"
    }
}
