package com.hitlist.data.fakes

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.repository.NewsRepository

class NewsRepositoryFake(
    private val result: Result<List<NewsArticle>> = Result.success(emptyList()),
    private val gameNewsResult: Result<List<NewsArticle>> = Result.success(emptyList())
) : NewsRepository {
    var lastQuery: String? = null
    var lastAppId: Int? = null

    override suspend fun getNews(query: String): Result<List<NewsArticle>> {
        lastQuery = query
        return result
    }

    override suspend fun getNewsForGame(appId: Int): Result<List<NewsArticle>> {
        lastAppId = appId
        return gameNewsResult
    }
}
