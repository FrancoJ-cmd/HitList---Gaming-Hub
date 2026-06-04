package com.hitlist.domain.usecase

import com.hitlist.data.fakes.NewsRepositoryFake
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.result.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetGameNewsUseCaseImplTest {

    private fun givenArticle(title: String = "Article") =
        NewsArticle(title, null, "Source", "https://url.com", null, "2024-01-01")

    @Test
    fun `given non-blank query, articles are returned for that query`() {
        val articles = listOf(givenArticle("Dota 2 news"))
        val repo = NewsRepositoryFake(result = AppResult.Success(articles))
        val useCase = GetGameNewsUseCaseImpl(repo)
        val result = runBlocking { useCase.execute("Dota 2") }
        assertEquals("Dota 2", repo.lastQuery)
        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(articles, result.data)
    }

    @Test
    fun `given blank query, uses gaming as fallback`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGameNewsUseCaseImpl(repo)
        runBlocking { useCase.execute("   ") }
        assertEquals(GetGameNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
    }

    @Test
    fun `given empty query, uses gaming as fallback`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGameNewsUseCaseImpl(repo)
        runBlocking { useCase.execute("") }
        assertEquals(GetGameNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
    }

    @Test
    fun `given empty article list, returns empty without error`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGameNewsUseCaseImpl(repo)
        val result = runBlocking { useCase.execute("gaming") }
        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(true, result.data.isEmpty())
    }

    @Test
    fun `given appId, routes to getNewsForGame instead of getNews`() {
        val articles = listOf(givenArticle("CS2 Update"))
        val repo = NewsRepositoryFake(gameNewsResult = AppResult.Success(articles))
        val useCase = GetGameNewsUseCaseImpl(repo)
        val result = runBlocking { useCase.execute("Counter-Strike 2", appId = 730) }
        assertEquals(730, repo.lastAppId)
        assertEquals(null, repo.lastQuery)
        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(articles, result.data)
    }

    @Test
    fun `given null appId, routes to getNews`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGameNewsUseCaseImpl(repo)
        runBlocking { useCase.execute("gaming", appId = null) }
        assertEquals(GetGameNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
        assertEquals(null, repo.lastAppId)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
