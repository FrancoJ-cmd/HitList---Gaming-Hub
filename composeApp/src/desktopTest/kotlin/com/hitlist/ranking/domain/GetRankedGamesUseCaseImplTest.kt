package com.hitlist.ranking.domain

import com.hitlist.common.domain.AppResult
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
    fun `given games with identical score, original order is preserved`() {
        val repo = RankingRepositoryFake(
            rankedGamesResult = AppResult.Success(
                listOf(givenGame(1), givenGame(2), givenGame(3)) to false
            )
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.observe().first() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertEquals(listOf(1, 2, 3), result.data.first.map { it.steamAppId })
    }

    @Test
    fun `given empty list from data source, returns empty list without exception`() {
        val repo = RankingRepositoryFake(rankedGamesResult = AppResult.Success(emptyList<RankedGame>() to false))
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.observe().first() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.first.isEmpty())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
