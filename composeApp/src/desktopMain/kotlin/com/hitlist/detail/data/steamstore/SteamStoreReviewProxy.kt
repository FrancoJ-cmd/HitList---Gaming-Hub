package com.hitlist.detail.data.steamstore

import com.hitlist.detail.data.GameReviewSource
import com.hitlist.detail.data.ReviewInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SteamStoreReviewProxy(private val client: HttpClient) : GameReviewSource {

    override suspend fun getGameReviews(appId: Int): ReviewInfo? = runCatching {
        client.get("/appreviews/$appId?json=1&num_per_page=0&language=all&l=english")
            .body<AppReviewsResponseDto>()
            .querySummary
            ?.toReviewInfo()
    }.getOrNull()

    private fun ReviewSummaryDto.toReviewInfo() = ReviewInfo(
        reviewScoreDesc = reviewScoreDesc,
        totalPositive = totalPositive,
        totalReviews = totalReviews
    )

    companion object {
        fun create() = SteamStoreReviewProxy(
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
