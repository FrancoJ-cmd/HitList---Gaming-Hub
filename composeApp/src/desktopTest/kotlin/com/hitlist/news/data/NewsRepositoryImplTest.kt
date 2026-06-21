package com.hitlist.news.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.news.domain.NewsArticle
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NewsRepositoryImplTest {

    private val cacheSource = mockk<NewsCacheSource>(relaxed = true)
    private val generalNewsSource = mockk<GeneralNewsSource>()
    private val gameNewsSource = mockk<GameNewsSource>()

    private fun repo() = NewsRepositoryImpl(cacheSource, generalNewsSource, gameNewsSource)

    private fun givenArticle(title: String = "Patch notes") = NewsArticle(
        title = title, description = "desc", sourceName = "Steam",
        url = "https://example.com", imageUrl = null, publishedAt = "2026-06-20"
    )

    @Test
    fun `given valid cache, returns cached general news without fetching`() {
        // Arrange
        val cached = listOf(givenArticle())
        every { cacheSource.getNews("dota") } returns (cached to System.currentTimeMillis())

        // Act
        val result = runBlocking { repo().getNews("dota") }

        // Assert
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertFalse(result.data.isStale)
        assertEquals(cached, result.data.value)
    }

    @Test
    fun `given expired cache, fetches fresh general news and caches it`() {
        // Arrange
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1000
        every { cacheSource.getNews("dota") } returns (listOf(givenArticle("old")) to expiredAt)
        every { cacheSource.saveNews(any(), any()) } just Runs
        val fresh = listOf(givenArticle("fresh"))
        coEvery { generalNewsSource.getNews("dota") } returns fresh

        // Act
        val result = runBlocking { repo().getNews("dota") }

        // Assert
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertFalse(result.data.isStale)
        assertEquals(fresh, result.data.value)
        verify { cacheSource.saveNews("dota", fresh) }
    }

    @Test
    fun `given empty fresh result, does not overwrite cache`() {
        // Arrange
        every { cacheSource.getNews("dota") } returns null
        coEvery { generalNewsSource.getNews("dota") } returns emptyList()

        // Act
        val result = runBlocking { repo().getNews("dota") }

        // Assert
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertTrue(result.data.value.isEmpty())
        verify(exactly = 0) { cacheSource.saveNews(any(), any()) }
    }

    @Test
    fun `given fetch fails with cache, returns stale data`() {
        // Arrange
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1000
        val stale = listOf(givenArticle("stale"))
        every { cacheSource.getNews("dota") } returns (stale to expiredAt)
        coEvery { generalNewsSource.getNews("dota") } throws Exception("network down")

        // Act
        val result = runBlocking { repo().getNews("dota") }

        // Assert
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertTrue(result.data.isStale)
        assertEquals(stale, result.data.value)
    }

    @Test
    fun `given fetch fails and no cache, returns failure`() {
        // Arrange
        every { cacheSource.getNews("dota") } returns null
        coEvery { generalNewsSource.getNews("dota") } throws Exception("network down")

        // Act
        val result = runBlocking { repo().getNews("dota") }

        // Assert
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given game news, uses steam-prefixed cache key`() {
        // Arrange
        every { cacheSource.getNews("steam_570") } returns null
        every { cacheSource.saveNews(any(), any()) } just Runs
        val fresh = listOf(givenArticle("dota update"))
        coEvery { gameNewsSource.getNewsForGame(570) } returns fresh

        // Act
        val result = runBlocking { repo().getNewsForGame(570) }

        // Assert
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertEquals(fresh, result.data.value)
        verify { cacheSource.saveNews("steam_570", fresh) }
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
