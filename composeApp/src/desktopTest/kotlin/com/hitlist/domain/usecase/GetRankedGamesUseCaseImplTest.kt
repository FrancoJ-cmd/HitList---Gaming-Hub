package com.hitlist.domain.usecase

import com.hitlist.data.fakes.GameRepositoryFake
import com.hitlist.domain.entity.RankedGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetRankedGamesUseCaseImplTest {

    private fun givenGame(
        appId: Int = 1,
        players: Int = 1000,
        positiveRatio: Double = 0.8,
        totalReviews: Int = 500,
        isTrending: Boolean = false
    ) = RankedGame(appId, "Game $appId", "", 0.0, players, positiveRatio, "Positive", totalReviews, emptyList(), isTrending)

    @Test
    fun `given enough reviews, score is calculated correctly`() {
        val score = GetRankedGamesUseCaseImpl.calculateScore(
            currentPlayers = 500,
            maxPlayersInDataset = 1000,
            positiveRatio = 0.8,
            totalReviews = 500
        )
        val expected = 0.6 * (500.0 / 1000.0) + 0.4 * 0.8
        assertEquals(expected, score, 0.0001)
    }

    @Test
    fun `given less than minimum reviews, score is zero`() {
        val score = GetRankedGamesUseCaseImpl.calculateScore(
            currentPlayers = 10000,
            maxPlayersInDataset = 10000,
            positiveRatio = 1.0,
            totalReviews = 49
        )
        assertEquals(0.0, score)
    }

    @Test
    fun `given maxPlayers is zero, trendScore is zero without division by zero`() {
        val score = GetRankedGamesUseCaseImpl.calculateScore(
            currentPlayers = 0,
            maxPlayersInDataset = 0,
            positiveRatio = 0.9,
            totalReviews = 500
        )
        assertEquals(0.4 * 0.9, score, 0.0001)
    }

    @Test
    fun `given positiveRatio is zero, score is only trend-based`() {
        val score = GetRankedGamesUseCaseImpl.calculateScore(
            currentPlayers = 1000,
            maxPlayersInDataset = 1000,
            positiveRatio = 0.0,
            totalReviews = 500
        )
        assertEquals(0.6 * 1.0, score, 0.0001)
    }

    @Test
    fun `given games with identical score, original order is preserved`() {
        val repo = GameRepositoryFake(
            rankedGamesResult = Result.success(
                listOf(givenGame(1), givenGame(2), givenGame(3)) to false
            )
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        val games = result.getOrThrow().first
        assertEquals(listOf(1, 2, 3), games.map { it.steamAppId })
    }

    @Test
    fun `given empty list from SteamSpy, returns empty list without exception`() {
        val repo = GameRepositoryFake(rankedGamesResult = Result.success(emptyList<RankedGame>() to false))
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().first.isEmpty())
    }

    @Test
    fun `markTrending — game that moved up is marked as trending`() {
        val previous = listOf(givenGame(1), givenGame(2), givenGame(3))
        val current = listOf(givenGame(2), givenGame(1), givenGame(3))
        val result = GetRankedGamesUseCaseImpl.markTrending(current, previous)
        assertTrue(result[0].isTrending)  // game 2 moved up (was 1, now 0)
        assertFalse(result[1].isTrending) // game 1 moved down
        assertFalse(result[2].isTrending) // game 3 stayed
    }

    @Test
    fun `markTrending — new game not in previous list is not trending`() {
        val previous = listOf(givenGame(1))
        val current = listOf(givenGame(1), givenGame(99))
        val result = GetRankedGamesUseCaseImpl.markTrending(current, previous)
        assertFalse(result[1].isTrending)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
