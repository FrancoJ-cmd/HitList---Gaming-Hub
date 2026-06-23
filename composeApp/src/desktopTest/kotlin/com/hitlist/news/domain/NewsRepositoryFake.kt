package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class NewsRepositoryFake(
    private val result: AppResult<Stale<List<NewsArticle>>> = AppResult.Success(Stale(emptyList(), false)),
    private val gameNewsResult: AppResult<Stale<List<NewsArticle>>> = AppResult.Success(Stale(emptyList(), false))
) : NewsRepository {
    var lastQuery: String? = null
    var lastAppId: Int? = null

    override suspend fun getNews(query: String): AppResult<Stale<List<NewsArticle>>> {
        lastQuery = query
        return result
    }

    override suspend fun getNewsForGame(appId: Int): AppResult<Stale<List<NewsArticle>>> {
        lastAppId = appId
        return gameNewsResult
    }
}
