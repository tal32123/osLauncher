package com.talauncher.service

import android.util.Xml
import com.talauncher.data.model.NewsArticle
import com.talauncher.data.model.NewsCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class NewsService {
    private val dateFormats = listOf(
        // Common RSS pubDate formats
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    )

    suspend fun fetchArticles(categories: Set<NewsCategory>): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        val errors = mutableListOf<String>()

        for (category in categories) {
            val url = feedUrlFor(category) ?: continue
            runCatching {
                val (stream, connection) = open(url)
                stream.use {
                    connection.use {
                        articles += parseRss(stream, category)
                    }
                }
            }.onFailure { error ->
                errors.add("Failed to fetch ${category.name}: ${error.message}")
            }
        }

        // Sort by published desc and cap
        articles.sortedByDescending { it.publishedAtMillis }.take(100)
    }

    private fun feedUrlFor(category: NewsCategory): String? {
        // Known WSJ RSS endpoints (subject to change)
        return when (category) {
            NewsCategory.WORLD -> "https://feeds.a.dj.com/rss/RSSWorldNews.xml"
            NewsCategory.US -> "https://feeds.a.dj.com/rss/RSSUSNews.xml"
            NewsCategory.POLITICS -> "https://feeds.a.dj.com/rss/RSSPolitics.xml"
            NewsCategory.ECONOMY -> "https://feeds.a.dj.com/rss/RSSEconomy.xml"
            NewsCategory.BUSINESS -> "https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml"
            NewsCategory.TECH -> "https://feeds.a.dj.com/rss/RSSWSJD.xml"
            NewsCategory.MARKETS -> "https://feeds.a.dj.com/rss/RSSMarketsMain.xml"
            NewsCategory.OPINION -> "https://feeds.a.dj.com/rss/RSSOpinion.xml"
        }
    }

    private fun open(urlString: String): Pair<InputStream, HttpURLConnection> {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.connect()

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            conn.disconnect()
            throw IllegalStateException("HTTP error code: $responseCode")
        }

        return Pair(conn.inputStream, conn)
    }

    private fun parseRss(input: InputStream, category: NewsCategory): List<NewsArticle> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(input, null)

        var eventType = parser.eventType
        var insideItem = false
        var title: String? = null
        var link: String? = null
        var pubDate: String? = null
        val results = mutableListOf<NewsArticle>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase(Locale.US)) {
                        "item" -> {
                            insideItem = true
                            title = null
                            link = null
                            pubDate = null
                        }
                        "title" -> if (insideItem) title = parser.nextText()
                        "link" -> if (insideItem) link = parser.nextText()
                        "pubdate" -> if (insideItem) pubDate = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (insideItem && parser.name.equals("item", ignoreCase = true)) {
                        val t = title?.trim()
                        val l = link?.trim()
                        if (!t.isNullOrEmpty() && !l.isNullOrEmpty()) {
                            val published = parseDate(pubDate)
                            // Only include articles with valid dates to avoid sorting issues
                            if (published != null) {
                                results += NewsArticle(
                                    title = t,
                                    link = l,
                                    category = category.name,
                                    publishedAtMillis = published
                                )
                            }
                        }
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        return results
    }

    private fun parseDate(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        for (fmt in dateFormats) {
            runCatching {
                return fmt.parse(text)?.time
            }
        }
        return null
    }
}

