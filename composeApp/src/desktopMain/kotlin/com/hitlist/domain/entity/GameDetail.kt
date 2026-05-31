package com.hitlist.domain.entity

data class GameDetail(
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
    val deals: List<Deal>
)
