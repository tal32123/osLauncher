package com.talauncher.ui.home

import android.content.Context
import androidx.annotation.ArrayRes
import com.talauncher.R
import kotlin.random.Random

class MotivationalQuotesProvider(
    context: Context,
    @ArrayRes private val quotesArrayRes: Int = R.array.motivational_quotes,
    private val random: Random = Random.Default
) {
    private val appContext = context.applicationContext ?: context

    fun getRandomQuote(): String {
        val quotes = appContext.resources.getStringArray(quotesArrayRes)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (quotes.isEmpty()) {
            return ""
        }

        return quotes.random(random)
    }
}
