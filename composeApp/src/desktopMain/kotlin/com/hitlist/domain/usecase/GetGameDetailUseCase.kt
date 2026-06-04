package com.hitlist.domain.usecase

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.result.AppResult

interface GetGameDetailUseCase {
    suspend fun execute(appId: Int, name: String): AppResult<GameDetail>
}
