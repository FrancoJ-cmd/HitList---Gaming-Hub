package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CachedRankingMetadataSourceTest {

    private val delegate = mockk<RankingMetadataSource>()
    private val cache = RankingCacheFake()

    private fun givenSource() = CachedRankingMetadataSource(delegate, cache)

    private val bulk = mapOf(
        570 to GameMetadataSeed(570, "Dota 2", positiveReviews = 1800000, negativeReviews = 200000, genres = listOf("Action"))
    )

    @Test
    fun `given valid cached metadata, when getBulkMetadata, then reuses cache without calling delegate`() = runTest {
        // Arrange
        cache.seedRankingMetadata(bulk, cachedAt = System.currentTimeMillis())

        // Act
        val result = givenSource().getBulkMetadata()

        // Assert
        assertEquals(bulk, result)
        coVerify(exactly = 0) { delegate.getBulkMetadata() }
    }

    @Test
    fun `given no cache, when getBulkMetadata, then fetches from delegate and persists it`() = runTest {
        // Arrange
        coEvery { delegate.getBulkMetadata() } returns bulk

        // Act
        val result = givenSource().getBulkMetadata()

        // Assert
        assertEquals(bulk, result)
        assertEquals(bulk, cache.getRankingMetadata()?.first)
        coVerify(exactly = 1) { delegate.getBulkMetadata() }
    }

    @Test
    fun `given expired cache, when getBulkMetadata, then refetches from delegate`() = runTest {
        // Arrange
        cache.seedRankingMetadata(bulk, cachedAt = System.currentTimeMillis() - CachePolicy.SEED_LIST_TTL_MS - 1)
        coEvery { delegate.getBulkMetadata() } returns bulk

        // Act
        givenSource().getBulkMetadata()

        // Assert
        coVerify(exactly = 1) { delegate.getBulkMetadata() }
    }

    @Test
    fun `given per-app lookup, when getMetadata, then delegates without caching`() = runTest {
        // Arrange
        val seed = GameMetadataSeed(999, "New Hit", positiveReviews = 9000, negativeReviews = 1000, genres = listOf("RPG"))
        coEvery { delegate.getMetadata(999) } returns seed

        // Act
        val result = givenSource().getMetadata(999)

        // Assert
        assertEquals(seed, result)
        coVerify(exactly = 1) { delegate.getMetadata(999) }
    }
}
