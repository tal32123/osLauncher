package com.talauncher.utils

import java.io.PrintWriter
import java.io.StringWriter

interface ErrorHandler {
    fun showError(
        title: String,
        message: String,
        throwable: Throwable? = null,
        onRetry: (() -> Unit)? = null,
        retryButtonText: String = "Retry"
    )
    fun requestPermission(permission: String, rationale: String, onResult: (Boolean) -> Unit)
}

object ErrorUtils {
    fun getStackTraceString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).use { printWriter ->
            throwable.printStackTrace(printWriter)
            printWriter.flush()
        }
        return stringWriter.toString()
    }
}