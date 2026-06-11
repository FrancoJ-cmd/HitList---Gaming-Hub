package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetRankedGamesUseCaseFake(
    private vararg val emissions: AppResult<Pair<List<RankedGame>, Boolean>> =
        arrayOf(AppResult.Success(emptyList<RankedGame>() to false))
) : GetRankedGamesUseCase {
    var observeCallCount = 0

    override fun observe(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>> = flow {
        observeCallCount++
        emissions.forEach { emit(it) }
    }
}
