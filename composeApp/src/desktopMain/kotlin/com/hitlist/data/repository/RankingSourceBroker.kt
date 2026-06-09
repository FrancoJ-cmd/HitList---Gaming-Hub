package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.remote.CombinedRanking
import com.hitlist.data.remote.CombinedRankingEntry
import com.hitlist.data.remote.CombinedRankingSource
import com.hitlist.data.remote.GameMetadataSeed
import com.hitlist.data.remote.LiveRankingSource
import com.hitlist.data.remote.RankingMetadataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class RankingSourceBroker(
    private val localDataSource: LocalDataSource,
    private val liveRankingSource: LiveRankingSource,
    private val rankingMetadataSource: RankingMetadataSource
) : CombinedRankingSource {

    override suspend fun getCombinedRanking(): CombinedRanking {
        val live = liveRankingSource.getLiveRanking()
        val metadata = loadRankingMetadata(live.entries.map { it.appId })
        val entries = live.entries.mapNotNull { entry ->
            val seed = metadata[entry.appId] ?: return@mapNotNull null
            CombinedRankingEntry(
                appId = entry.appId,
                name = seed.name,
                concurrentPlayers = entry.concurrentPlayers,
                positiveReviews = seed.positiveReviews,
                negativeReviews = seed.negativeReviews,
                genres = seed.genres
            )
        }
        return CombinedRanking(entries, live.lastUpdate)
    }

    private suspend fun loadRankingMetadata(appIds: List<Int>): Map<Int, GameMetadataSeed> {
        val cached = localDataSource.getRankingMetadata()
        val valid = cached != null && CachePolicy.isValid(cached.second, CachePolicy.SEED_LIST_TTL_MS)
        val base = if (valid) cached!!.first else rankingMetadataSource.getBulkMetadata()
        val cachedAt = if (valid) cached!!.second else System.currentTimeMillis()

        val missing = appIds.filterNot { base.containsKey(it) }
        val merged = if (missing.isEmpty()) base else base + fetchMissingMetadata(missing)
        localDataSource.saveRankingMetadata(merged, cachedAt)
        return merged
    }

    private suspend fun fetchMissingMetadata(appIds: List<Int>): Map<Int, GameMetadataSeed> = coroutineScope {
        appIds
            .map { id -> async { rankingMetadataSource.getMetadata(id) } }
            .awaitAll()
            .filterNotNull()
            .associateBy { it.appId }
    }
}
