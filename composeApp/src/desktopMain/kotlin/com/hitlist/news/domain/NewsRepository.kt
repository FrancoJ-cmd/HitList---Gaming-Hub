package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

interface NewsRepository {
    suspend fun getNews(query: String): AppResult<List<NewsArticle>>
    suspend fun getNewsForGame(appId: Int): AppResult<List<NewsArticle>>
}
