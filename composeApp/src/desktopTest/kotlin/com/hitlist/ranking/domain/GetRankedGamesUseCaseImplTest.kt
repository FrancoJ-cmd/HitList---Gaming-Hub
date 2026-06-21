package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetRankedGamesUseCaseImplTest {

    private fun givenGame(appId: Int = 1) = RankedGame(
        appId, "Game $appId", "", 0.0, 1000, 0.8, "Positive", 500, emptyList(), false
    )

    @Test
    fun `passes through the ranking emitted by the repository`() {
        // Arrange
        val games = listOf(givenGame(1), givenGame(2), givenGame(3))
        val repo = RankingRepositoryFake(
            rankedGamesResult = AppResult.Success(Stale(Ranking(games), isStale = false))
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.observe().first() }
        // Assert
        assertIs<AppResult.Success<Stale<Ranking>>>(result)
        assertEquals(listOf(1, 2, 3), result.data.value.games.map { it.steamAppId })
    }

    @Test
    fun `propagates the stale flag from the repository`() {
        // Arrange
        val repo = RankingRepositoryFake(
            rankedGamesResult = AppResult.Success(Stale(Ranking(listOf(givenGame())), isStale = true))
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.observe().first() }
        // Assert
        assertIs<AppResult.Success<Stale<Ranking>>>(result)
        assertTrue(result.data.isStale)
    }

    @Test
    fun `given empty ranking from data source, returns empty without exception`() {
        // Arrange
        val repo = RankingRepositoryFake(
            rankedGamesResult = AppResult.Success(Stale(Ranking(emptyList()), isStale = false))
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.observe().first() }
        // Assert
        assertIs<AppResult.Success<Stale<Ranking>>>(result)
        assertTrue(result.data.value.games.isEmpty())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
