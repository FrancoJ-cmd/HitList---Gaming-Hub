package com.hitlist.common.data

import com.hitlist.detail.domain.Deal
import com.hitlist.detail.domain.GameDetail
import com.hitlist.news.domain.NewsArticle
import com.hitlist.ranking.data.GameMetadataSeed
import com.hitlist.ranking.domain.RankedGame
import kotlinx.serialization.Serializable

@Serializable
data class CacheEntry<T>(val cachedAt: Long, val data: T)

@Serializable
data class SerializableGameMetadataSeed(
    val appId: Int,
    val name: String,
    val positiveReviews: Int,
    val negativeReviews: Int,
    val genres: List<String>
) {
    fun toModel() = GameMetadataSeed(appId, name, positiveReviews, negativeReviews, genres)

    companion object {
        fun fromModel(s: GameMetadataSeed) =
            SerializableGameMetadataSeed(s.appId, s.name, s.positiveReviews, s.negativeReviews, s.genres)
    }
}

@Serializable
data class SerializableRankedGame(
    val steamAppId: Int,
    val name: String,
    val headerImageUrl: String,
    val score: Double,
    val currentPlayers: Int,
    val positiveRatio: Double,
    val reviewScoreDesc: String,
    val totalReviews: Int,
    val genres: List<String>,
    val isTrending: Boolean
) {
    fun toDomain() = RankedGame(
        steamAppId, name, headerImageUrl, score, currentPlayers,
        positiveRatio, reviewScoreDesc, totalReviews, genres, isTrending
    )

    companion object {
        fun fromDomain(g: RankedGame) = SerializableRankedGame(
            g.steamAppId, g.name, g.headerImageUrl, g.score, g.currentPlayers,
            g.positiveRatio, g.reviewScoreDesc, g.totalReviews, g.genres, g.isTrending
        )
    }
}

@Serializable
data class SerializableGameDetail(
    val steamAppId: Int,
    val name: String,
    val shortDescription: String,
    val headerImageUrl: String,
    val screenshots: List<String>,
    val metacriticScore: Int?,
    val genres: List<String>,
    val developers: List<String>,
    val releaseDate: String?,
    val isFree: Boolean,
    val currentPlayers: Int,
    val positiveRatio: Double,
    val reviewScoreDesc: String,
    val totalReviews: Int,
    val deals: List<SerializableDeal>
) {
    fun toDomain() = GameDetail(
        steamAppId, name, shortDescription, headerImageUrl, screenshots, metacriticScore,
        genres, developers, releaseDate, isFree, currentPlayers,
        positiveRatio, reviewScoreDesc, totalReviews, deals.map { it.toDomain() }
    )

    companion object {
        fun fromDomain(d: GameDetail) = SerializableGameDetail(
            d.steamAppId, d.name, d.shortDescription, d.headerImageUrl, d.screenshots,
            d.metacriticScore, d.genres, d.developers, d.releaseDate, d.isFree,
            d.currentPlayers, d.positiveRatio, d.reviewScoreDesc, d.totalReviews,
            d.deals.map { SerializableDeal.fromDomain(it) }
        )
    }
}

@Serializable
data class SerializableDeal(
    val storeName: String,
    val currentPrice: String,
    val retailPrice: String,
    val savingsPercent: Double,
    val cheapestEverPrice: String
) {
    fun toDomain() = Deal(storeName, currentPrice, retailPrice, savingsPercent, cheapestEverPrice)

    companion object {
        fun fromDomain(d: Deal) =
            SerializableDeal(d.storeName, d.currentPrice, d.retailPrice, d.savingsPercent, d.cheapestEverPrice)
    }
}

@Serializable
data class SerializableNewsArticle(
    val title: String,
    val description: String?,
    val sourceName: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String
) {
    fun toDomain() = NewsArticle(title, description, sourceName, url, imageUrl, publishedAt)

    companion object {
        fun fromDomain(a: NewsArticle) =
            SerializableNewsArticle(a.title, a.description, a.sourceName, a.url, a.imageUrl, a.publishedAt)
    }
}
