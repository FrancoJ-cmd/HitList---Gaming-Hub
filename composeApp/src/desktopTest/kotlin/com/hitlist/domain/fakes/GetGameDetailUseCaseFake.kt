package com.hitlist.domain.fakes

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.usecase.GetGameDetailUseCase

class GetGameDetailUseCaseFake(
    private val result: Result<GameDetail> = Result.failure(Exception("Not set"))
) : GetGameDetailUseCase {
    var lastAppId: Int? = null
    var lastName: String? = null

    override suspend fun execute(appId: Int, name: String): Result<GameDetail> {
        lastAppId = appId
        lastName = name
        return result
    }
}
