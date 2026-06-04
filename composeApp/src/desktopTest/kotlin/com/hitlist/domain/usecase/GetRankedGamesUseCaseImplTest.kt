package com.hitlist.domain.usecase

import com.hitlist.data.fakes.GameRepositoryFake
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
            rankedGamesResult = AppResult.Success(
                listOf(givenGame(1), givenGame(2), givenGame(3)) to false
            )
        )
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertEquals(listOf(1, 2, 3), result.data.first.map { it.steamAppId })
    }

    @Test
    fun `given empty list from SteamSpy, returns empty list without exception`() {
        val repo = GameRepositoryFake(rankedGamesResult = AppResult.Success(emptyList<RankedGame>() to false))
        val useCase = GetRankedGamesUseCaseImpl(repo)
        val result = runBlocking { useCase.execute() }
        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.first.isEmpty())
    }

    @Test
    fun `markTrending — game that moved up is marked as trending`() {
        val previous = listOf(givenGame(1), givenGame(2), givenGame(3))
        val current = listOf(givenGame(2), givenGame(1), givenGame(3))
        val result = GetRankedGamesUseCaseImpl.markTrending(current, previous)
        assertTrue(result[0].isTrending)
        assertFalse(result[1].isTrending)
        assertFalse(result[2].isTrending)
    }

    @Test
    fun `markTrending — new game not in previous list is not trending`() {
        val previous = listOf(givenGame(1))
        val current = listOf(givenGame(1), givenGame(99))
        val result = GetRankedGamesUseCaseImpl.markTrending(current, previous)
        assertFalse(result[1].isTrending)
    }

    @Test
    fun `describeReviewScore — overwhelmingly positive when ratio over 95 percent and 500 plus reviews`() {
        assertEquals("Overwhelmingly Positive", GetRankedGamesUseCaseImpl.describeReviewScore(1900, 100))
    }

    @Test
    fun `describeReviewScore — very positive when ratio over 80 percent`() {
        assertEquals("Very Positive", GetRankedGamesUseCaseImpl.describeReviewScore(800, 200))
    }

    @Test
    fun `describeReviewScore — mixed when ratio between 40 and 70 percent`() {
        assertEquals("Mixed", GetRankedGamesUseCaseImpl.describeReviewScore(500, 500))
    }

    @Test
    fun `describeReviewScore — empty string when fewer than 10 reviews`() {
        assertEquals("", GetRankedGamesUseCaseImpl.describeReviewScore(8, 1))
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
