package com.hitlist.news.presentation

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.common.presentation.UiState
import com.hitlist.news.domain.GetGameNewsUseCaseFake
import com.hitlist.news.domain.GetGeneralNewsUseCaseFake
import com.hitlist.news.domain.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun givenArticle() = NewsArticle("Title", null, "Source", "https://url.com", null, "2024-01-01")

    private fun givenVm(
        generalResult: AppResult<Stale<List<NewsArticle>>> = AppResult.Success(Stale(emptyList(), false)),
        gameResult: AppResult<Stale<List<NewsArticle>>> = AppResult.Success(Stale(emptyList(), false))
    ) = NewsViewModel(GetGeneralNewsUseCaseFake(generalResult), GetGameNewsUseCaseFake(gameResult))

    @Test
    fun `given query with results, state is Success with articles`() = runTest {
        // Arrange
        val articles = listOf(givenArticle())
        val vm = givenVm(generalResult = AppResult.Success(Stale(articles, false)))
        // Act
        vm.loadGeneralNews("Dota 2")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = vm.uiState.value.articlesState
        assertIs<UiState.Success<*>>(state)
        assertEquals(articles, (state as UiState.Success<*>).data)
    }

    @Test
    fun `given empty query, state is Success`() = runTest {
        // Arrange
        val vm = givenVm()
        // Act
        vm.loadGeneralNews("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertIs<UiState.Success<*>>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given API error, state is Error`() = runTest {
        // Arrange
        val vm = givenVm(generalResult = AppResult.Failure(AppError.Network.Timeout))
        // Act
        vm.loadGeneralNews("gaming")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertIs<UiState.Error>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given missing API key, isApiKeyMissing is true and state is Success with empty list`() = runTest {
        // Arrange
        val vm = givenVm()
        // Act
        vm.setApiKeyMissing()

        // Assert
        assertTrue(vm.uiState.value.isApiKeyMissing)
        assertIs<UiState.Success<*>>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given appId, loadGameNews returns articles from game news use case`() = runTest {
        // Arrange
        val articles = listOf(givenArticle())
        val vm = givenVm(gameResult = AppResult.Success(Stale(articles, false)))
        // Act
        vm.loadGameNews(730)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = vm.uiState.value.articlesState
        assertIs<UiState.Success<*>>(state)
        assertEquals(articles, (state as UiState.Success<*>).data)
    }
}
