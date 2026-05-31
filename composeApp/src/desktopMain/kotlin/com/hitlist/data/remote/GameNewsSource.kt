package com.hitlist.data.remote

import com.hitlist.domain.entity.NewsArticle

interface GameNewsSource {
    suspend fun getNewsForGame(appId: Int): List<NewsArticle>
}
