package com.talauncher.domain.quotes

/**
 * Interface for providing motivational quotes.
 *
 * Architecture:
 * - Follows Dependency Inversion Principle (depends on abstraction, not concrete implementation)
 * - Enables easy testing with mock implementations
 * - Allows different quote sources (resources, API, database)
 *
 * Design Pattern: Strategy pattern for different quote sources
 * SOLID: Interface Segregation - single method interface
 */
interface QuotesProvider {
    /**
     * Returns a random motivational quote.
     *
     * @return A motivational quote string, or empty string if none available
     */
    fun getRandomQuote(): String
}
