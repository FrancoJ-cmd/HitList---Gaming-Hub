package com.hitlist.common.data

import com.hitlist.detail.domain.GameDetail
import com.hitlist.news.domain.NewsArticle
import com.hitlist.ranking.data.GameMetadataSeed
import com.hitlist.ranking.domain.RankedGame

class LocalDataSourceFake :
    RankingCacheSource, RankingMetadataCacheSource, GameDetailCacheSource, NewsCacheSource {
    private var rankedGames: Pair<List<RankedGame>, Long>? = null
    private var rankingMetadata: Pair<Map<Int, GameMetadataSeed>, Long>? = null
    private val gameDetails = mutableMapOf<Int, Pair<GameDetail, Long>>()
    private val news = mutableMapOf<String, Pair<List<NewsArticle>, Long>>()

    override fun getRankedGames() = rankedGames
    override fun saveRankedGames(games: List<RankedGame>) {
        rankedGames = games to System.currentTimeMillis()
    }

    override fun getRankingMetadata() = rankingMetadata
    override fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long) {
        rankingMetadata = metadata to cachedAt
    }

    override fun getGameDetail(appId: Int) = gameDetails[appId]
    override fun saveGameDetail(detail: GameDetail) {
        gameDetails[detail.steamAppId] = detail to System.currentTimeMillis()
    }

    override fun getNews(query: String) = news[query]
    override fun saveNews(query: String, articles: List<NewsArticle>) {
        news[query] = articles to System.currentTimeMillis()
    }

    fun seedRankedGames(games: List<RankedGame>, cachedAt: Long = System.currentTimeMillis()) {
        rankedGames = games to cachedAt
    }

    fun seedRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long = System.currentTimeMillis()) {
        rankingMetadata = metadata to cachedAt
    }

    fun seedGameDetail(detail: GameDetail, cachedAt: Long = System.currentTimeMillis()) {
        gameDetails[detail.steamAppId] = detail to cachedAt
    }

    fun seedNews(query: String, articles: List<NewsArticle>, cachedAt: Long = System.currentTimeMillis()) {
        news[query] = articles to cachedAt
    }
}
