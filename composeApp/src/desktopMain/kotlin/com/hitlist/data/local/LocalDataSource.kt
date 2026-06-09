package com.hitlist.data.local

import com.hitlist.data.remote.GameMetadataSeed
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.entity.RankedGame

interface LocalDataSource {
    fun getRankedGames(): Pair<List<RankedGame>, Long>?
    fun saveRankedGames(games: List<RankedGame>)

    fun getRankingMetadata(): Pair<Map<Int, GameMetadataSeed>, Long>?
    fun saveRankingMetadata(metadata: Map<Int, GameMetadataSeed>, cachedAt: Long)

    fun getGameDetail(appId: Int): Pair<GameDetail, Long>?
    fun saveGameDetail(detail: GameDetail)

    fun getDeals(gameName: String): Pair<List<Deal>, Long>?
    fun saveDeals(gameName: String, deals: List<Deal>)

    fun getNews(query: String): Pair<List<NewsArticle>, Long>?
    fun saveNews(query: String, articles: List<NewsArticle>)
}
