package com.hitlist.data.remote

data class GameSeed(val appId: Int, val name: String)

data class GameMetadata(
    val name: String,
    val shortDescription: String,
    val headerImageUrl: String,
    val screenshots: List<String>,
    val metacriticScore: Int?,
    val genres: List<String>,
    val developers: List<String>,
    val releaseDate: String?,
    val isFree: Boolean
)

data class ReviewInfo(
    val reviewScoreDesc: String,
    val totalPositive: Int,
    val totalReviews: Int
)
