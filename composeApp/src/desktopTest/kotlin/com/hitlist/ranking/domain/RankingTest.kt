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
        // mismo rating, distinto live player count -> el de más jugadores rankea primero
        val ranking = Ranking.from(
            listOf(
                entry(appId = 1, concurrentPlayers = 100),
                entry(appId = 2, concurrentPlayers = 100000)
            )
        )
        assertEquals(listOf(2, 1), ranking.games.map { it.steamAppId })
        assertTrue(ranking.games.first().score > ranking.games.last().score)
    }

    @Test
    fun `drops games below the minimum reviews threshold`() {
        val ranking = Ranking.from(
            listOf(
                entry(appId = 1, positive = 900, negative = 100),
                entry(appId = 2, positive = 10, negative = 5) // 15 reviews < 50
            )
        )
        assertEquals(listOf(1), ranking.games.map { it.steamAppId })
    }

    @Test
    fun `classifies the review score from the ratio`() {
        val ranking = Ranking.from(listOf(entry(appId = 1, positive = 960, negative = 40))) // 0.96, 1000 reviews
        assertEquals("Overwhelmingly Positive", ranking.games.single().reviewScoreDesc)
    }

    @Test
    fun `marks a game as trending when it climbs vs the previous ranking`() {
        val previous = Ranking.from(
            listOf(
                entry(appId = 1, concurrentPlayers = 100000),
                entry(appId = 2, concurrentPlayers = 100)
            )
        ) // orden previo: [1, 2]

        val current = Ranking.from(
            listOf(
                entry(appId = 1, concurrentPlayers = 100),
                entry(appId = 2, concurrentPlayers = 100000)
            ),
            previous = previous
        ) // orden nuevo: [2, 1], el 2 subió

        val game2 = current.games.first { it.steamAppId == 2 }
        val game1 = current.games.first { it.steamAppId == 1 }
        assertTrue(game2.isTrending)
        assertFalse(game1.isTrending)
    }
}
