package com.hitlist.ranking.presentation

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.presentation.UiState
import com.hitlist.ranking.domain.GetRankedGamesUseCaseFake
import com.hitlist.ranking.domain.RankedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class RankingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun givenGame(appId: Int, genre: String = "Action") = RankedGame(
        appId, "Game $appId", "", 0.7, 1000, 0.8, "Positive", 500, listOf(genre), false
    )

    @Test
    fun `given successful load, state transitions to Success with game list`() = runTest {
        val games = listOf(givenGame(1), givenGame(2))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(games to false))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<UiState.Success<*>>(state.gamesState)
        assertEquals(games, (state.gamesState as UiState.Success<*>).data)
    }

    @Test
    fun `given network failure, state transitions to Error`() = runTest {
        val useCase = GetRankedGamesUseCaseFake(AppResult.Failure(AppError.Network.NoConnection))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Error>(vm.uiState.value.gamesState)
    }

    @Test
    fun `given offline data, state is Success with isStale true`() = runTest {
        val games = listOf(givenGame(1))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(games to true))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value.gamesState
        assertIs<UiState.Success<*>>(state)
        assertTrue((state as UiState.Success<*>).isStale)
    }

    @Test
    fun `given genre filter applied, only matching games are shown`() = runTest {
        val games = listOf(givenGame(1, "Action"), givenGame(2, "RPG"))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(games to false))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectGenre("Action")
        val filtered = (vm.uiState.value.gamesState as UiState.Success<*>).data as List<*>
        assertEquals(1, filtered.size)
        assertEquals(1, (filtered[0] as RankedGame).steamAppId)
    }

    @Test
    fun `given Todos genre selected, full list is restored`() = runTest {
        val games = listOf(givenGame(1, "Action"), givenGame(2, "RPG"))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(games to false))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.selectGenre("Action")
        vm.selectGenre(null)
        val all = (vm.uiState.value.gamesState as UiState.Success<*>).data as List<*>
        assertEquals(2, all.size)
    }
}
