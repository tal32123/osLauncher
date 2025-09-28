package com.talauncher.utils

import com.talauncher.data.model.AppInfo
import com.talauncher.ui.appdrawer.AlphabetIndexEntry
import com.talauncher.ui.appdrawer.AppDrawerSection
import java.text.Collator
import java.util.Locale

object AppSectioningHelper {

    fun generateSectionKey(label: String, locale: Locale): String {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) {
            return "#"
        }
        val firstChar = trimmed.firstOrNull { it.isLetterOrDigit() } ?: return "#"
        if (!firstChar.isLetter()) {
            return "#"
        }
        val upper = firstChar.toString().uppercase(locale)
        return upper.take(1)
    }

    fun createAppComparator(collator: Collator): Comparator<AppInfo> = Comparator { left, right ->
        val labelComparison = collator.compare(left.appName.trim(), right.appName.trim())
        if (labelComparison != 0) {
            labelComparison
        } else {
            left.packageName.compareTo(right.packageName, ignoreCase = true)
        }
    }

    fun groupAppsBySection(
        apps: List<AppInfo>,
        locale: Locale,
        recentApps: List<AppInfo>,
        includeRecentSection: Boolean
    ): List<AppDrawerSection> {
        val sections = mutableListOf<AppDrawerSection>()

        if (includeRecentSection && recentApps.isNotEmpty()) {
            sections.add(
                AppDrawerSection(
                    key = "recent",
                    label = "Recently Used",
                    apps = recentApps,
                    isIndexable = false
                )
            )
        }

        if (apps.isNotEmpty()) {
            val grouped = linkedMapOf<String, MutableList<AppInfo>>()
            apps.forEach { app ->
                val sectionKey = generateSectionKey(app.appName, locale)
                val sectionApps = grouped.getOrPut(sectionKey) { mutableListOf() }
                sectionApps += app
            }

            // Sort sections: # first, then alphabetically
            val sortedKeys = grouped.keys.sortedWith { a, b ->
                when {
                    a == "#" && b != "#" -> -1
                    a != "#" && b == "#" -> 1
                    else -> a.compareTo(b)
                }
            }

            sortedKeys.forEach { key ->
                val appList = grouped[key]!!
                sections.add(
                    AppDrawerSection(
                        key = key,
                        label = key,
                        apps = appList,
                        isIndexable = true
                    )
                )
            }
        }

        return sections
    }

    fun createAlphabetIndexEntries(sections: List<AppDrawerSection>): List<AlphabetIndexEntry> {
        val sectionPositions = mutableMapOf<String, Pair<Int, String?>>()
        var currentIndex = 0

        sections.forEach { section ->
            if (section.isIndexable && section.apps.isNotEmpty()) {
                sectionPositions[section.key] = Pair(
                    currentIndex,
                    section.apps.first().appName
                )
            }
            currentIndex += 1 + section.apps.size
        }

        if (sectionPositions.isEmpty()) {
            return emptyList()
        }

        val baseAlphabet = ('A'..'Z').map { it.toString() }
        val sectionKeys = sections.filter { it.isIndexable }.map { it.key }
        val extraKeys = sectionKeys.filter { it !in baseAlphabet && it != "#" }
        val orderedKeys = (baseAlphabet + extraKeys + listOf("#")).distinct()

        return orderedKeys.map { key ->
            val position = sectionPositions[key]
            AlphabetIndexEntry(
                key = key,
                displayLabel = key,
                targetIndex = position?.first,
                hasApps = position != null,
                previewAppName = position?.second
            )
        }
    }
}