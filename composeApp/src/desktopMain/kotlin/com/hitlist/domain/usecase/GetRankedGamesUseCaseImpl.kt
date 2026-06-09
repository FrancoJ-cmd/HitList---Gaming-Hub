package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult
import kotlinx.coroutines.flow.Flow

class GetRankedGamesUseCaseImpl(
    private val gameRepository: GameRepository
) : GetRankedGamesUseCase {

    override fun observe(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>> =
        gameRepository.observeRankedGames()
}
