package com.hitlist.data.remote.steamstore

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class SteamStoreProxy(private val client: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAppDetails(appId: Int): AppDetailsDto? = runCatching {
        val raw = client.get("/api/appdetails?appids=$appId").body<JsonObject>()
        val wrapper = json.decodeFromJsonElement<AppDetailsWrapperDto>(raw[appId.toString()]!!)
        if (wrapper.success) wrapper.data else null
    }.getOrNull()

    suspend fun getAppReviews(appId: Int): ReviewSummaryDto? = runCatching {
        client.get("/appreviews/$appId?json=1&num_per_page=0&language=all")
            .body<AppReviewsResponseDto>()
            .querySummary
    }.getOrNull()

    companion object {
        fun create() = SteamStoreProxy(
            HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(DefaultRequest) {
                    url { protocol = URLProtocol.HTTPS; host = "store.steampowered.com" }
                }
                install(HttpTimeout) { requestTimeoutMillis = 10_000 }
            }
        )
    }
}
