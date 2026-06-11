package com.hitlist.detail.data

interface PlayerCountSource {
    suspend fun getCurrentPlayers(appId: Int): Int
}
