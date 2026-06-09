package com.hitlist.domain.repository

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeRankedGames(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>>
    suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail>
}
