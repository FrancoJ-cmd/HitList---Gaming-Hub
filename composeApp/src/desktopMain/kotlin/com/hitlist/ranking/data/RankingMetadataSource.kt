package com.hitlist.ranking.data

interface RankingMetadataSource {
    suspend fun getBulkMetadata(): Map<Int, GameMetadataSeed>
    suspend fun getMetadata(appId: Int): GameMetadataSeed?
}

data class GameMetadataSeed(
    val appId: Int,
    val name: String,
    val positiveReviews: Int,
    val negativeReviews: Int,
    val genres: List<String>
)
