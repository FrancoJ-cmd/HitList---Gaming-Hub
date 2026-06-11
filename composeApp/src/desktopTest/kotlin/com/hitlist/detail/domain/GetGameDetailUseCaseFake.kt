package com.hitlist.detail.domain

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult

class GetGameDetailUseCaseFake(
    private val result: AppResult<GameDetail> = AppResult.Failure(AppError.Unexpected())
) : GetGameDetailUseCase {
    var lastAppId: Int? = null
    var lastName: String? = null

    override suspend fun execute(appId: Int, name: String): AppResult<GameDetail> {
        lastAppId = appId
        lastName = name
        return result
    }
}
