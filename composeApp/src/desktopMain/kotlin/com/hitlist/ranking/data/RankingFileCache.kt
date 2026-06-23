package com.hitlist.ranking.data

import com.hitlist.common.data.CacheEntry
import com.hitlist.common.data.JsonFileStore
import com.hitlist.ranking.domain.RankedGame

class RankingFileCache(private val store: JsonFileStore) :
    RankingCacheSource, RankingMetadataCacheSource {

    private val rankedGamesFile = store.file("ranked_games.json")
    private val rankingMetadataFile = store.file("ranking_metadata.json")

    override fun getRankedGames(): Pair<List<RankedGame>, Long>? =
        store.read<CacheEntry<List<SerializableRankedGame>>>(rankedGamesFile)
            ?.let { entry -> entry.data.map { it.toDomain() } to entry.cachedAt }

    override fun saveRankedGames(games: List<RankedGame>) {
        store.write(
            rankedGamesFile,
            CacheEntry(System.currentTimeMillis(), games.map { SerializableRankedGame.fromDomain(it) })
        )
    }

    override fun getRankingMetadata(): Pair<Map<Int, GameMetadataSeed>, Long>? =
        store.read<CacheEntry<Map<Int, SerializableGameMetadataSeed>>>(rankingMetadataFile)
            ?.let { entry -> entry.data.mapValues { it.value.toModel() } to entry.cachedAt }

    override fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long) {
        store.write(
            rankingMetadataFile,
            CacheEntry(cachedAt, metadata.mapValues { SerializableGameMetadataSeed.fromModel(it.value) })
        )
    }
}