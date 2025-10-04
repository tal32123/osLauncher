package com.talauncher.data.repository

import com.talauncher.data.database.NewsDao
import com.talauncher.data.model.NewsArticle
import com.talauncher.data.model.NewsCategory
import com.talauncher.data.model.NewsRefreshInterval
import com.talauncher.data.model.LauncherSettings
import com.talauncher.service.NewsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NewsRepository(
    private val newsDao: NewsDao,
    private val settingsRepository: SettingsRepository,
    private val newsService: NewsService
) {
    fun getLatestArticles(): Flow<List<NewsArticle>> = newsDao.getLatestArticles()

    suspend fun updateSelectedCategories(categories: Set<NewsCategory>) {
        val settings = settingsRepository.getSettingsSync()
        val csv = categories.joinToString(",") { it.name }
        settingsRepository.updateSettings(settings.copy(newsCategoriesCsv = csv))
    }

    suspend fun updateRefreshInterval(interval: NewsRefreshInterval) {
        val settings = settingsRepository.getSettingsSync()
        settingsRepository.updateSettings(settings.copy(newsRefreshInterval = interval))
    }

    suspend fun refreshIfNeeded() {
        val settings = settingsRepository.getSettingsSync()
        val selected = parseCategories(settings)
        val interval = settings.newsRefreshInterval
        val last = settings.newsLastFetchedAt
        val now = System.currentTimeMillis()
        val threshold = when (interval) {
            NewsRefreshInterval.HOURLY -> 60L * 60 * 1000
            NewsRefreshInterval.DAILY -> 24L * 60 * 60 * 1000
        }
        if (selected.isEmpty()) return
        if (last == null || now - last >= threshold) {
            fetchAndStore(selected)
        }
    }

    suspend fun forceRefresh() {
        val settings = settingsRepository.getSettingsSync()
        val selected = parseCategories(settings)
        if (selected.isNotEmpty()) fetchAndStore(selected)
    }

    private suspend fun fetchAndStore(categories: Set<NewsCategory>) {
        val articles = newsService.fetchArticles(categories)
        withContext(Dispatchers.IO) {
            newsDao.clearAll()
            if (articles.isNotEmpty()) {
                newsDao.insertAll(articles)
            }
        }
        val settings = settingsRepository.getSettingsSync()
        settingsRepository.updateSettings(settings.copy(newsLastFetchedAt = System.currentTimeMillis()))
    }

    private fun parseCategories(settings: LauncherSettings): Set<NewsCategory> {
        val csv = settings.newsCategoriesCsv ?: return emptySet()
        return csv.split(',')
            .mapNotNull { name ->
                runCatching { NewsCategory.valueOf(name) }.getOrNull()
            }.toSet()
    }
}

