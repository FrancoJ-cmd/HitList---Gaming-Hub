package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.ranking.domain.CombinedRanking
import com.hitlist.ranking.domain.CombinedRankingEntry
import com.hitlist.ranking.domain.RankedGame
import com.hitlist.ranking.domain.Ranking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RankingRepositoryImplTest {

    private val rankingSource = mockk<CombinedRankingSource>()

    private fun givenGame(appId: Int = 570) = RankedGame(
        appId, "Dota 2", "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg",
        0.7, 400000, 0.8, "Very Positive", 2000000, emptyList(), false
    )

    private fun givenRepo(local: RankingCacheFake) = RankingRepositoryImpl(local, rankingSource)

    @Test
    fun `given valid cache, returns cached data without remote calls`() {
        val local = RankingCacheFake()
        val cached = listOf(givenGame())
        local.seedRankedGames(cached, System.currentTimeMillis())
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Stale<Ranking>>>(result)
        assertEquals(cached, result.data.value.games)
        assertFalse(result.data.isStale)
        coVerify(exactly = 0) { rankingSource.getCombinedRanking() }
    }

    @Test
    fun `given expired cache and no network, returns stale data`() {
        val local = RankingCacheFake()
        val staleGames = listOf(givenGame())
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(staleGames, expiredAt)

        coEvery { rankingSource.getCombinedRanking() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Stale<Ranking>>>(result)
        assertTrue(result.data.isStale)
        assertEquals(staleGames, result.data.value.games)
    }

    @Test
    fun `given no cache and no network, returns failure`() {
        val local = RankingCacheFake()
        coEvery { rankingSource.getCombinedRanking() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given expired cache and network available, fetches fresh data`() {
        val local = RankingCacheFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(listOf(givenGame()), expiredAt)

        coEvery { rankingSource.getCombinedRanking() } returns CombinedRanking(
            entries = listOf(
                CombinedRankingEntry(
                    appId = 570, name = "Dota 2",
                    headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/570/header.jpg",
                    concurrentPlayers = 400000,
                    positiveReviews = 1800000, negativeReviews = 200000, genres = listOf("Action")
                )
            ),
            lastUpdate = System.currentTimeMillis() / 1000
        )

        val repo = givenRepo(local)
        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Stale<Ranking>>>(result)
        assertFalse(result.data.isStale)
        assertEquals(570, result.data.value.games.single().steamAppId)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
