package com.hitlist.data.repository

import com.hitlist.data.fakes.LocalDataSourceFake
import com.hitlist.data.local.CachePolicy
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.data.remote.GameMetadataSeed
import com.hitlist.data.remote.GameMetadataSource
import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.LiveRankEntry
import com.hitlist.data.remote.LiveRanking
import com.hitlist.data.remote.LiveRankingSource
import com.hitlist.data.remote.PlayerCountSource
import com.hitlist.data.remote.RankingMetadataSource
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRepositoryImplTest {

    private val liveRankingSource = mockk<LiveRankingSource>()
    private val rankingMetadataSource = mockk<RankingMetadataSource>()
    private val playerCountSource = mockk<PlayerCountSource>()
    private val metadataSource = mockk<GameMetadataSource>()
    private val reviewSource = mockk<GameReviewSource>()
    private val dealsSource = mockk<GameDealsSource>()

    private fun givenGame(appId: Int = 570) = RankedGame(
        appId, "Dota 2", "", 0.7, 400000, 0.8, "Very Positive", 2000000, emptyList(), false
    )

    private fun givenRepo(local: LocalDataSourceFake) = GameRepositoryImpl(
        local, liveRankingSource, rankingMetadataSource, playerCountSource, metadataSource, reviewSource, dealsSource
    )

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
        coVerify(exactly = 0) { liveRankingSource.getLiveRanking() }
    }

    @Test
    fun `given expired cache and no network, returns stale data`() {
        val local = LocalDataSourceFake()
        val staleGames = listOf(givenGame())
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(staleGames, expiredAt)

        coEvery { liveRankingSource.getLiveRanking() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertTrue(result.data.second)
        assertEquals(staleGames, result.data.first)
    }

    @Test
    fun `given no cache and no network, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { liveRankingSource.getLiveRanking() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given expired cache and network available, fetches fresh data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(listOf(givenGame()), expiredAt)

        coEvery { liveRankingSource.getLiveRanking() } returns LiveRanking(
            entries = listOf(LiveRankEntry(appId = 570, concurrentPlayers = 400000, rank = 1)),
            lastUpdate = System.currentTimeMillis() / 1000
        )
        coEvery { rankingMetadataSource.getBulkMetadata() } returns mapOf(
            570 to GameMetadataSeed(570, "Dota 2", positiveReviews = 1800000, negativeReviews = 200000, genres = listOf("Action"))
        )

        val repo = givenRepo(local)
        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertFalse(result.data.second)
        assertEquals(570, result.data.first.single().steamAppId)
    }

    @Test
    fun `given appId missing from bulk metadata, fetches it via per-game fallback`() {
        val local = LocalDataSourceFake()
        coEvery { liveRankingSource.getLiveRanking() } returns LiveRanking(
            entries = listOf(LiveRankEntry(appId = 999, concurrentPlayers = 50000, rank = 1)),
            lastUpdate = System.currentTimeMillis() / 1000
        )
        coEvery { rankingMetadataSource.getBulkMetadata() } returns emptyMap()
        coEvery { rankingMetadataSource.getMetadata(999) } returns
            GameMetadataSeed(999, "New Hit", positiveReviews = 9000, negativeReviews = 1000, genres = listOf("RPG"))

        val repo = givenRepo(local)
        val result = runBlocking { repo.observeRankedGames().first() }

        assertIs<AppResult.Success<Pair<List<RankedGame>, Boolean>>>(result)
        assertEquals("New Hit", result.data.first.single().name)
        coVerify(exactly = 1) { rankingMetadataSource.getMetadata(999) }
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
