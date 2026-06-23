package com.hitlist.news.data

import com.hitlist.news.domain.NewsArticle
import kotlinx.serialization.Serializable

@Serializable
data class SerializableNewsArticle(
    val title: String,
    val description: String?,
    val sourceName: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String
) {
    fun toDomain() = NewsArticle(title, description, sourceName, url, imageUrl, publishedAt)

    companion object {
        fun fromDomain(a: NewsArticle) =
            SerializableNewsArticle(a.title, a.description, a.sourceName, a.url, a.imageUrl, a.publishedAt)
    }
}