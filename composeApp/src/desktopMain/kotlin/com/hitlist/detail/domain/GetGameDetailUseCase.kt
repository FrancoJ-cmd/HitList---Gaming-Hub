package com.hitlist.detail.domain

import com.hitlist.common.domain.AppResult

interface GetGameDetailUseCase {
    suspend fun execute(appId: Int, name: String): AppResult<GameDetail>
}
