package com.hitlist.common.data

import com.hitlist.detail.domain.GameDetail
import com.hitlist.news.domain.NewsArticle
import com.hitlist.ranking.data.GameMetadataSeed
import com.hitlist.ranking.domain.RankedGame
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LocalDataSourceImpl(cacheDir: File = defaultCacheDir()) :
    RankingCacheSource, RankingMetadataCacheSource, GameDetailCacheSource, NewsCacheSource {

    private val rankedGamesFile = File(cacheDir, "ranked_games.json")
    private val rankingMetadataFile = File(cacheDir, "ranking_metadata.json")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        cacheDir.mkdirs()
    }

    override fun getRankedGames(): Pair<List<RankedGame>, Long>? =
        readCache<CacheEntry<List<SerializableRankedGame>>>(rankedGamesFile)
            ?.let { entry -> entry.data.map { it.toDomain() } to entry.cachedAt }

    override fun saveRankedGames(games: List<RankedGame>) {
        val entry = CacheEntry(
            cachedAt = System.currentTimeMillis(),
            data = games.map { SerializableRankedGame.fromDomain(it) }
        )
        rankedGamesFile.writeText(json.encodeToString(entry))
    }

    override fun getRankingMetadata(): Pair<Map<Int, GameMetadataSeed>, Long>? =
        readCache<CacheEntry<Map<Int, SerializableGameMetadataSeed>>>(rankingMetadataFile)
            ?.let { entry -> entry.data.mapValues { it.value.toModel() } to entry.cachedAt }

    override fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long) {
        val entry = CacheEntry(
            cachedAt = cachedAt,
            data = metadata.mapValues { SerializableGameMetadataSeed.fromModel(it.value) }
        )
        rankingMetadataFile.writeText(json.encodeToString(entry))
    }

    override fun getGameDetail(appId: Int): Pair<GameDetail, Long>? {
        val file = detailFile(appId)
        return readCache<CacheEntry<SerializableGameDetail>>(file)
            ?.let { entry -> entry.data.toDomain() to entry.cachedAt }
    }

    override fun saveGameDetail(detail: GameDetail) {
        val entry = CacheEntry(
            cachedAt = System.currentTimeMillis(),
            data = SerializableGameDetail.fromDomain(detail)
        )
        detailFile(detail.steamAppId).writeText(json.encodeToString(entry))
    }

    override fun getNews(query: String): Pair<List<NewsArticle>, Long>? {
        val file = newsFile(query)
        return readCache<CacheEntry<List<SerializableNewsArticle>>>(file)
            ?.let { entry -> entry.data.map { it.toDomain() } to entry.cachedAt }
    }

    override fun saveNews(query: String, articles: List<NewsArticle>) {
        val entry = CacheEntry(
            cachedAt = System.currentTimeMillis(),
            data = articles.map { SerializableNewsArticle.fromDomain(it) }
        )
        newsFile(query).writeText(json.encodeToString(entry))
    }

    private inline fun <reified T> readCache(file: File): T? =
        runCatching {
            if (file.exists()) json.decodeFromString<T>(file.readText()) else null
        }.getOrNull()

    private fun detailFile(appId: Int) = File(rankedGamesFile.parentFile, "game_detail_$appId.json")

    private fun newsFile(query: String) =
        File(rankedGamesFile.parentFile, "news_${query.hashCode()}.json")

    companion object {
        fun defaultCacheDir() = File("cache")
    }
}
