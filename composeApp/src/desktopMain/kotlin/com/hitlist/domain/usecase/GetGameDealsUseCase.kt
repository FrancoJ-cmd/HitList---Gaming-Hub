package com.hitlist.domain.usecase

import com.hitlist.domain.entity.Deal

interface GetGameDealsUseCase {
    suspend fun execute(gameName: String): List<Deal>
}
