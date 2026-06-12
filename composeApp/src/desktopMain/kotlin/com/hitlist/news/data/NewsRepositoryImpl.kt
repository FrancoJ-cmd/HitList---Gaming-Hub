package com.hitlist.news.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.data.toAppResult
import com.hitlist.common.domain.AppResult
import com.hitlist.news.domain.NewsArticle
import com.hitlist.news.domain.NewsRepository

class NewsRepositoryImpl(
    private val newsCacheSource: NewsCacheSource,
    private val generalNewsSource: GeneralNewsSource,
    private val gameNewsSource: GameNewsSource
) : NewsRepository {

    override suspend fun getNews(query: String): AppResult<List<NewsArticle>> {
        val cached = newsCacheSource.getNews(query)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return AppResult.Success(cached.first)
        }

        val freshResult = runCatching {
            val articles = generalNewsSource.getNews(query)
            if (articles.isNotEmpty()) newsCacheSource.saveNews(query, articles)
            articles
        }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> freshResult
            is AppResult.Failure -> if (cached != null) AppResult.Success(cached.first) else freshResult
        }
    }

    override suspend fun getNewsForGame(appId: Int): AppResult<List<NewsArticle>> {
        val cacheKey = gameNewsCacheKey(appId)
        val cached = newsCacheSource.getNews(cacheKey)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return AppResult.Success(cached.first)
        }

        val freshResult = runCatching {
            val articles = gameNewsSource.getNewsForGame(appId)
            if (articles.isNotEmpty()) newsCacheSource.saveNews(cacheKey, articles)
            articles
        }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> freshResult
            is AppResult.Failure -> if (cached != null) AppResult.Success(cached.first) else freshResult
        }
    }

    private fun gameNewsCacheKey(appId: Int) = "steam_$appId"
}
