package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.domain.AppResult
import com.hitlist.common.data.LocalDataSourceFake
import com.hitlist.ranking.domain.RankedGame
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

    private fun givenRepo(local: LocalDataSourceFake) = RankingRepositoryImpl(local, rankingSource)

    @Test
    fun `given valid cache, returns cached data without remote calls`() {
        val local = LocalDataSourceFake()
        val cached = listOf(givenGame())
        local.seedRankedGames(cached, System.currentTimeMillis())
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertEquals(cached, result.data.first)
        assertFalse(result.data.second)
        coVerify(exactly = 0) { rankingSource.getCombinedRanking() }
    }

    @Test
    fun `given expired cache and no network, returns stale data`() {
        val local = LocalDataSourceFake()
        val staleGames = listOf(givenGame())
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(staleGames, expiredAt)

        coEvery { rankingSource.getCombinedRanking() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.second)
        assertEquals(staleGames, result.data.first)
    }

    @Test
    fun `given no cache and no network, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { rankingSource.getCombinedRanking() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given expired cache and network available, fetches fresh data`() {
        val local = LocalDataSourceFake()
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

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertFalse(result.data.second)
        assertEquals(570, result.data.first.single().steamAppId)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
