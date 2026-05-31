package com.hitlist.domain.fakes

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.usecase.GetRankedGamesUseCase

class GetRankedGamesUseCaseFake(
    private val result: Result<Pair<List<RankedGame>, Boolean>> = Result.success(emptyList<RankedGame>() to false)
) : GetRankedGamesUseCase {
    var executeCallCount = 0

    override suspend fun execute(): Result<Pair<List<RankedGame>, Boolean>> {
        executeCallCount++
        return result
    }
}
