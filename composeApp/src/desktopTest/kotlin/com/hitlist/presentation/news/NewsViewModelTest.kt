package com.hitlist.presentation.news

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.fakes.GetGameNewsUseCaseFake
import com.hitlist.presentation.common.UiState
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

    @Test
    fun `given query with results, state is Success with articles`() = runTest {
        val articles = listOf(givenArticle())
        val useCase = GetGameNewsUseCaseFake(Result.success(articles))
        val vm = NewsViewModel(useCase)
        vm.loadNews("Dota 2")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value.articlesState
        assertIs<UiState.Success<*>>(state)
        assertEquals(articles, (state as UiState.Success<*>).data)
    }

    @Test
    fun `given empty query, state is Success with gaming articles`() = runTest {
        val useCase = GetGameNewsUseCaseFake(Result.success(emptyList()))
        val vm = NewsViewModel(useCase)
        vm.loadNews("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Success<*>>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given API error, state is Error`() = runTest {
        val useCase = GetGameNewsUseCaseFake(Result.failure(Exception("API error")))
        val vm = NewsViewModel(useCase)
        vm.loadNews("gaming")
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Error>(vm.uiState.value.articlesState)
    }

    @Test
    fun `given missing API key, isApiKeyMissing is true and state is Success with empty list`() = runTest {
        val useCase = GetGameNewsUseCaseFake(Result.success(emptyList()))
        val vm = NewsViewModel(useCase)
        vm.setApiKeyMissing()

        assertTrue(vm.uiState.value.isApiKeyMissing)
        assertIs<UiState.Success<*>>(vm.uiState.value.articlesState)
    }
}
