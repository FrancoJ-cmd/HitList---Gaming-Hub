package com.hitlist.data.remote.steamstore

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import com.hitlist.data.remote.GameMetadata
import com.hitlist.data.remote.GameMetadataSource
import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.ReviewInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class SteamStoreProxy(private val client: HttpClient) : GameMetadataSource, GameReviewSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getGameMetadata(appId: Int): GameMetadata? = runCatching {
        val raw = client.get("/api/appdetails?appids=$appId").body<JsonObject>()
        val wrapper = json.decodeFromJsonElement<AppDetailsWrapperDto>(raw[appId.toString()]!!)
        wrapper.data?.takeIf { wrapper.success }?.toGameMetadata()
    }.getOrNull()

    override suspend fun getGameReviews(appId: Int): ReviewInfo? = runCatching {
        client.get("/appreviews/$appId?json=1&num_per_page=0&language=all")
            .body<AppReviewsResponseDto>()
            .querySummary
            ?.toReviewInfo()
    }.getOrNull()

    private fun AppDetailsDto.toGameMetadata() = GameMetadata(
        name = name,
        shortDescription = shortDescription,
        headerImageUrl = headerImage,
        screenshots = screenshots.take(5).map { it.pathFull },
        metacriticScore = metacritic?.score,
        genres = genres.map { it.description },
        developers = developers,
        releaseDate = releaseDate?.date,
        isFree = isFree
    )

    private fun ReviewSummaryDto.toReviewInfo() = ReviewInfo(
        reviewScoreDesc = reviewScoreDesc,
        totalPositive = totalPositive,
        totalReviews = totalReviews
    )

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
