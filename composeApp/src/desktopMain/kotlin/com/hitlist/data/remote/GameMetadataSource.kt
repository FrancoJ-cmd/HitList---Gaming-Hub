package com.hitlist.data.remote

interface GameMetadataSource {
    suspend fun getGameMetadata(appId: Int): GameMetadata?
}
