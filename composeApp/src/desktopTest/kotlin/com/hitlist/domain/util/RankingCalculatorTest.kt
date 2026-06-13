package com.hitlist.domain.util

import com.hitlist.domain.entity.RankedGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RankingCalculatorTest {

    private fun givenGame(appId: Int, isTrending: Boolean = false) = RankedGame(
        steamAppId = appId,
        name = "Game $appId",
        headerImageUrl = "",
        score = 0.0,
        currentPlayers = 1000,
        positiveRatio = 0.8,
        reviewScoreDesc = "Positive",
        totalReviews = 500,
        genres = emptyList(),
        isTrending = isTrending
    )

    // --- calculateScore ---

    @Test
    fun `given enough reviews, score equals weighted sum of trend and rating`() {
        val score = RankingCalculator.calculateScore(
            currentPlayers = 500,
            maxPlayersInDataset = 1000,
            positiveRatio = 0.8,
            totalReviews = 500
        )
        val expected = 0.6 * (500.0 / 1000.0) + 0.4 * 0.8
        assertEquals(expected, score, 0.0001)
    }

    @Test
    fun `given fewer than minimum reviews, score is zero`() {
        val score = RankingCalculator.calculateScore(
            currentPlayers = 10_000,
            maxPlayersInDataset = 10_000,
            positiveRatio = 1.0,
            totalReviews = RankingCalculator.MIN_REVIEWS_THRESHOLD - 1
        )
        assertEquals(0.0, score)
    }

    @Test
    fun `given exactly minimum reviews, score is calculated normally`() {
        val score = RankingCalculator.calculateScore(
            currentPlayers = 1000,
            maxPlayersInDataset = 1000,
            positiveRatio = 0.8,
            totalReviews = RankingCalculator.MIN_REVIEWS_THRESHOLD
        )
        val expected = 0.6 * 1.0 + 0.4 * 0.8
        assertEquals(expected, score, 0.0001)
    }

    @Test
    fun `given maxPlayersInDataset is zero, no division by zero and trend score is zero`() {
        val score = RankingCalculator.calculateScore(
            currentPlayers = 0,
            maxPlayersInDataset = 0,
            positiveRatio = 0.9,
            totalReviews = 500
        )
        assertEquals(0.4 * 0.9, score, 0.0001)
    }

    @Test
    fun `given positiveRatio is zero, score is only trend-based`() {
        val score = RankingCalculator.calculateScore(
            currentPlayers = 1000,
            maxPlayersInDataset = 1000,
            positiveRatio = 0.0,
            totalReviews = 500
        )
        assertEquals(0.6 * 1.0, score, 0.0001)
    }

    // --- markTrending ---

    @Test
    fun `given game moved up in ranking, it is marked as trending`() {
        val previous = listOf(givenGame(1), givenGame(2), givenGame(3))
        val current  = listOf(givenGame(2), givenGame(1), givenGame(3))
        val result = RankingCalculator.markTrending(current, previous)
        assertTrue(result[0].isTrending)  // game 2 subió de posición 1 a posición 0
        assertFalse(result[1].isTrending) // game 1 bajó
        assertFalse(result[2].isTrending) // game 3 igual
    }

    @Test
    fun `given game not present in previous list, it is not marked as trending`() {
        val previous = listOf(givenGame(1))
        val current  = listOf(givenGame(1), givenGame(99))
        val result = RankingCalculator.markTrending(current, previous)
        assertFalse(result[1].isTrending) // game 99 es nuevo, no trending
    }

    @Test
    fun `given empty previous list, no game is marked as trending`() {
        val current = listOf(givenGame(1), givenGame(2))
        val result = RankingCalculator.markTrending(current, emptyList())
        result.forEach { assertFalse(it.isTrending) }
    }

    @Test
    fun `given game that stayed in same position, it is not trending`() {
        val previous = listOf(givenGame(1), givenGame(2))
        val current  = listOf(givenGame(1), givenGame(2))
        val result = RankingCalculator.markTrending(current, previous)
        assertFalse(result[0].isTrending)
        assertFalse(result[1].isTrending)
    }

    // --- describeReviewScore ---

    @Test
    fun `given ratio over 95 percent with 500 or more reviews, returns overwhelmingly positive`() {
        assertEquals("Overwhelmingly Positive", RankingCalculator.describeReviewScore(1900, 100))
    }

    @Test
    fun `given ratio over 95 percent but fewer than 500 reviews, does not return overwhelmingly positive`() {
        val result = RankingCalculator.describeReviewScore(19, 1)
        assertEquals("Very Positive", result)
    }

    @Test
    fun `given ratio over 80 percent, returns very positive`() {
        assertEquals("Very Positive", RankingCalculator.describeReviewScore(800, 200))
    }

    @Test
    fun `given ratio over 70 percent, returns mostly positive`() {
        assertEquals("Mostly Positive", RankingCalculator.describeReviewScore(750, 250))
    }

    @Test
    fun `given ratio between 40 and 70 percent, returns mixed`() {
        assertEquals("Mixed", RankingCalculator.describeReviewScore(500, 500))
    }

    @Test
    fun `given ratio below 20 percent, returns overwhelmingly negative`() {
        assertEquals("Overwhelmingly Negative", RankingCalculator.describeReviewScore(1, 9))
    }

    @Test
    fun `given fewer than 10 total reviews, returns empty string`() {
        assertEquals("", RankingCalculator.describeReviewScore(8, 1))
    }
}

