package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy

class CachedRankingMetadataSource(
    private val delegate: RankingMetadataSource,
    private val cache: RankingMetadataCacheSource
) : RankingMetadataSource {

    override suspend fun getBulkMetadata(): Map<Int, GameMetadataSeed> {
        cache.getRankingMetadata()?.let { (data, cachedAt) ->
            if (CachePolicy.isValid(cachedAt, CachePolicy.SEED_LIST_TTL_MS)) return data
        }
        return delegate.getBulkMetadata().also { cache.saveRankingMetadata(it, System.currentTimeMillis()) }
    }

    override suspend fun getMetadata(appId: Int): GameMetadataSeed? = delegate.getMetadata(appId)
}
