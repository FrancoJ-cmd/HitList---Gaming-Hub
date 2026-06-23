package com.hitlist.ranking.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RankingTest {

    private fun entry(
        appId: Int,
        concurrentPlayers: Int = 1000,
        positive: Int = 900,
        negative: Int = 100,
        genres: List<String> = listOf("Action")
    ) = CombinedRankingEntry(
        appId = appId,
        name = "Game $appId",
        headerImageUrl = "",
        concurrentPlayers = concurrentPlayers,
        positiveReviews = positive,
        negativeReviews = negative,
        genres = genres
    )

    @Test
    fun `orders games by score descending, live players dominate`() {
        // Arrange
        val games = listOf(
            entry(appId = 1, concurrentPlayers = 100),
            entry(appId = 2, concurrentPlayers = 100000)
        )
        // Act
        val ranking = Ranking.from(games)
        // Assert
        assertEquals(listOf(2, 1), ranking.games.map { it.steamAppId })
        assertTrue(ranking.games.first().score > ranking.games.last().score)
    }

    @Test
    fun `drops games below the minimum reviews threshold`() {
        // Arrange
        val games = listOf(
            entry(appId = 1, positive = 900, negative = 100),
            entry(appId = 2, positive = 10, negative = 5)
        )
        // Act
        val ranking = Ranking.from(games)
        // Assert
        assertEquals(listOf(1), ranking.games.map { it.steamAppId })
    }

    @Test
    fun `classifies the review score from the ratio`() {
        // Act
        val ranking = Ranking.from(listOf(entry(appId = 1, positive = 960, negative = 40)))
        // Assert
        assertEquals("Overwhelmingly Positive", ranking.games.single().reviewScoreDesc)
    }

    @Test
    fun `classifies every review score band from the ratio`() {
        // Arrange
        fun describe(positive: Int, negative: Int) =
            Ranking.from(listOf(entry(appId = 1, positive = positive, negative = negative)))
                .games.single().reviewScoreDesc

        // Act & Assert
        assertEquals("Very Positive", describe(positive = 850, negative = 150))
        assertEquals("Mostly Positive", describe(positive = 750, negative = 250))
        assertEquals("Mixed", describe(positive = 500, negative = 500))
        assertEquals("Mostly Negative", describe(positive = 300, negative = 700))
        assertEquals("Overwhelmingly Negative", describe(positive = 100, negative = 900))
    }

    @Test
    fun `high ratio but few reviews is not overwhelmingly positive`() {
        // Act
        val ranking = Ranking.from(listOf(entry(appId = 1, positive = 96, negative = 4)))
        // Assert
        assertEquals("Very Positive", ranking.games.single().reviewScoreDesc)
    }

    @Test
    fun `scores games even when nobody is playing in the dataset`() {
        // Act
        val ranking = Ranking.from(listOf(entry(appId = 1, concurrentPlayers = 0)))
        // Assert
        assertEquals(1, ranking.games.size)
        assertTrue(ranking.games.single().score > 0.0)
    }

    @Test
    fun `marks a game as trending when it climbs vs the previous ranking`() {
        // Arrange
        val previous = Ranking.from(
            listOf(
                entry(appId = 1, concurrentPlayers = 100000),
                entry(appId = 2, concurrentPlayers = 100)
            )
        )

        // Act
        val current = Ranking.from(
            listOf(
                entry(appId = 1, concurrentPlayers = 100),
                entry(appId = 2, concurrentPlayers = 100000)
            ),
            previous = previous
        )

        // Assert
        val game2 = current.games.first { it.steamAppId == 2 }
        val game1 = current.games.first { it.steamAppId == 1 }
        assertTrue(game2.isTrending)
        assertFalse(game1.isTrending)
    }
}
