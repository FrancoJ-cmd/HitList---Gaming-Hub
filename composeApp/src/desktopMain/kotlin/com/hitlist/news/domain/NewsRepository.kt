package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

interface NewsRepository {
    suspend fun getNews(query: String): AppResult<Stale<List<NewsArticle>>>
    suspend fun getNewsForGame(appId: Int): AppResult<Stale<List<NewsArticle>>>
}
