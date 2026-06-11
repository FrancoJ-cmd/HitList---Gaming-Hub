package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    fun observeRankedGames(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>>
}
