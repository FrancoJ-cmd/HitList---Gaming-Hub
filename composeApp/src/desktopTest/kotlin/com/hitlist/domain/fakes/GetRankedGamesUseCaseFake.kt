package com.hitlist.domain.fakes

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.usecase.GetRankedGamesUseCase

class GetRankedGamesUseCaseFake(
    private val result: AppResult<Pair<List<RankedGame>, Boolean>> = AppResult.Success(emptyList<RankedGame>() to false)
) : GetRankedGamesUseCase {
    var executeCallCount = 0

    override suspend fun execute(): AppResult<Pair<List<RankedGame>, Boolean>> {
        executeCallCount++
        return result
    }
}
