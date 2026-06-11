package com.hitlist.detail.domain

import com.hitlist.common.domain.AppResult

interface GameDetailRepository {
    suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail>
}
