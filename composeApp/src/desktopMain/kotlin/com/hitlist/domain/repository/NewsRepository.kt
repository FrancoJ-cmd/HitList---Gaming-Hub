package com.hitlist.domain.repository

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.result.AppResult

interface NewsRepository {
    suspend fun getNews(query: String): AppResult<List<NewsArticle>>
    suspend fun getNewsForGame(appId: Int): AppResult<List<NewsArticle>>
}
