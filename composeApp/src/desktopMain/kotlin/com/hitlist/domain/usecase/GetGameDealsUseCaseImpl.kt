package com.hitlist.domain.usecase

import com.hitlist.domain.entity.Deal
import com.hitlist.domain.repository.DealsRepository

class GetGameDealsUseCaseImpl(
    private val dealsRepository: DealsRepository
) : GetGameDealsUseCase {

    override suspend fun execute(gameName: String): List<Deal> =
        dealsRepository.getDeals(gameName)
}
