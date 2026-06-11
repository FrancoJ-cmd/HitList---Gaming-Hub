package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetGeneralNewsUseCaseImplTest {

    private fun givenArticle(title: String = "Article") =
        NewsArticle(title, null, "Source", "https://url.com", null, "2024-01-01")

    @Test
    fun `given non-blank query, passes it through to repository`() {
        val articles = listOf(givenArticle("Dota 2 news"))
        val repo = NewsRepositoryFake(result = AppResult.Success(articles))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        val result = runBlocking { useCase.execute("Dota 2") }
        assertEquals("Dota 2", repo.lastQuery)
        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(articles, result.data)
    }

    @Test
    fun `given blank query, uses gaming as fallback`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        runBlocking { useCase.execute("   ") }
        assertEquals(GetGeneralNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
    }

    @Test
    fun `given empty query, uses gaming as fallback`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        runBlocking { useCase.execute("") }
        assertEquals(GetGeneralNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
    }

    @Test
    fun `given empty article list, returns empty without error`() {
        val repo = NewsRepositoryFake(result = AppResult.Success(emptyList()))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        val result = runBlocking { useCase.execute("gaming") }
        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertTrue(result.data.isEmpty())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
