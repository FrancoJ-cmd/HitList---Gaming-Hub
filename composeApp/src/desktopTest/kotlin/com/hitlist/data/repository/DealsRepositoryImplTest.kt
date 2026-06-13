package com.hitlist.data.repository

import com.hitlist.data.fakes.LocalDataSourceFake
import com.hitlist.data.local.CachePolicy
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DealsRepositoryImplTest {

    private val dealsSource = mockk<GameDealsSource>()

    private fun givenDeal(store: String = "Steam") =
        Deal(store, "5.99", "29.99", 80.0, "4.99")

    private fun givenRepo(local: LocalDataSourceFake) =
        DealsRepositoryImpl(local, dealsSource)

    @Test
    fun `given valid cache, returns cached deals without calling remote`() {
        val local = LocalDataSourceFake()
        val cached = listOf(givenDeal())
        local.saveDeals("Dota 2", cached)

        val repo = givenRepo(local)
        val result = runBlocking { repo.getDeals("Dota 2") }

        assertIs<AppResult.Success<List<Deal>>>(result)
        assertEquals(cached, result.data)
        coVerify(exactly = 0) { dealsSource.getDeals(any()) }
    }

    @Test
    fun `given expired cache and remote returns data, returns fresh data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.DEALS_TTL_MS - 1_000
        local.seedDeals("Dota 2", listOf(givenDeal("GOG")), expiredAt)

        val freshDeals = listOf(givenDeal("Steam"))
        coEvery { dealsSource.getDeals("Dota 2") } returns freshDeals

        val repo = givenRepo(local)
        val result = runBlocking { repo.getDeals("Dota 2") }

        assertIs<AppResult.Success<List<Deal>>>(result)
        assertEquals(freshDeals, result.data)
    }

    @Test
    fun `given expired cache and remote throws, returns stale cached data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.DEALS_TTL_MS - 1_000
        val stale = listOf(givenDeal("Humble Store"))
        local.seedDeals("Dota 2", stale, expiredAt)

        coEvery { dealsSource.getDeals(any()) } throws Exception("Network error")

        val repo = givenRepo(local)
        val result = runBlocking { repo.getDeals("Dota 2") }

        assertIs<AppResult.Success<List<Deal>>>(result)
        assertEquals(stale, result.data)
    }

    @Test
    fun `given no cache and remote throws, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { dealsSource.getDeals(any()) } throws Exception("Network error")

        val repo = givenRepo(local)
        val result = runBlocking { repo.getDeals("Unknown Game") }

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given no cache and remote returns empty list, returns success with empty list`() {
        val local = LocalDataSourceFake()
        coEvery { dealsSource.getDeals(any()) } returns emptyList()

        val repo = givenRepo(local)
        val result = runBlocking { repo.getDeals("Obscure Game") }

        assertIs<AppResult.Success<List<Deal>>>(result)
        assertEquals(emptyList(), result.data)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }

