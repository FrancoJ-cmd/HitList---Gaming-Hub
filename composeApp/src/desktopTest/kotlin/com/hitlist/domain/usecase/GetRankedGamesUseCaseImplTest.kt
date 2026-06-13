package com.hitlist.domain.usecase

import com.hitlist.data.fakes.GameRepositoryFake
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.error.AppError
import com.hitlist.domain.result.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetRankedGamesUseCaseImplTest {

    private fun givenGame(appId: Int = 1) = RankedGame(
        appId, "Game $appId", "", 0.7, 1000, 0.8, "Positive", 500, emptyList(), false
    )

    @Test
    fun `given repository returns success, use case propagates result`() {
        val games = listOf(givenGame(1), givenGame(2))
        val repo = GameRepositoryFake(rankedGamesResult = AppResult.Success(games to false))
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertEquals(games, result.data.first)
        assertFalse(result.data.second)
    }

    @Test
    fun `given repository returns stale flag, use case propagates stale flag`() {
        val games = listOf(givenGame(1))
        val repo = GameRepositoryFake(rankedGamesResult = AppResult.Success(games to true))
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.second)
    }

    @Test
    fun `given repository returns failure, use case propagates failure`() {
        val repo = GameRepositoryFake(
            rankedGamesResult = AppResult.Failure(AppError.Network.NoConnection)
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given repository returns empty list, use case returns empty list`() {
        val repo = GameRepositoryFake(rankedGamesResult = AppResult.Success(emptyList<RankedGame>() to false))
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.first.isEmpty())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
