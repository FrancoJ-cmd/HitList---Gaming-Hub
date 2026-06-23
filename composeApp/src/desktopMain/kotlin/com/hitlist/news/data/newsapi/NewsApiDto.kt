package com.hitlist.news.data.newsapi

import kotlinx.serialization.Serializable

@Serializable
data class NewsResponseDto(
    val status: String = "",
    val articles: List<NewsArticleDto> = emptyList()
)

@Serializable
data class NewsArticleDto(
    val title: String = "",
    val description: String? = null,
    val source: NewsSourceDto = NewsSourceDto(),
    val url: String = "",
    val urlToImage: String? = null,
    val publishedAt: String = ""
)

@Serializable
data class NewsSourceDto(
    val name: String = ""
)
