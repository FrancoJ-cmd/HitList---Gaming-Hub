package com.hitlist.detail.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

interface GetGameDetailUseCase {
    suspend fun execute(appId: Int, name: String): AppResult<Stale<GameDetail>>
}
