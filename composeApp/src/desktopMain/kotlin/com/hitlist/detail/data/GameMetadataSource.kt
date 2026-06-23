package com.hitlist.detail.data

interface GameMetadataSource {
    suspend fun getGameMetadata(appId: Int): GameMetadata?
}
