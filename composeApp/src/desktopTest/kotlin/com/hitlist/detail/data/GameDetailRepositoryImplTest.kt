package com.hitlist.detail.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.detail.domain.Deal
import com.hitlist.detail.domain.GameDetail
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameDetailRepositoryImplTest {

    private val cacheSource = mockk<GameDetailCacheSource>(relaxed = true)
    private val playerCountSource = mockk<PlayerCountSource>()
    private val metadataSource = mockk<GameMetadataSource>()
    private val reviewSource = mockk<GameReviewSource>()
    private val dealsSource = mockk<GameDealsSource>()

    private fun repo() = GameDetailRepositoryImpl(
        cacheSource, playerCountSource, metadataSource, reviewSource, dealsSource
    )

    private fun givenMetadata() = GameMetadata(
        name = "Dota 2", shortDescription = "MOBA", headerImageUrl = "header.jpg",
        screenshots = listOf("s1.jpg"), metacriticScore = 90, genres = listOf("Action"),
        developers = listOf("Valve"), releaseDate = "2013", isFree = true
    )

    private fun givenDetail() = GameDetail(
        steamAppId = 570, name = "Dota 2", shortDescription = "MOBA",
        headerImageUrl = "header.jpg", screenshots = emptyList(), metacriticScore = 90,
        genres = listOf("Action"), developers = listOf("Valve"), releaseDate = "2013",
        isFree = true, currentPlayers = 400000, positiveRatio = 0.9,
        reviewScoreDesc = "Very Positive", totalReviews = 100, deals = emptyList()
    )

    private fun stubFreshSources(
        metadata: GameMetadata? = givenMetadata(),
        review: ReviewInfo? = ReviewInfo("Very Positive", totalPositive = 90, totalReviews = 100),
        deals: List<Deal> = emptyList(),
        players: Int = 400000
    ) {
        coEvery { metadataSource.getGameMetadata(any()) } returns metadata
        coEvery { reviewSource.getGameReviews(any()) } returns review
        coEvery { dealsSource.getDeals(any()) } returns deals
        coEvery { playerCountSource.getCurrentPlayers(any()) } returns players
    }

    @Test
    fun `given valid cache, returns cached data without remote calls`() {
        // Arrange
        every { cacheSource.getGameDetail(570) } returns (givenDetail() to System.currentTimeMillis())

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertFalse(result.data.isStale)
        assertEquals(givenDetail(), result.data.value)
        coVerify(exactly = 0) { metadataSource.getGameMetadata(any()) }
    }

    @Test
    fun `given expired cache and fresh fetch, returns fresh data and caches it`() {
        // Arrange
        val expiredAt = System.currentTimeMillis() - CachePolicy.METADATA_TTL_MS - 1000
        every { cacheSource.getGameDetail(570) } returns (givenDetail() to expiredAt)
        every { cacheSource.saveGameDetail(any()) } just Runs
        stubFreshSources()

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertFalse(result.data.isStale)
        assertEquals(0.9, result.data.value.positiveRatio)
        assertEquals(400000, result.data.value.currentPlayers)
        verify { cacheSource.saveGameDetail(any()) }
    }

    @Test
    fun `given no cache and metadata missing, returns NotFound`() {
        // Arrange
        every { cacheSource.getGameDetail(570) } returns null
        stubFreshSources(metadata = null)

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.NotFound, result.error)
    }

    @Test
    fun `given fetch fails but cache exists, returns stale cached data`() {
        // Arrange
        val expiredAt = System.currentTimeMillis() - CachePolicy.METADATA_TTL_MS - 1000
        every { cacheSource.getGameDetail(570) } returns (givenDetail() to expiredAt)
        coEvery { metadataSource.getGameMetadata(any()) } throws Exception("network down")

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertTrue(result.data.isStale)
        assertEquals(givenDetail(), result.data.value)
    }

    @Test
    fun `given fetch fails and no cache, returns failure`() {
        // Arrange
        every { cacheSource.getGameDetail(570) } returns null
        coEvery { metadataSource.getGameMetadata(any()) } throws Exception("network down")

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Failure>(result)
    }

    @Test
    fun `given no reviews, computes zero ratio and empty score description`() {
        // Arrange
        every { cacheSource.getGameDetail(570) } returns null
        every { cacheSource.saveGameDetail(any()) } just Runs
        stubFreshSources(review = null)

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertEquals(0.0, result.data.value.positiveRatio)
        assertEquals("", result.data.value.reviewScoreDesc)
        assertEquals(0, result.data.value.totalReviews)
    }

    @Test
    fun `given zero total reviews, avoids division and returns zero ratio`() {
        // Arrange
        every { cacheSource.getGameDetail(570) } returns null
        every { cacheSource.saveGameDetail(any()) } just Runs
        stubFreshSources(review = ReviewInfo("", totalPositive = 0, totalReviews = 0))

        // Act
        val result = runBlocking { repo().getGameDetail(570, "Dota 2") }

        // Assert
        assertIs<AppResult.Success<Stale<GameDetail>>>(result)
        assertEquals(0.0, result.data.value.positiveRatio)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
