package com.hitlist.ranking.data.steamspy

import com.hitlist.ranking.data.GameMetadataSeed
import com.hitlist.ranking.data.RankingMetadataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class SteamSpyProxy(private val client: HttpClient) : RankingMetadataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getBulkMetadata(): Map<Int, GameMetadataSeed> {
        val raw = client.get("/api.php?request=top100in2weeks").body<JsonObject>()
        return raw.values.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<SteamSpyGameDto>(element) }.getOrNull()?.toSeed()
        }.associateBy { it.appId }
    }

    override suspend fun getMetadata(appId: Int): GameMetadataSeed? = runCatching {
        client.get("/api.php?request=appdetails&appid=$appId").body<SteamSpyGameDto>().toSeed()
    }.getOrNull()

    private fun SteamSpyGameDto.toSeed() = GameMetadataSeed(
        appId = appId,
        name = name,
        positiveReviews = positive,
        negativeReviews = negative,
        genres = genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
    )

    companion object {
        fun create() = SteamSpyProxy(
            HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(DefaultRequest) {
                    url { protocol = URLProtocol.HTTPS; host = "steamspy.com" }
                }
                install(HttpTimeout) { requestTimeoutMillis = 10_000 }
            }
        )
    }
}
