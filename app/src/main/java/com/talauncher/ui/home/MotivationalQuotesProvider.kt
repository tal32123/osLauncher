package com.talauncher.ui.home

import android.content.Context
import androidx.annotation.ArrayRes
import com.talauncher.R
import com.talauncher.domain.quotes.QuotesProvider
import kotlin.random.Random

/**
 * Android resource-based implementation of QuotesProvider.
 *
 * Architecture:
 * - Implements QuotesProvider interface (Dependency Inversion Principle)
 * - Encapsulates Android-specific resource loading
 * - Uses Random as a dependency for testability
 *
 * Design Pattern: Strategy pattern implementation
 * SOLID:
 * - Single Responsibility: Only handles loading quotes from Android resources
 * - Dependency Inversion: Depends on Random abstraction, implements QuotesProvider interface
 * - Open/Closed: Can be extended for different resource sources
 *
 * @param context Android context for accessing resources
 * @param quotesArrayRes Resource ID of the string array containing quotes
 * @param random Random instance for quote selection (injectable for testing)
 */
class MotivationalQuotesProvider(
    context: Context,
    @ArrayRes private val quotesArrayRes: Int = R.array.motivational_quotes,
    private val random: Random = Random.Default
) : QuotesProvider {
    private val appContext = context.applicationContext ?: context

    /**
     * Returns a random motivational quote from Android resources.
     *
     * @return A motivational quote, or empty string if no quotes available
     */
    override fun getRandomQuote(): String {
        val quotes = appContext.resources.getStringArray(quotesArrayRes)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (quotes.isEmpty()) {
            return ""
        }

        return quotes.random(random)
    }
}
