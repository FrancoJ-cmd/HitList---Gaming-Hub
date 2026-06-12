package com.hitlist.detail.data

import com.hitlist.detail.domain.Deal
import com.hitlist.detail.domain.GameDetail
import kotlinx.serialization.Serializable

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