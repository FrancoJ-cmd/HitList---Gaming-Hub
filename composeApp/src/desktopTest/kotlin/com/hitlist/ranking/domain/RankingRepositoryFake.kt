package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RankingRepositoryFake(
    private val rankedGamesResult: AppResult<Pair<List<RankedGame>, Boolean>> = AppResult.Success(emptyList<RankedGame>() to false)
) : RankingRepository {
    override fun observeRankedGames(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>> = flowOf(rankedGamesResult)
}
