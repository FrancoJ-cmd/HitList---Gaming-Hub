package com.hitlist.data.remote.steamspy

import com.hitlist.data.remote.GameRankingSource
import com.hitlist.data.remote.GameSeed
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

class SteamSpyProxy(private val client: HttpClient) : GameRankingSource {

    override suspend fun getTopGames(): List<GameSeed> {
        val raw = client.get("/api.php?request=top100in2weeks").body<JsonObject>()
        val json = Json { ignoreUnknownKeys = true }
        return raw.values.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement<SteamSpyGameDto>(element) }.getOrNull()
                ?.let { dto ->
                    GameSeed(
                        appId = dto.appId,
                        name = dto.name,
                        currentPlayers = dto.ccu,
                        positiveReviews = dto.positive,
                        negativeReviews = dto.negative,
                        genres = dto.genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    )
                }
        }
    }

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
