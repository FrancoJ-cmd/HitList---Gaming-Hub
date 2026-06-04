package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult

interface GetRankedGamesUseCase {
    suspend fun execute(): AppResult<Pair<List<RankedGame>, Boolean>>
}
