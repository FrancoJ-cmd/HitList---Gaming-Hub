package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RankingRepositoryFake(
    private val rankedGamesResult: AppResult<Stale<Ranking>> =
        AppResult.Success(Stale(Ranking(emptyList()), isStale = false))
) : RankingRepository {
    override fun observeRankedGames(): Flow<AppResult<Stale<Ranking>>> = flowOf(rankedGamesResult)
}
