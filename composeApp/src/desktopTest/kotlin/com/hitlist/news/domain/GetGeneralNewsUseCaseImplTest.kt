package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetGeneralNewsUseCaseImplTest {

    private fun givenArticle(title: String = "Article") =
        NewsArticle(title, null, "Source", "https://url.com", null, "2024-01-01")

    @Test
    fun `given non-blank query, passes it through to repository`() {
        // Arrange
        val articles = listOf(givenArticle("Dota 2 news"))
        val repo = NewsRepositoryFake(result = AppResult.Success(Stale(articles, false)))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute("Dota 2") }
        // Assert
        assertEquals("Dota 2", repo.lastQuery)
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertEquals(articles, result.data.value)
    }

    @Test
    fun `given blank query, uses gaming as fallback`() {
        // Arrange
        val repo = NewsRepositoryFake(result = AppResult.Success(Stale(emptyList(), false)))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        // Act
        runBlocking { useCase.execute("   ") }
        // Assert
        assertEquals(GetGeneralNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
    }

    @Test
    fun `given empty query, uses gaming as fallback`() {
        // Arrange
        val repo = NewsRepositoryFake(result = AppResult.Success(Stale(emptyList(), false)))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        // Act
        runBlocking { useCase.execute("") }
        // Assert
        assertEquals(GetGeneralNewsUseCaseImpl.DEFAULT_QUERY, repo.lastQuery)
    }

    @Test
    fun `given empty article list, returns empty without error`() {
        // Arrange
        val repo = NewsRepositoryFake(result = AppResult.Success(Stale(emptyList(), false)))
        val useCase = GetGeneralNewsUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute("gaming") }
        // Assert
        assertIs<AppResult.Success<Stale<List<NewsArticle>>>>(result)
        assertTrue(result.data.value.isEmpty())
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
