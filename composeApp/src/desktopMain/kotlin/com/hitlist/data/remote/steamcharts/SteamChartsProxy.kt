package com.hitlist.data.remote.steamcharts

import com.hitlist.data.remote.LiveRankEntry
import com.hitlist.data.remote.LiveRanking
import com.hitlist.data.remote.LiveRankingSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SteamChartsProxy(private val client: HttpClient) : LiveRankingSource {

    override suspend fun getLiveRanking(): LiveRanking {
        val data = client.get("/ISteamChartsService/GetGamesByConcurrentPlayers/v1/")
            .body<MostPlayedResponseDto>()
            .response
        return LiveRanking(
            entries = data.ranks.map { LiveRankEntry(it.appId, it.concurrentInGame, it.rank) },
            lastUpdate = data.lastUpdate
        )
    }

    companion object {
        fun create() = SteamChartsProxy(
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
