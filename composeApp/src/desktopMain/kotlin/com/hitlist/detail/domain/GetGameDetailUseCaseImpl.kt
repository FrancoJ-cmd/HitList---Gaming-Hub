package com.hitlist.detail.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class GetGameDetailUseCaseImpl(
    private val gameDetailRepository: GameDetailRepository
) : GetGameDetailUseCase {
    override suspend fun execute(appId: Int, name: String): AppResult<Stale<GameDetail>> =
        gameDetailRepository.getGameDetail(appId, name)
}
