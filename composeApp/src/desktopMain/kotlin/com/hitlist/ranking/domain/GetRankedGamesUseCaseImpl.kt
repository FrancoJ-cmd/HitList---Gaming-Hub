package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlinx.coroutines.flow.Flow

class GetRankedGamesUseCaseImpl(
    private val rankingRepository: RankingRepository
) : GetRankedGamesUseCase {
    override fun observe(): Flow<AppResult<Stale<Ranking>>> =
        rankingRepository.observeRankedGames()
}
