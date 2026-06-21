package com.hitlist.detail.domain

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetGameDetailUseCaseImplTest {

    private fun givenDetail(deals: List<Deal> = emptyList()) = GameDetail(
        steamAppId = 570, name = "Dota 2", shortDescription = "MOBA",
        headerImageUrl = "", screenshots = emptyList(), metacriticScore = null,
        genres = listOf("Action"), developers = listOf("Valve"),
        releaseDate = "2013", isFree = true, currentPlayers = 500000,
        positiveRatio = 0.8, reviewScoreDesc = "Very Positive",
        totalReviews = 2000000, deals = deals
    )

    private fun success(detail: GameDetail, isStale: Boolean = false) =
        AppResult.Success(Stale(detail, isStale))

    @Test
    fun `given valid appId, detail is returned`() {
        // Arrange
        val detail = givenDetail()
        val repo = GameDetailRepositoryFake(gameDetailResult = success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertEquals(detail, result.data.value)
    }

    @Test
    fun `given appdetails failure, error is propagated`() {
        // Arrange
        val repo = GameDetailRepositoryFake(gameDetailResult = AppResult.Failure(AppError.Network.NoConnection))
        val useCase = GetGameDetailUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute(99999, "Unknown") }
        // Assert
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given stale cache fallback, stale flag is propagated`() {
        // Arrange
        val repo = GameDetailRepositoryFake(gameDetailResult = success(givenDetail(), isStale = true))
        val useCase = GetGameDetailUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertTrue(result.data.isStale)
    }

    @Test
    fun `given CheapShark unavailable, detail is returned with empty deals`() {
        // Arrange
        val detail = givenDetail(deals = emptyList())
        val repo = GameDetailRepositoryFake(gameDetailResult = success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertTrue(result.data.value.deals.isEmpty())
    }

    @Test
    fun `given metacritic null, detail loads without error`() {
        // Arrange
        val detail = givenDetail().copy(metacriticScore = null)
        val repo = GameDetailRepositoryFake(gameDetailResult = success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        // Act
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertEquals(null, result.data.value.metacriticScore)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
