package com.hitlist.news.data

import com.hitlist.news.domain.NewsArticle

interface GeneralNewsSource {
    suspend fun getNews(query: String): List<NewsArticle>
}
