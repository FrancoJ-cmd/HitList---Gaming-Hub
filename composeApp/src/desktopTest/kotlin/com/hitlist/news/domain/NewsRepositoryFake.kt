package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

class NewsRepositoryFake(
    private val result: AppResult<List<NewsArticle>> = AppResult.Success(emptyList()),
    private val gameNewsResult: AppResult<List<NewsArticle>> = AppResult.Success(emptyList())
) : NewsRepository {
    var lastQuery: String? = null
    var lastAppId: Int? = null

    override suspend fun getNews(query: String): AppResult<List<NewsArticle>> {
        lastQuery = query
        return result
    }

    override suspend fun getNewsForGame(appId: Int): AppResult<List<NewsArticle>> {
        lastAppId = appId
        return gameNewsResult
    }
}
