package com.hitlist.common.data

import com.hitlist.news.domain.NewsArticle

interface NewsCacheSource {
    fun getNews(query: String): Pair<List<NewsArticle>, Long>?
    fun saveNews(query: String, articles: List<NewsArticle>)
}
