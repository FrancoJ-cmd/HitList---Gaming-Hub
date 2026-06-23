package com.hitlist.ranking.presentation

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.common.presentation.UiState
import com.hitlist.ranking.domain.GetRankedGamesUseCaseFake
import com.hitlist.ranking.domain.RankedGame
import com.hitlist.ranking.domain.Ranking
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
        // Arrange
        val games = listOf(givenGame(1), givenGame(2))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(Stale(Ranking(games), isStale = false)))
        // Act
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = vm.uiState.value
        assertIs<UiState.Success<*>>(state.gamesState)
        assertEquals(games, (state.gamesState as UiState.Success<*>).data)
    }

    @Test
    fun `given network failure, state transitions to Error`() = runTest {
        // Arrange
        val useCase = GetRankedGamesUseCaseFake(AppResult.Failure(AppError.Network.NoConnection))
        // Act
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertIs<UiState.Error>(vm.uiState.value.gamesState)
    }

    @Test
    fun `given offline data, state is Success with isStale true`() = runTest {
        // Arrange
        val games = listOf(givenGame(1))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(Stale(Ranking(games), isStale = true)))
        // Act
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = vm.uiState.value.gamesState
        assertIs<UiState.Success<*>>(state)
        assertTrue((state as UiState.Success<*>).isStale)
    }

    @Test
    fun `given genre filter applied, only matching games are shown`() = runTest {
        // Arrange
        val games = listOf(givenGame(1, "Action"), givenGame(2, "RPG"))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(Stale(Ranking(games), isStale = false)))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        vm.selectGenre("Action")

        // Assert
        val filtered = (vm.uiState.value.gamesState as UiState.Success<*>).data as List<*>
        assertEquals(1, filtered.size)
        assertEquals(1, (filtered[0] as RankedGame).steamAppId)
    }

    @Test
    fun `given Todos genre selected, full list is restored`() = runTest {
        // Arrange
        val games = listOf(givenGame(1, "Action"), givenGame(2, "RPG"))
        val useCase = GetRankedGamesUseCaseFake(AppResult.Success(Stale(Ranking(games), isStale = false)))
        val vm = RankingViewModel(useCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        vm.selectGenre("Action")
        vm.selectGenre(null)

        // Assert
        val all = (vm.uiState.value.gamesState as UiState.Success<*>).data as List<*>
        assertEquals(2, all.size)
    }
}
