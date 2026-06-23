package com.hitlist.news.data

import com.hitlist.news.domain.NewsArticle

interface GameNewsSource {
    suspend fun getNewsForGame(appId: Int): List<NewsArticle>
}
