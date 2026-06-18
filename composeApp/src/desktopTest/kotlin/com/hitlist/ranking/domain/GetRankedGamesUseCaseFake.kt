package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetRankedGamesUseCaseFake(
    private vararg val emissions: AppResult<Stale<Ranking>> =
        arrayOf(AppResult.Success(Stale(Ranking(emptyList()), isStale = false)))
) : GetRankedGamesUseCase {
    var observeCallCount = 0

    override fun observe(): Flow<AppResult<Stale<Ranking>>> = flow {
        observeCallCount++
        emissions.forEach { emit(it) }
    }
}
