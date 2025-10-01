package com.talauncher.domain.usecases

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case for formatting time and date.
 *
 * Architecture:
 * - Follows Single Responsibility Principle (only handles time/date formatting)
 * - Encapsulates formatting logic for reusability and testability
 * - Stateless operation (functional approach)
 *
 * Design Pattern: Use Case pattern (Clean Architecture)
 * SOLID:
 * - Single Responsibility: Only formats time and date
 * - Open/Closed: Can be extended with different formats without modification
 *
 * @param timeFormat Format pattern for time (default: 24-hour format)
 * @param dateFormat Format pattern for date (default: full date with day name)
 */
class FormatTimeUseCase(
    private val timeFormat: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()),
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
) {
    /**
     * Formats the current time and date.
     *
     * @return Pair of (formatted time, formatted date)
     */
    fun execute(): TimeFormatResult {
        return try {
            val now = Date()
            TimeFormatResult(
                time = timeFormat.format(now),
                date = dateFormat.format(now)
            )
        } catch (e: Exception) {
            // Fallback on error
            TimeFormatResult(
                time = "--:--",
                date = ""
            )
        }
    }

    /**
     * Result data class for time formatting operation.
     *
     * @property time Formatted time string
     * @property date Formatted date string
     */
    data class TimeFormatResult(
        val time: String,
        val date: String
    )
}
