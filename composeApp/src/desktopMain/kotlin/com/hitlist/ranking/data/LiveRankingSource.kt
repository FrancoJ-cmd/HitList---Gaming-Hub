package com.hitlist.ranking.data

interface LiveRankingSource {
    suspend fun getLiveRanking(): LiveRanking
}

data class LiveRanking(
    val entries: List<LiveRankEntry>,
    val lastUpdate: Long
)

data class LiveRankEntry(
    val appId: Int,
    val concurrentPlayers: Int,
    val rank: Int
)
