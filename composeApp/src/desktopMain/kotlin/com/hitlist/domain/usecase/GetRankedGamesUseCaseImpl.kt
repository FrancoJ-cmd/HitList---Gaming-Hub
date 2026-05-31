package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository

class GetRankedGamesUseCaseImpl(
    private val gameRepository: GameRepository
) : GetRankedGamesUseCase {

    override suspend fun execute(): Result<Pair<List<RankedGame>, Boolean>> =
        gameRepository.getRankedGames()

    companion object {
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

        fun markTrending(
            current: List<RankedGame>,
            previous: List<RankedGame>
        ): List<RankedGame> {
            val prevPositions = previous.mapIndexed { idx, g -> g.steamAppId to idx }.toMap()
            return current.mapIndexed { currentIdx, game ->
                val prevIdx = prevPositions[game.steamAppId]
                game.copy(isTrending = prevIdx != null && currentIdx < prevIdx)
            }
        }
    }
}
