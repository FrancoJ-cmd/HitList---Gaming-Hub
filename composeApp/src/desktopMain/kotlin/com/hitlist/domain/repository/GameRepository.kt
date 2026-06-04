package com.hitlist.domain.repository

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult

interface GameRepository {
    suspend fun getRankedGames(): AppResult<Pair<List<RankedGame>, Boolean>>
    suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail>
}
