package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import kotlinx.coroutines.flow.Flow

interface GetRankedGamesUseCase {
    fun observe(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>>
}
