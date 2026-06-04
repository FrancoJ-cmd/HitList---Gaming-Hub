package com.hitlist.domain.fakes

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.usecase.GetGameDetailUseCase

class GetGameDetailUseCaseFake(
    private val result: AppResult<GameDetail> = AppResult.Failure(com.hitlist.domain.error.AppError.Unexpected())
) : GetGameDetailUseCase {
    var lastAppId: Int? = null
    var lastName: String? = null

    override suspend fun execute(appId: Int, name: String): AppResult<GameDetail> {
        lastAppId = appId
        lastName = name
        return result
    }
}
