package com.hitlist.domain.repository

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame

interface GameRepository {
    suspend fun getRankedGames(): Result<Pair<List<RankedGame>, Boolean>>
    suspend fun getGameDetail(appId: Int, name: String): Result<GameDetail>
}
