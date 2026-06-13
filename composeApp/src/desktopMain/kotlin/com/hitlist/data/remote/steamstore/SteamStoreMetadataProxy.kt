package com.hitlist.data.remote.steamstore

import com.hitlist.data.remote.GameMetadata
import com.hitlist.data.remote.GameMetadataSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class SteamStoreMetadataProxy(private val client: HttpClient) : GameMetadataSource {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getGameMetadata(appId: Int): GameMetadata? = runCatching {
        val raw = client.get("/api/appdetails?appids=$appId").body<JsonObject>()
        val wrapper = json.decodeFromJsonElement<AppDetailsWrapperDto>(raw[appId.toString()]!!)
        wrapper.data?.takeIf { wrapper.success }?.toGameMetadata()
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
}

