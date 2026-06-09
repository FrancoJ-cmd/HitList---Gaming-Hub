package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface GetRankedGamesUseCase {
    fun observe(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>>
}
