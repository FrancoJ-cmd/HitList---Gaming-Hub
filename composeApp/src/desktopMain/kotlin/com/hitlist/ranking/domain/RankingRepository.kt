package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    fun observeRankedGames(): Flow<AppResult<Stale<Ranking>>>
}
