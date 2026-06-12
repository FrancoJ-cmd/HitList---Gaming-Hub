package com.hitlist.ranking.data

import com.hitlist.ranking.domain.RankedGame
import kotlinx.serialization.Serializable

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