package com.hitlist.detail.domain

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult

class GameDetailRepositoryFake(
    private val gameDetailResult: AppResult<GameDetail> = AppResult.Failure(AppError.Unexpected())
) : GameDetailRepository {
    override suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail> = gameDetailResult
}
