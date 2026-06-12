package com.hitlist.ranking.data

interface RankingMetadataCacheSource {
    fun getRankingMetadata(): Pair<Map<Int, GameMetadataSeed>, Long>?
    fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long)
}