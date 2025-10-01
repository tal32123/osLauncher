package com.talauncher.domain.model

/**
 * Constant for the "Recent Apps" index key.
 */
const val RECENT_APPS_INDEX_KEY = "*"

/**
 * Represents a single entry in the alphabet index.
 *
 * Architecture:
 * - Domain model following Clean Architecture principles
 * - Immutable data class for thread safety
 * - Part of domain layer (independent of UI framework)
 *
 * @property key Unique identifier for this entry (e.g., "A", "B", "#", "*" for recent)
 * @property displayLabel Text to display in the index
 * @property targetIndex Index in the list to scroll to (null if no apps for this letter)
 * @property hasApps Whether this entry has any apps
 * @property previewAppName Optional app name for preview (future enhancement)
 */
data class AlphabetIndexEntry(
    val key: String,
    val displayLabel: String,
    val targetIndex: Int?,
    val hasApps: Boolean,
    val previewAppName: String? = null
)
