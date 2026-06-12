package com.hitlist.ranking.data

import com.hitlist.ranking.domain.RankedGame

class RankingCacheFake : RankingCacheSource, RankingMetadataCacheSource {
    private var rankedGames: Pair<List<RankedGame>, Long>? = null
    private var rankingMetadata: Pair<Map<Int, GameMetadataSeed>, Long>? = null

    override fun getRankedGames() = rankedGames
    override fun saveRankedGames(games: List<RankedGame>) {
        rankedGames = games to System.currentTimeMillis()
    }

    override fun getRankingMetadata() = rankingMetadata
    override fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long) {
        rankingMetadata = metadata to cachedAt
    }

    fun seedRankedGames(games: List<RankedGame>, cachedAt: Long = System.currentTimeMillis()) {
        rankedGames = games to cachedAt
    }

    fun seedRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long = System.currentTimeMillis()) {
        rankingMetadata = metadata to cachedAt
    }
}