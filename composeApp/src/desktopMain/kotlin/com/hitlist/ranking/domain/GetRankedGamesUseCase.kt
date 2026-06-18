package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlinx.coroutines.flow.Flow

interface GetRankedGamesUseCase {
    fun observe(): Flow<AppResult<Stale<Ranking>>>
}
