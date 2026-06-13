package com.hitlist.data.remote.steamstore

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SteamStoreClientFactory {

    fun create(): HttpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(DefaultRequest) {
            url { protocol = URLProtocol.HTTPS; host = "store.steampowered.com" }
        }
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
    }
}
