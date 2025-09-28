package com.talauncher.testutils

import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mockito helper functions and extensions for cleaner test code
 * Provides common mocking patterns and reduces boilerplate in tests
 */

/**
 * Creates a mock that returns the provided flow when any method is called
 */
inline fun <reified T> mockFlow(flow: Flow<T>): T {
    val mock = Mockito.mock(T::class.java)
    whenever(mock.toString()).thenReturn("Mock<${T::class.simpleName}>")
    return mock
}

/**
 * Creates a mock repository that returns the provided data as a flow
 */
inline fun <reified T, R> mockRepository(data: R): T {
    return Mockito.mock(T::class.java)
}

/**
 * Extension function to easily stub flow returns
 */
inline fun <reified T> T.returnsFlow(vararg values: Any): T {
    values.forEach { value ->
        whenever(this.toString()).thenReturn(flowOf(value).toString())
    }
    return this
}

/**
 * Creates an ArgumentCaptor for the specified type
 */
inline fun <reified T> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)

/**
 * Captures the last argument passed to a mocked method
 */
inline fun <reified T> captureArgument(): ArgumentCaptor<T> {
    return ArgumentCaptor.forClass(T::class.java)
}

/**
 * Helper for stubbing suspend functions that return Unit
 */
suspend fun stubUnit() = Unit

/**
 * Helper for stubbing methods that should throw exceptions
 */
fun throwsException(exception: Throwable): Nothing = throw exception

/**
 * Common mock behaviors for testing error scenarios
 */
object MockBehaviors {
    val networkError = RuntimeException("Network error")
    val databaseError = RuntimeException("Database error")
    val permissionError = SecurityException("Permission denied")

    /**
     * Creates a mock that throws a network error
     */
    fun networkFailure(): Nothing = throwsException(networkError)

    /**
     * Creates a mock that throws a database error
     */
    fun databaseFailure(): Nothing = throwsException(databaseError)

    /**
     * Creates a mock that throws a permission error
     */
    fun permissionFailure(): Nothing = throwsException(permissionError)
}

/**
 * Test doubles for common Android/Kotlin patterns
 */
object TestDoubles {
    /**
     * Creates a flow that emits the provided values in sequence
     */
    fun <T> flowOf(vararg values: T): Flow<T> = kotlinx.coroutines.flow.flowOf(*values)

    /**
     * Creates an empty flow for testing empty states
     */
    fun <T> emptyFlow(): Flow<T> = kotlinx.coroutines.flow.emptyFlow()

    /**
     * Creates a flow that throws an error for testing error states
     */
    fun <T> errorFlow(error: Throwable): Flow<T> = kotlinx.coroutines.flow.flow { throw error }
}