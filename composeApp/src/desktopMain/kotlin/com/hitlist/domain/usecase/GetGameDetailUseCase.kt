package com.hitlist.domain.usecase

import com.hitlist.domain.entity.GameDetail

interface GetGameDetailUseCase {
    suspend fun execute(appId: Int, name: String): Result<GameDetail>
}
