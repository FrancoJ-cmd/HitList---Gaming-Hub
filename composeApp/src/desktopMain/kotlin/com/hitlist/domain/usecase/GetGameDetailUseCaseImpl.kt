package com.hitlist.domain.usecase

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.repository.GameRepository

class GetGameDetailUseCaseImpl(
    private val gameRepository: GameRepository
) : GetGameDetailUseCase {

    override suspend fun execute(appId: Int, name: String): Result<GameDetail> =
        gameRepository.getGameDetail(appId, name)
}
