package com.hitlist.ranking.domain

data class Ranking(val games: List<RankedGame>) {

    companion object {
        private const val TREND_WEIGHT = 0.6
        private const val RATING_WEIGHT = 0.4
        private const val MIN_REVIEWS_THRESHOLD = 50

        fun from(rawGames: List<CombinedRankingEntry>, previous: Ranking? = null): Ranking {
            val maxPlayers = rawGames.maxOfOrNull { it.concurrentPlayers } ?: 0
            val ranked = rawGames.mapNotNull { entry ->
                val totalReviews = entry.positiveReviews + entry.negativeReviews
                val positiveRatio = if (totalReviews > 0) entry.positiveReviews.toDouble() / totalReviews else 0.0
                val score = calculateScore(entry.concurrentPlayers, maxPlayers, positiveRatio, totalReviews)
                if (score == 0.0 && totalReviews < MIN_REVIEWS_THRESHOLD) return@mapNotNull null
                RankedGame(
                    steamAppId = entry.appId,
                    name = entry.name,
                    headerImageUrl = entry.headerImageUrl,
                    score = score,
                    currentPlayers = entry.concurrentPlayers,
                    positiveRatio = positiveRatio,
                    reviewScoreDesc = describeReviewScore(entry.positiveReviews, entry.negativeReviews),
                    totalReviews = totalReviews,
                    genres = entry.genres,
                    isTrending = false
                )
            }.sortedByDescending { it.score }
            return Ranking(markTrending(ranked, previous?.games ?: emptyList()))
        }

        private fun calculateScore(
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

        private fun markTrending(current: List<RankedGame>, previous: List<RankedGame>): List<RankedGame> {
            val prevPositions = previous.mapIndexed { idx, g -> g.steamAppId to idx }.toMap()
            return current.mapIndexed { currentIdx, game ->
                val prevIdx = prevPositions[game.steamAppId]
                game.copy(isTrending = prevIdx != null && currentIdx < prevIdx)
            }
        }

        private fun describeReviewScore(positive: Int, negative: Int): String {
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
}
