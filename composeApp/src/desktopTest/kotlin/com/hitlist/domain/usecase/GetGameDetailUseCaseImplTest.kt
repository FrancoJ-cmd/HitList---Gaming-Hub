package com.hitlist.domain.usecase

import com.hitlist.data.fakes.GameRepositoryFake
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.error.AppError
import com.hitlist.domain.result.AppResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetGameDetailUseCaseImplTest {

    private fun givenDetail(deals: List<Deal> = emptyList()) = GameDetail(
        steamAppId = 570, name = "Dota 2", shortDescription = "MOBA",
        headerImageUrl = "", screenshots = emptyList(), metacriticScore = null,
        genres = listOf("Action"), developers = listOf("Valve"),
        releaseDate = "2013", isFree = true, currentPlayers = 500000,
        positiveRatio = 0.8, reviewScoreDesc = "Very Positive",
        totalReviews = 2000000, deals = deals
    )

    @Test
    fun `given valid appId, detail is returned`() {
        val detail = givenDetail()
        val repo = GameRepositoryFake(gameDetailResult = AppResult.Success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        assertIs<AppResult.Success<GameDetail>>(result)
        assertEquals(detail, result.data)
    }

    @Test
    fun `given appdetails failure, error is propagated`() {
        val repo = GameRepositoryFake(gameDetailResult = AppResult.Failure(AppError.Network.NoConnection))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(99999, "Unknown") }
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given CheapShark unavailable, detail is returned with empty deals`() {
        val detail = givenDetail(deals = emptyList())
        val repo = GameRepositoryFake(gameDetailResult = AppResult.Success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        assertIs<AppResult.Success<GameDetail>>(result)
        assertEquals(true, result.data.deals.isEmpty())
    }

    @Test
    fun `given metacritic null, detail loads without error`() {
        val detail = givenDetail().copy(metacriticScore = null)
        val repo = GameRepositoryFake(gameDetailResult = AppResult.Success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        assertIs<AppResult.Success<GameDetail>>(result)
        assertEquals(null, result.data.metacriticScore)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
