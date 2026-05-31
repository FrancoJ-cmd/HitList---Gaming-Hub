package com.hitlist.presentation.detail

import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.fakes.GetGameDetailUseCaseFake
import com.hitlist.presentation.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private fun givenDetail() = GameDetail(
        570, "Dota 2", "MOBA", "", emptyList(), null,
        listOf("Action"), listOf("Valve"), "2013", true,
        400000, 0.8, "Very Positive", 2000000, emptyList()
    )

    @Test
    fun `given successful detail load, state is Success`() = runTest {
        val detail = givenDetail()
        val useCase = GetGameDetailUseCaseFake(Result.success(detail))
        val vm = DetailViewModel(useCase)
        vm.loadDetail(570, "Dota 2")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value.detailState
        assertIs<UiState.Success<*>>(state)
        assertEquals(detail, (state as UiState.Success<*>).data)
    }

    @Test
    fun `given detail failure, state is Error`() = runTest {
        val useCase = GetGameDetailUseCaseFake(Result.failure(Exception("Not found")))
        val vm = DetailViewModel(useCase)
        vm.loadDetail(99999, "Unknown")
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Error>(vm.uiState.value.detailState)
    }

    @Test
    fun `given CheapShark unavailable, state is Success with empty deals`() = runTest {
        val detail = givenDetail().copy(deals = emptyList())
        val useCase = GetGameDetailUseCaseFake(Result.success(detail))
        val vm = DetailViewModel(useCase)
        vm.loadDetail(570, "Dota 2")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value.detailState
        assertIs<UiState.Success<*>>(state)
        assertTrue(((state as UiState.Success<*>).data as GameDetail).deals.isEmpty())
    }
}
