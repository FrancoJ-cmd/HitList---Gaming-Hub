package com.hitlist.ranking.data

import com.hitlist.ranking.domain.CombinedRanking
import com.hitlist.ranking.domain.CombinedRankingEntry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CombinedRankingSourceImpl(
    private val liveRankingSource: LiveRankingSource,
    private val rankingMetadataSource: RankingMetadataSource
) : CombinedRankingSource {

    override suspend fun getCombinedRanking(): CombinedRanking {
        val live = liveRankingSource.getLiveRanking()
        val bulkMetadata = rankingMetadataSource.getBulkMetadata()
        val missing = live.entries.map { it.appId }.filterNot { bulkMetadata.containsKey(it) }
        val allMetadata = if (missing.isEmpty()) bulkMetadata else bulkMetadata + fetchMissingMetadata(missing)
        val entries = live.entries.mapNotNull { entry ->
            val seed = allMetadata[entry.appId] ?: return@mapNotNull null
            CombinedRankingEntry(
                appId = entry.appId,
                name = seed.name,
                headerImageUrl = steamHeaderImageUrl(entry.appId),
                concurrentPlayers = entry.concurrentPlayers,
                positiveReviews = seed.positiveReviews,
                negativeReviews = seed.negativeReviews,
                genres = seed.genres
            )
        }
        return CombinedRanking(entries, live.lastUpdate)
    }

    private suspend fun fetchMissingMetadata(appIds: List<Int>): Map<Int, GameMetadataSeed> =
        coroutineScope {
            appIds
                .map { id -> async { rankingMetadataSource.getMetadata(id) } }
                .awaitAll()
                .filterNotNull()
                .associateBy { it.appId }
        }

    private fun steamHeaderImageUrl(appId: Int) =
        "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg"
}
