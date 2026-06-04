package com.hitlist.domain.util

import com.hitlist.domain.entity.RankedGame

object RankingCalculator {
    const val TREND_WEIGHT = 0.6
    const val RATING_WEIGHT = 0.4
    const val MIN_REVIEWS_THRESHOLD = 50

    fun calculateScore(
        currentPlayers: Int,
        maxPlayersInDataset: Int,
        positiveRatio: Double,
        totalReviews: Int
    ): Double {
        if (totalReviews < MIN_REVIEWS_THRESHOLD) return 0.0
        val trendScore = if (maxPlayersInDataset == 0) 0.0
                         else currentPlayers.toDouble() / maxPlayersInDataset
        return TREND_WEIGHT * trendScore + RATING_WEIGHT * positiveRatio
    }

    fun markTrending(current: List<RankedGame>, previous: List<RankedGame>): List<RankedGame> {
        val prevPositions = previous.mapIndexed { idx, g -> g.steamAppId to idx }.toMap()
        return current.mapIndexed { currentIdx, game ->
            val prevIdx = prevPositions[game.steamAppId]
            game.copy(isTrending = prevIdx != null && currentIdx < prevIdx)
        }
    }

    fun describeReviewScore(positive: Int, negative: Int): String {
        val total = positive + negative
        if (total < 10) return ""
        val ratio = positive.toDouble() / total
        return when {
            ratio >= 0.95 && total >= 500 -> "Overwhelmingly Positive"
            ratio >= 0.80 -> "Very Positive"
            ratio >= 0.70 -> "Mostly Positive"
            ratio >= 0.40 -> "Mixed"
            ratio >= 0.20 -> "Mostly Negative"
            else -> "Overwhelmingly Negative"
        }
    }
}
