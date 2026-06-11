package com.hitlist.detail.domain

import com.hitlist.common.domain.AppResult

class GetGameDetailUseCaseImpl(
    private val gameDetailRepository: GameDetailRepository
) : GetGameDetailUseCase {
    override suspend fun execute(appId: Int, name: String): AppResult<GameDetail> =
        gameDetailRepository.getGameDetail(appId, name)
}
