package com.hitlist.detail.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

interface GameDetailRepository {
    suspend fun getGameDetail(appId: Int, name: String): AppResult<Stale<GameDetail>>
}
