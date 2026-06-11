package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetGameNewsUseCaseImplTest {

    private fun givenArticle(title: String = "Article") =
        NewsArticle(title, null, "Source", "https://url.com", null, "2024-01-01")

    @Test
    fun `given appId, routes to getNewsForGame`() {
        val articles = listOf(givenArticle("CS2 Update"))
        val repo = NewsRepositoryFake(gameNewsResult = AppResult.Success(articles))
        val useCase = GetGameNewsUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(730) }
        assertEquals(730, repo.lastAppId)
        assertIs<AppResult.Success<List<NewsArticle>>>(result)
        assertEquals(articles, result.data)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
