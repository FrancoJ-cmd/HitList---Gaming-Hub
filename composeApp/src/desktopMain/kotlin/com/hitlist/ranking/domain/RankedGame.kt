package com.hitlist.ranking.domain

data class RankedGame(
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
)
