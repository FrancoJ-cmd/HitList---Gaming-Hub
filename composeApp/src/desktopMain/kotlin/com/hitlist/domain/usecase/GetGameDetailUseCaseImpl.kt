package com.hitlist.domain.usecase

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult

class GetGameDetailUseCaseImpl(
    private val gameRepository: GameRepository
) : GetGameDetailUseCase {

    override suspend fun execute(appId: Int, name: String): AppResult<GameDetail> =
        gameRepository.getGameDetail(appId, name)
}
