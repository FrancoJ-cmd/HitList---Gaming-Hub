package com.hitlist.detail.domain

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class GetGameDetailUseCaseFake(
    private val result: AppResult<Stale<GameDetail>> = AppResult.Failure(AppError.Unexpected())
) : GetGameDetailUseCase {
    var lastAppId: Int? = null
    var lastName: String? = null

    override suspend fun execute(appId: Int, name: String): AppResult<Stale<GameDetail>> {
        lastAppId = appId
        lastName = name
        return result
    }
}
