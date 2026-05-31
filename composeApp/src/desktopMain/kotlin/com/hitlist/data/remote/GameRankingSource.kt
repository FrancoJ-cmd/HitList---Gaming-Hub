package com.hitlist.data.remote

interface GameRankingSource {
    suspend fun getTopGames(): List<GameSeed>
}
