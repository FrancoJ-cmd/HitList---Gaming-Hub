package com.hitlist.data.fakes

import com.hitlist.data.local.LocalDataSource
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.entity.RankedGame

class LocalDataSourceFake : LocalDataSource {
    private var rankedGames: Pair<List<RankedGame>, Long>? = null
    private val gameDetails = mutableMapOf<Int, Pair<GameDetail, Long>>()
    private val deals = mutableMapOf<String, Pair<List<Deal>, Long>>()
    private val news = mutableMapOf<String, Pair<List<NewsArticle>, Long>>()

    override fun getRankedGames() = rankedGames
    override fun saveRankedGames(games: List<RankedGame>) {
        rankedGames = games to System.currentTimeMillis()
    }

    override fun getGameDetail(appId: Int) = gameDetails[appId]
    override fun saveGameDetail(detail: GameDetail) {
        gameDetails[detail.steamAppId] = detail to System.currentTimeMillis()
    }

    override fun getDeals(gameName: String) = deals[gameName]
    override fun saveDeals(gameName: String, dealList: List<Deal>) {
        deals[gameName] = dealList to System.currentTimeMillis()
    }

    override fun getNews(query: String) = news[query]
    override fun saveNews(query: String, articles: List<NewsArticle>) {
        news[query] = articles to System.currentTimeMillis()
    }

    fun seedRankedGames(games: List<RankedGame>, cachedAt: Long = System.currentTimeMillis()) {
        rankedGames = games to cachedAt
    }

    fun seedGameDetail(detail: GameDetail, cachedAt: Long = System.currentTimeMillis()) {
        gameDetails[detail.steamAppId] = detail to cachedAt
    }

    fun seedNews(query: String, articles: List<NewsArticle>, cachedAt: Long = System.currentTimeMillis()) {
        news[query] = articles to cachedAt
    }
}
