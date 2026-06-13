package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult

class GetRankedGamesUseCaseImpl(
    private val gameRepository: GameRepository
) : GetRankedGamesUseCase {

    override suspend fun execute(): AppResult<Pair<List<RankedGame>, Boolean>> =
        gameRepository.getRankedGames()
}
