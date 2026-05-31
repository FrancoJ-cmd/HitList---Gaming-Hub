package com.hitlist.domain.repository

import com.hitlist.domain.entity.NewsArticle

interface NewsRepository {
    suspend fun getNews(query: String): Result<List<NewsArticle>>
    suspend fun getNewsForGame(appId: Int): Result<List<NewsArticle>>
}
