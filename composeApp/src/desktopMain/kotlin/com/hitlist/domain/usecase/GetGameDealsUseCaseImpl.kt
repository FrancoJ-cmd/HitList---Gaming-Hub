package com.hitlist.domain.usecase

import com.hitlist.domain.entity.Deal
import com.hitlist.domain.repository.DealsRepository
import com.hitlist.domain.result.AppResult

class GetGameDealsUseCaseImpl(
    private val dealsRepository: DealsRepository
) : GetGameDealsUseCase {

    override suspend fun execute(gameName: String): AppResult<List<Deal>> =
        dealsRepository.getDeals(gameName)
}
