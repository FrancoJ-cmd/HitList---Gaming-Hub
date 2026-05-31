package com.hitlist.data.remote

import com.hitlist.domain.entity.NewsArticle

interface GeneralNewsSource {
    suspend fun getNews(query: String): List<NewsArticle>
}
