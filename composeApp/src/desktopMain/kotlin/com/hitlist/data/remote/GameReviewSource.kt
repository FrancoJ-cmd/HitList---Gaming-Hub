package com.hitlist.data.remote

interface GameReviewSource {
    suspend fun getGameReviews(appId: Int): ReviewInfo?
}
