package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.remote.newsapi.NewsApiProxy
import com.hitlist.data.remote.steamnews.SteamNewsProxy
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.repository.NewsRepository

class NewsRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val newsApiProxy: NewsApiProxy,
    private val steamNewsProxy: SteamNewsProxy
) : NewsRepository {

    override suspend fun getNews(query: String): Result<List<NewsArticle>> {
        val cached = localDataSource.getNews(query)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return Result.success(cached.first)
        }

        return runCatching {
            val articles = newsApiProxy.getNews(query)
            if (articles.isNotEmpty()) localDataSource.saveNews(query, articles)
            articles
        }.recoverCatching { exception ->
            cached?.first ?: throw exception
        }
    }

    override suspend fun getNewsForGame(appId: Int): Result<List<NewsArticle>> {
        val cacheKey = "steam_$appId"
        val cached = localDataSource.getNews(cacheKey)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return Result.success(cached.first)
        }

        return runCatching {
            val articles = steamNewsProxy.getNewsForGame(appId)
            if (articles.isNotEmpty()) localDataSource.saveNews(cacheKey, articles)
            articles
        }.recoverCatching { exception ->
            cached?.first ?: throw exception
        }
    }
}
