package com.hitlist.data.remote.newsapi

import com.hitlist.data.remote.GeneralNewsSource
import com.hitlist.domain.entity.NewsArticle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NewsApiProxy(
    private val client: HttpClient,
    private val apiKey: String
) : GeneralNewsSource {

    override suspend fun getNews(query: String): List<NewsArticle> {
        if (apiKey.isBlank()) return emptyList()
        return runCatching {
            client.get("/v2/everything") {
                parameter("q", "\"$query\"")
                parameter("language", "en")
                parameter("sortBy", "publishedAt")
                parameter("apiKey", apiKey)
            }
                .body<NewsResponseDto>()
                .articles
                .filter { it.title.isNotBlank() && it.title != "[Removed]" }
                .map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun NewsArticleDto.toDomain() = NewsArticle(
        title = title,
        description = description,
        sourceName = source.name,
        url = url,
        imageUrl = urlToImage,
        publishedAt = publishedAt
    )
}
