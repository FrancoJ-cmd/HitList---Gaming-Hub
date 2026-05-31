package com.hitlist.data.remote.steamstore

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppDetailsWrapperDto(
    val success: Boolean = false,
    val data: AppDetailsDto? = null
)

@Serializable
data class AppDetailsDto(
    val name: String = "",
    @SerialName("short_description") val shortDescription: String = "",
    @SerialName("header_image") val headerImage: String = "",
    val screenshots: List<ScreenshotDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val developers: List<String> = emptyList(),
    @SerialName("release_date") val releaseDate: ReleaseDateDto? = null,
    val metacritic: MetacriticDto? = null,
    @SerialName("is_free") val isFree: Boolean = false
)

@Serializable
data class ScreenshotDto(
    @SerialName("path_thumbnail") val pathThumbnail: String = "",
    @SerialName("path_full") val pathFull: String = ""
)

@Serializable
data class GenreDto(
    val id: String = "",
    val description: String = ""
)

@Serializable
data class ReleaseDateDto(
    val date: String = ""
)

@Serializable
data class MetacriticDto(
    val score: Int = 0
)

@Serializable
data class AppReviewsResponseDto(
    @SerialName("query_summary") val querySummary: ReviewSummaryDto? = null
)

@Serializable
data class ReviewSummaryDto(
    @SerialName("review_score_desc") val reviewScoreDesc: String = "",
    @SerialName("total_positive") val totalPositive: Int = 0,
    @SerialName("total_reviews") val totalReviews: Int = 0
)
