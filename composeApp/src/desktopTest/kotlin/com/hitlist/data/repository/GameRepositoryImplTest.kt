package com.hitlist.data.repository

import com.hitlist.data.fakes.LocalDataSourceFake
import com.hitlist.data.local.CachePolicy
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.data.remote.GameMetadataSource
import com.hitlist.data.remote.GameRankingSource
import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.GameSeed
import com.hitlist.data.remote.PlayerCountSource
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRepositoryImplTest {

    private val rankingSource = mockk<GameRankingSource>()
    private val playerCountSource = mockk<PlayerCountSource>()
    private val metadataSource = mockk<GameMetadataSource>()
    private val reviewSource = mockk<GameReviewSource>()
    private val dealsSource = mockk<GameDealsSource>()

    private fun givenGame(appId: Int = 570) = RankedGame(
        appId, "Dota 2", "", 0.7, 400000, 0.8, "Very Positive", 2000000, emptyList(), false
    )

    private fun givenRepo(local: LocalDataSourceFake) = GameRepositoryImpl(
        local, rankingSource, playerCountSource, metadataSource, reviewSource, dealsSource
    )

    @Test
    fun `given valid cache, returns cached data without remote calls`() {
        val local = LocalDataSourceFake()
        val cached = listOf(givenGame())
        local.seedRankedGames(cached, System.currentTimeMillis())
        val repo = givenRepo(local)

        val result = runBlocking { repo.getRankedGames() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertEquals(cached, result.data.first)
        assertFalse(result.data.second)
        coVerify(exactly = 0) { rankingSource.getTopGames() }
    }

    @Test
    fun `given expired cache and no network, returns stale data`() {
        val local = LocalDataSourceFake()
        val staleGames = listOf(givenGame())
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(staleGames, expiredAt)

        coEvery { rankingSource.getTopGames() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.getRankedGames() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.second)
        assertEquals(staleGames, result.data.first)
    }

    @Test
    fun `given no cache and no network, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { rankingSource.getTopGames() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.getRankedGames() }

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given expired cache and network available, fetches fresh data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(listOf(givenGame()), expiredAt)

        coEvery { rankingSource.getTopGames() } returns listOf(
            GameSeed(570, "Dota 2", currentPlayers = 400000, positiveReviews = 1800000, negativeReviews = 200000, genres = listOf("Action"))
        )

        val repo = givenRepo(local)
        val result = runBlocking { repo.getRankedGames() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertFalse(result.data.second)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
