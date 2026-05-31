package com.hitlist.data.repository

import com.hitlist.data.fakes.LocalDataSourceFake
import com.hitlist.data.local.CachePolicy
import com.hitlist.domain.entity.RankedGame
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import com.hitlist.data.remote.cheapshark.CheapSharkProxy
import com.hitlist.data.remote.steamspy.SteamSpyProxy
import com.hitlist.data.remote.steamspy.SteamSpyGameDto
import com.hitlist.data.remote.steamstore.SteamStoreProxy
import com.hitlist.data.remote.steamweb.SteamWebProxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRepositoryImplTest {

    private val steamSpy = mockk<SteamSpyProxy>()
    private val steamWeb = mockk<SteamWebProxy>()
    private val steamStore = mockk<SteamStoreProxy>()
    private val cheapShark = mockk<CheapSharkProxy>()

    private fun givenGame(appId: Int = 570, isTrending: Boolean = false) = RankedGame(
        appId, "Dota 2", "", 0.7, 400000, 0.8, "Very Positive", 2000000, emptyList(), isTrending
    )

    private fun givenRepo(local: LocalDataSourceFake) = GameRepositoryImpl(
        local, steamSpy, steamWeb, steamStore, cheapShark
    )

    @Test
    fun `given valid cache, returns cached data without remote calls`() {
        val local = LocalDataSourceFake()
        val cached = listOf(givenGame())
        local.seedRankedGames(cached, System.currentTimeMillis())
        val repo = givenRepo(local)

        val result = runBlocking { repo.getRankedGames() }

        assertTrue(result.isSuccess)
        assertEquals(cached, result.getOrThrow().first)
        assertFalse(result.getOrThrow().second)
        coVerify(exactly = 0) { steamSpy.getTop100Games() }
    }

    @Test
    fun `given expired cache and no network, returns stale data`() {
        val local = LocalDataSourceFake()
        val staleGames = listOf(givenGame())
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(staleGames, expiredAt)

        coEvery { steamSpy.getTop100Games() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.getRankedGames() }

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().second) // isStale = true
        assertEquals(staleGames, result.getOrThrow().first)
    }

    @Test
    fun `given no cache and no network, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { steamSpy.getTop100Games() } throws Exception("No network")
        val repo = givenRepo(local)

        val result = runBlocking { repo.getRankedGames() }

        assertTrue(result.isFailure)
    }

    @Test
    fun `given expired cache and network available, fetches fresh data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.LIVE_PLAYERS_TTL_MS - 1000
        local.seedRankedGames(listOf(givenGame()), expiredAt)

        coEvery { steamSpy.getTop100Games() } returns listOf(SteamSpyGameDto(570, "Dota 2"))
        coEvery { steamWeb.getCurrentPlayers(570) } returns 400000
        coEvery { steamStore.getAppReviews(570) } returns null

        val repo = givenRepo(local)
        val result = runBlocking { repo.getRankedGames() }

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().second) // isStale = false
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
