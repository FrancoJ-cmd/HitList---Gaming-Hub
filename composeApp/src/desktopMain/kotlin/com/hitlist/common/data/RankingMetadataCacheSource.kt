package com.hitlist.common.data

import com.hitlist.ranking.data.GameMetadataSeed

interface RankingMetadataCacheSource {
    fun getRankingMetadata(): Pair<Map<Int, GameMetadataSeed>, Long>?
    fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long)
}
