package com.hitlist.data.local

import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.entity.RankedGame
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class LocalDataSourceImpl(cacheDir: File = defaultCacheDir()) : LocalDataSource {

    private val rankedGamesFile = File(cacheDir, "ranked_games.json")
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

    override fun getDeals(gameName: String): Pair<List<Deal>, Long>? {
        val file = dealsFile(gameName)
        return readCache<CacheEntry<List<SerializableDeal>>>(file)
            ?.let { entry -> entry.data.map { it.toDomain() } to entry.cachedAt }
    }

    override fun saveDeals(gameName: String, deals: List<Deal>) {
        val entry = CacheEntry(
            cachedAt = System.currentTimeMillis(),
            data = deals.map { SerializableDeal.fromDomain(it) }
        )
        dealsFile(gameName).writeText(json.encodeToString(entry))
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

    private fun detailFile(appId: Int) =
        File(rankedGamesFile.parentFile, "game_detail_$appId.json")

    private fun dealsFile(gameName: String) =
        File(rankedGamesFile.parentFile, "deals_${safeKey(gameName)}.json")

    private fun newsFile(query: String) =
        File(rankedGamesFile.parentFile, "news_${safeKey(query)}.json")

    private fun safeKey(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    companion object {
        fun defaultCacheDir() = File("cache")
    }
}
