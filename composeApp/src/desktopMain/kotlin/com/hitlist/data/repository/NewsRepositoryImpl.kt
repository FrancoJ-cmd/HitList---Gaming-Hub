package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.mapper.toAppResult
import com.hitlist.data.remote.GameNewsSource
import com.hitlist.data.remote.GeneralNewsSource
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.repository.NewsRepository
import com.hitlist.domain.result.AppResult

class NewsRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val generalNewsSource: GeneralNewsSource,
    private val gameNewsSource: GameNewsSource
) : NewsRepository {

    override suspend fun getNews(query: String): AppResult<List<NewsArticle>> {
        val cached = localDataSource.getNews(query)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return AppResult.Success(cached.first)
        }

        val freshResult = runCatching {
            val articles = generalNewsSource.getNews(query)
            if (articles.isNotEmpty()) localDataSource.saveNews(query, articles)
            articles
        }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> freshResult
            is AppResult.Failure -> if (cached != null) AppResult.Success(cached.first) else freshResult
        }
    }

    override suspend fun getNewsForGame(appId: Int): AppResult<List<NewsArticle>> {
        val cacheKey = "steam_$appId"
        val cached = localDataSource.getNews(cacheKey)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return AppResult.Success(cached.first)
        }

        val freshResult = runCatching {
            val articles = gameNewsSource.getNewsForGame(appId)
            if (articles.isNotEmpty()) localDataSource.saveNews(cacheKey, articles)
            articles
        }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> freshResult
            is AppResult.Failure -> if (cached != null) AppResult.Success(cached.first) else freshResult
        }
    }
}
