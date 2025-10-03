package com.talauncher.domain.fastscroll

/**
 * Abstraction for haptic feedback operations.
 *
 * Architecture:
 * - Interface Segregation Principle: Focused interface for haptic operations only
 * - Dependency Inversion: High-level code depends on this abstraction
 * - Makes the domain layer testable and platform-independent
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles haptic feedback
 * - Interface Segregation: Minimal, focused interface
 * - Dependency Inversion: Domain layer depends on abstraction, not Android APIs
 *
 * @see HapticsImpl for Android implementation
 */
interface Haptics {
    /**
     * Performs a subtle haptic pulse for letter boundary crossing.
     * Should be brief and non-intrusive.
     */
    fun performLetterChange()

    /**
     * Performs a very light haptic pulse for app-to-app movement within a section.
     * Should be lighter than letter change and rate-limited to avoid spam.
     * Implementation should enforce rate limiting (e.g., max 1 per 50ms).
     */
    fun performAppChange()
}

/**
 * Abstraction for accessibility announcements (TalkBack support).
 *
 * Architecture:
 * - Interface Segregation Principle: Focused on accessibility only
 * - Dependency Inversion: Domain layer doesn't depend on Android accessibility APIs
 * - Enables testing without Android framework
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles accessibility announcements
 * - Interface Segregation: Minimal interface for announcement operations
 * - Dependency Inversion: High-level code depends on abstraction
 *
 * @see A11yAnnouncerImpl for Android implementation
 */
interface A11yAnnouncer {
    /**
     * Announces the current section letter and app name.
     * Implementation should debounce announcements to avoid spam
     * (e.g., max 1 announcement per 300ms).
     *
     * @param letter Current section letter (e.g., "A", "B", "#")
     * @param appName Current app name (e.g., "Chrome", "Gmail")
     */
    fun announce(letter: String, appName: String)

    /**
     * Announces only the section letter (for section boundary crossing).
     * Implementation should debounce announcements.
     *
     * @param letter Current section letter
     */
    fun announceLetter(letter: String)
}

/**
 * Abstraction for locale-aware collation and bucketing.
 *
 * Architecture:
 * - Interface Segregation Principle: Focused on collation/sorting only
 * - Dependency Inversion: Domain logic doesn't depend on Java Collator directly
 * - Enables locale-specific sorting without coupling to implementation
 *
 * SOLID Principles:
 * - Single Responsibility: Only handles locale-aware sorting and bucketing
 * - Open/Closed: Can be extended for different locales without modification
 * - Dependency Inversion: High-level sorting logic depends on abstraction
 *
 * @see CollationProviderImpl for default implementation
 */
interface CollationProvider {
    /**
     * Sorts a list of app names using locale-aware collation.
     * Ties should be broken by original order (stable sort).
     *
     * @param items List of items to sort
     * @param getName Function to extract the name from each item
     * @return Sorted list
     */
    fun <T> sortByName(items: List<T>, getName: (T) -> String): List<T>

    /**
     * Determines the bucket key for a given app name.
     * Returns "A"-"Z" for letters, "#" for non-alphabetic characters.
     *
     * @param appName The app name to bucket
     * @return Bucket key (e.g., "A", "B", "#")
     */
    fun getBucketKey(appName: String): String

    /**
     * Gets the display label for a bucket key.
     * Usually the same as the key, but can be locale-specific.
     *
     * @param bucketKey The bucket key
     * @return Display label
     */
    fun getDisplayLabel(bucketKey: String): String
}
