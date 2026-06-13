package com.hitlist.data.repository

import com.hitlist.data.fakes.LocalDataSourceFake
import com.hitlist.data.local.CachePolicy
import com.hitlist.data.remote.GameNewsSource
import com.hitlist.data.remote.GeneralNewsSource
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.result.AppResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NewsRepositoryImplTest {

    private val generalNewsSource = mockk<GeneralNewsSource>()
    private val gameNewsSource    = mockk<GameNewsSource>()

    private fun givenArticle(title: String = "Article") =
        NewsArticle(title, null, "Source", "https://url.com", null, "2024-01-01")

    private fun givenRepo(local: LocalDataSourceFake) =
        NewsRepositoryImpl(local, generalNewsSource, gameNewsSource)

    // --- getNews (general query) ---

    @Test
    fun `given valid cache for query, returns cached articles without remote call`() {
        val local = LocalDataSourceFake()
        val cached = listOf(givenArticle("Cached"))
        local.saveNews("gaming", cached)

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNews("gaming") }

        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(cached, result.data)
        coVerify(exactly = 0) { generalNewsSource.getNews(any()) }
    }

    @Test
    fun `given expired cache and remote returns articles, returns fresh data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1_000
        local.seedNews("gaming", listOf(givenArticle("Old")), expiredAt)

        val fresh = listOf(givenArticle("Fresh"))
        coEvery { generalNewsSource.getNews("gaming") } returns fresh

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNews("gaming") }

        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(fresh, result.data)
    }

    @Test
    fun `given expired cache and remote throws, returns stale cached data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1_000
        val stale = listOf(givenArticle("Stale"))
        local.seedNews("gaming", stale, expiredAt)

        coEvery { generalNewsSource.getNews(any()) } throws Exception("Timeout")

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNews("gaming") }

        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(stale, result.data)
    }

    @Test
    fun `given no cache and remote throws, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { generalNewsSource.getNews(any()) } throws Exception("No network")

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNews("gaming") }

        assertIs<AppResult.Failure>(result)
    }

    // --- getNewsForGame (Steam News por appId) ---

    @Test
    fun `given valid cache for appId, returns cached articles without calling gameNewsSource`() {
        val local = LocalDataSourceFake()
        val cached = listOf(givenArticle("Steam News Cached"))
        local.saveNews("steam_570", cached)

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNewsForGame(570) }

        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(cached, result.data)
        coVerify(exactly = 0) { gameNewsSource.getNewsForGame(any()) }
    }

    @Test
    fun `given expired cache for appId and remote returns articles, returns fresh data`() {
        val local = LocalDataSourceFake()
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1_000
        local.seedNews("steam_570", listOf(givenArticle("Old Steam")), expiredAt)

        val fresh = listOf(givenArticle("Fresh Steam"))
        coEvery { gameNewsSource.getNewsForGame(570) } returns fresh

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNewsForGame(570) }

        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(fresh, result.data)
    }

    @Test
    fun `given no cache for appId and remote throws, returns failure`() {
        val local = LocalDataSourceFake()
        coEvery { gameNewsSource.getNewsForGame(any()) } throws Exception("No network")

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNewsForGame(9999) }

        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given getNewsForGame returns empty list, does not save to cache and returns success`() {
        val local = LocalDataSourceFake()
        coEvery { gameNewsSource.getNewsForGame(570) } returns emptyList()

        val repo = givenRepo(local)
        val result = runBlocking { repo.getNewsForGame(570) }

        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(emptyList(), result.data)
        // Verificar que no se guardó nada (cache key vacía)
        assertEquals(null, local.getNews("steam_570"))
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }

