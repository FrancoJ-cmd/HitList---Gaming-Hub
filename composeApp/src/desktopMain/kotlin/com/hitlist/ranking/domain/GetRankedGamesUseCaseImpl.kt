package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import kotlinx.coroutines.flow.Flow

class GetRankedGamesUseCaseImpl(
    private val rankingRepository: RankingRepository
) : GetRankedGamesUseCase {
    override fun observe(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>> =
        rankingRepository.observeRankedGames()
}
