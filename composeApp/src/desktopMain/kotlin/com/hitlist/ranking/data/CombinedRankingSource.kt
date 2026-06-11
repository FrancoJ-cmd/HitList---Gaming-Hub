package com.hitlist.ranking.data

interface CombinedRankingSource {
    suspend fun getCombinedRanking(): CombinedRanking
}

data class CombinedRanking(
    val entries: List<CombinedRankingEntry>,
    val lastUpdate: Long
)

data class CombinedRankingEntry(
    val appId: Int,
    val name: String,
    val headerImageUrl: String,
    val concurrentPlayers: Int,
    val positiveReviews: Int,
    val negativeReviews: Int,
    val genres: List<String>
)
