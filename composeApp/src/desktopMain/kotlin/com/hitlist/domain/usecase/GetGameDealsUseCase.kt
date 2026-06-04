package com.hitlist.domain.usecase

import com.hitlist.domain.entity.Deal
import com.hitlist.domain.result.AppResult

interface GetGameDealsUseCase {
    suspend fun execute(gameName: String): AppResult<List<Deal>>
}
