package com.hitlist.data.remote.steamweb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SteamWebProxy(private val client: HttpClient) {

    suspend fun getCurrentPlayers(appId: Int): Int = runCatching {
        client.get("/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?appid=$appId")
            .body<CurrentPlayersResponseDto>()
            .response.playerCount
    }.getOrDefault(0)

    companion object {
        fun create() = SteamWebProxy(
            HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(DefaultRequest) {
                    url { protocol = URLProtocol.HTTPS; host = "api.steampowered.com" }
                }
                install(HttpTimeout) { requestTimeoutMillis = 5_000 }
            }
        )
    }
}
