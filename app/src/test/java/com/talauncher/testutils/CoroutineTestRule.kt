package com.talauncher.testutils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that provides a test dispatcher for coroutine testing
 * This rule sets up the Main dispatcher for testing and ensures proper cleanup
 *
 * Usage in test classes:
 * ```
 * @get:Rule
 * val coroutineTestRule = CoroutineTestRule()
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTestRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }

    /**
     * Get the test dispatcher for use in tests
     */
    fun getTestDispatcher(): TestDispatcher = testDispatcher
}

/**
 * Extension function to advance time in tests using TestScope
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineTestRule.advanceTimeBy(delayTimeMillis: Long) {
    // Note: Time advancement should be done through TestScope in the actual test
    // This is a placeholder for future enhancement
}

/**
 * Extension function to advance until idle in tests using TestScope
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineTestRule.advanceUntilIdle() {
    // Note: Use TestScope.advanceUntilIdle() in actual tests
    // This is a placeholder for future enhancement
}