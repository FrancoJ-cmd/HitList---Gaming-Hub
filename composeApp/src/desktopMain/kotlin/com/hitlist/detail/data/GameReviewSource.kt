package com.hitlist.detail.data

interface GameReviewSource {
    suspend fun getGameReviews(appId: Int): ReviewInfo?
}
