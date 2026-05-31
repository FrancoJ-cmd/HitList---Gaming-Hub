package com.hitlist.data.remote.steamnews

import com.hitlist.data.remote.GameNewsSource
import com.hitlist.domain.entity.NewsArticle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SteamNewsProxy(private val client: HttpClient) : GameNewsSource {

    override suspend fun getNewsForGame(appId: Int): List<NewsArticle> = runCatching {
        client.get("/ISteamNews/GetNewsForApp/v2/?appid=$appId&count=10&maxlength=300&format=json")
            .body<SteamNewsResponseDto>()
            .appNews
            ?.newsItems
            ?.filter { it.title.isNotBlank() && it.url.isNotBlank() }
            ?.map { it.toDomain() }
            ?: emptyList()
    }.getOrDefault(emptyList())

    private fun SteamNewsItemDto.toDomain() = NewsArticle(
        title = title,
        description = contents.stripHtml().take(200).ifBlank { null },
        sourceName = feedLabel.ifBlank { "Steam" },
        url = url,
        imageUrl = null,
        publishedAt = formatDate(date)
    )

    private fun String.stripHtml(): String =
        replace(Regex("<[^>]+>"), "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").trim()

    private fun formatDate(epochSeconds: Long): String = runCatching {
        Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrDefault(epochSeconds.toString())

    companion object {
        fun create() = SteamNewsProxy(
            HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(DefaultRequest) {
                    url { protocol = URLProtocol.HTTPS; host = "api.steampowered.com" }
                }
                install(HttpTimeout) { requestTimeoutMillis = 10_000 }
            }
        )
    }
}
