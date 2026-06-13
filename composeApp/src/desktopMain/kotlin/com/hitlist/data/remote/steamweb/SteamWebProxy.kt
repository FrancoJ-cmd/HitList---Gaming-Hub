package com.hitlist.data.remote.steamweb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import com.hitlist.data.remote.PlayerCountSource
import kotlinx.serialization.json.Json

class SteamWebProxy(private val client: HttpClient) : PlayerCountSource {

    override suspend fun getCurrentPlayers(appId: Int): Int = runCatching {
        client.get("/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?appid=$appId")
            .body<CurrentPlayersResponseDto>()
            .response.playerCount
    }.getOrDefault(0)
}
