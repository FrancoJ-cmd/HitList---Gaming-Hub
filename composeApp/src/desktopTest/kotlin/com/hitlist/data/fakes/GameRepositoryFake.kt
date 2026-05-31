package com.hitlist.data.fakes

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository

class GameRepositoryFake(
    private val rankedGamesResult: Result<Pair<List<RankedGame>, Boolean>> = Result.success(emptyList<RankedGame>() to false),
    private val gameDetailResult: Result<GameDetail> = Result.failure(Exception("Not set"))
) : GameRepository {

    override suspend fun getRankedGames(): Result<Pair<List<RankedGame>, Boolean>> = rankedGamesResult

    override suspend fun getGameDetail(appId: Int, name: String): Result<GameDetail> = gameDetailResult
}
