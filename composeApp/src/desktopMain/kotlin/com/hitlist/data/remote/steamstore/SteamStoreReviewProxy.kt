package com.hitlist.data.remote.steamstore

import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.ReviewInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class SteamStoreReviewProxy(private val client: HttpClient) : GameReviewSource {

    override suspend fun getGameReviews(appId: Int): ReviewInfo? = runCatching {
        client.get("/appreviews/$appId?json=1&num_per_page=0&language=all")
            .body<AppReviewsResponseDto>()
            .querySummary
            ?.toReviewInfo()
    }.getOrNull()

    private fun ReviewSummaryDto.toReviewInfo() = ReviewInfo(
        reviewScoreDesc = reviewScoreDesc,
        totalPositive = totalPositive,
        totalReviews = totalReviews
    )
}

