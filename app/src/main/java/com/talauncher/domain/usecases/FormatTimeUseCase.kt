package com.talauncher.domain.usecases

import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Use case for formatting time and date using modern java.time API.
 *
 * Architecture:
 * - Follows Single Responsibility Principle (only handles time/date formatting)
 * - Encapsulates formatting logic for reusability and testability
 * - Thread-safe (DateTimeFormatter is immutable and thread-safe)
 * - Stateless operation (functional approach)
 *
 * Design Pattern: Use Case pattern (Clean Architecture)
 * SOLID Principles:
 * - Single Responsibility: Only formats time and date
 * - Open/Closed: Can be extended with different formats without modification
 * - Dependency Inversion: Depends on Clock abstraction for testability
 *
 * Thread-Safety:
 * - DateTimeFormatter is thread-safe (unlike SimpleDateFormat)
 * - Clock is immutable and thread-safe
 * - No mutable state shared between invocations
 *
 * @param clock Clock instance for obtaining current time (default: system default zone)
 *              Injecting Clock enables testing with fixed or mock times
 * @param timeFormatter Formatter for time (default: 24-hour format HH:mm)
 * @param dateFormatter Formatter for date (default: full date with day name)
 */
class FormatTimeUseCase(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()),
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
) {
    /**
     * Formats the current time and date using the system clock.
     *
     * Performance:
     * - LocalDateTime.now() is more efficient than Date()
     * - DateTimeFormatter is thread-safe, eliminating synchronization overhead
     * - No object pooling or caching needed
     *
     * @return TimeFormatResult containing formatted time and date strings
     */
    fun execute(): TimeFormatResult {
        return try {
            val now = LocalDateTime.now(clock)
            TimeFormatResult(
                time = timeFormatter.format(now),
                date = dateFormatter.format(now)
            )
        } catch (e: Exception) {
            // Fallback on error (e.g., formatter pattern issues)
            TimeFormatResult(
                time = "--:--",
                date = ""
            )
        }
    }

    /**
     * Result data class for time formatting operation.
     *
     * @property time Formatted time string (e.g., "14:30")
     * @property date Formatted date string (e.g., "Friday, October 2")
     */
    data class TimeFormatResult(
        val time: String,
        val date: String
    )
}
