package com.hitlist.detail.domain

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class GameDetailRepositoryFake(
    private val gameDetailResult: AppResult<Stale<GameDetail>> = AppResult.Failure(AppError.Unexpected())
) : GameDetailRepository {
    override suspend fun getGameDetail(appId: Int, name: String): AppResult<Stale<GameDetail>> = gameDetailResult
}
