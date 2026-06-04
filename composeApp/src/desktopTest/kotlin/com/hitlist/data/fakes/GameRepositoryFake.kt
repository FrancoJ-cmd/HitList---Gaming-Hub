package com.hitlist.data.fakes

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.error.AppError
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult

class GameRepositoryFake(
    private val rankedGamesResult: AppResult<Pair<List<RankedGame>, Boolean>> = AppResult.Success(emptyList<RankedGame>() to false),
    private val gameDetailResult: AppResult<GameDetail> = AppResult.Failure(AppError.Unexpected())
) : GameRepository {

    override suspend fun getRankedGames(): AppResult<Pair<List<RankedGame>, Boolean>> = rankedGamesResult

    override suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail> = gameDetailResult
}
