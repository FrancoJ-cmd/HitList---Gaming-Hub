package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy

class CachedRankingMetadataSource(
    private val delegate: RankingMetadataSource
) : RankingMetadataSource {

    private var cached: Map<Int, GameMetadataSeed>? = null
    private var cachedAt = 0L

    override suspend fun getBulkMetadata(): Map<Int, GameMetadataSeed> {
        cached?.let { if (CachePolicy.isValid(cachedAt, CachePolicy.SEED_LIST_TTL_MS)) return it }
        return delegate.getBulkMetadata().also {
            cached = it
            cachedAt = System.currentTimeMillis()
        }
    }

    override suspend fun getMetadata(appId: Int): GameMetadataSeed? = delegate.getMetadata(appId)
}