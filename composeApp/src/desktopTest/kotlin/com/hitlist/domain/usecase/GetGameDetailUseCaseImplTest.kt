package com.hitlist.domain.usecase

import com.hitlist.data.fakes.GameRepositoryFake
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `given valid appId, detail is returned`() {
        val detail = givenDetail()
        val repo = GameRepositoryFake(gameDetailResult = Result.success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        assertTrue(result.isSuccess)
        assertEquals(detail, result.getOrThrow())
    }

    @Test
    fun `given appdetails failure, error is propagated`() {
        val repo = GameRepositoryFake(gameDetailResult = Result.failure(Exception("Not found")))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(99999, "Unknown") }
        assertTrue(result.isFailure)
    }

    @Test
    fun `given CheapShark unavailable, detail is returned with empty deals`() {
        val detail = givenDetail(deals = emptyList())
        val repo = GameRepositoryFake(gameDetailResult = Result.success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().deals.isEmpty())
    }

    @Test
    fun `given metacritic null, detail loads without error`() {
        val detail = givenDetail().copy(metacriticScore = null)
        val repo = GameRepositoryFake(gameDetailResult = Result.success(detail))
        val useCase = GetGameDetailUseCaseImpl(repo)
        val result = runBlocking { useCase.execute(570, "Dota 2") }
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow().metacriticScore)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
