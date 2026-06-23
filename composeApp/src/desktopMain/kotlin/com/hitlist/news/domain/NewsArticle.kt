package com.hitlist.news.domain

data class NewsArticle(
    val title: String,
    val description: String?,
    val sourceName: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String
)
