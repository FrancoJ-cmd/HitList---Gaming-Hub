package com.hitlist.data.remote

interface PlayerCountSource {
    suspend fun getCurrentPlayers(appId: Int): Int
}
