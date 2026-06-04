package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.util.RankingCalculator

class GetRankedGamesUseCaseImpl(
    private val gameRepository: GameRepository
) : GetRankedGamesUseCase {

    override suspend fun execute(): AppResult<Pair<List<RankedGame>, Boolean>> =
        gameRepository.getRankedGames()

    companion object {
        val MIN_REVIEWS_THRESHOLD get() = RankingCalculator.MIN_REVIEWS_THRESHOLD
        fun calculateScore(currentPlayers: Int, maxPlayersInDataset: Int, positiveRatio: Double, totalReviews: Int) =
            RankingCalculator.calculateScore(currentPlayers, maxPlayersInDataset, positiveRatio, totalReviews)
        fun markTrending(current: List<RankedGame>, previous: List<RankedGame>) =
            RankingCalculator.markTrending(current, previous)
        fun describeReviewScore(positive: Int, negative: Int) =
            RankingCalculator.describeReviewScore(positive, negative)
    }
}
