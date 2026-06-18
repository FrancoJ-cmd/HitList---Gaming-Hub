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
        val articles = listOf(givenArticle())
        val vm = givenVm(generalResult = AppResult.Success(Stale(articles, false)))
        vm.loadGeneralNews("Dota 2")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value.articlesState
        assertIs<UiState.Success<*>>(state)
        assertEquals(articles, (state as UiState.Success<*>).data)
    }

    @Test
    fun `given empty query, state is Success`() = runTest {
        val vm = givenVm()
        vm.loadGeneralNews("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Success<*>>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given API error, state is Error`() = runTest {
        val vm = givenVm(generalResult = AppResult.Failure(AppError.Network.Timeout))
        vm.loadGeneralNews("gaming")
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Error>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given missing API key, isApiKeyMissing is true and state is Success with empty list`() = runTest {
        val vm = givenVm()
        vm.setApiKeyMissing()

        assertTrue(vm.uiState.value.isApiKeyMissing)
        assertIs<UiState.Success<*>>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given appId, loadGameNews returns articles from game news use case`() = runTest {
        val articles = listOf(givenArticle())
        val vm = givenVm(gameResult = AppResult.Success(Stale(articles, false)))
        vm.loadGameNews(730)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value.articlesState
        assertIs<UiState.Success<*>>(state)
        assertEquals(articles, (state as UiState.Success<*>).data)
    }
}
