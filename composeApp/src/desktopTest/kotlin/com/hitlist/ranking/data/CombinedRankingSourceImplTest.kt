package com.hitlist.ranking.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CombinedRankingSourceImplTest {

    private val liveRankingSource = mockk<LiveRankingSource>()
    private val rankingMetadataSource = mockk<RankingMetadataSource>()

    private fun givenSource() = CombinedRankingSourceImpl(liveRankingSource, rankingMetadataSource)

    @Test
    fun `given live entries and bulk metadata, when getCombinedRanking, then joins them by appId`() = runTest {
        // Arrange
        coEvery { liveRankingSource.getLiveRanking() } returns LiveRanking(
            entries = listOf(LiveRankEntry(appId = 570, concurrentPlayers = 400000, rank = 1)),
            lastUpdate = 1_700L
        )
        coEvery { rankingMetadataSource.getBulkMetadata() } returns mapOf(
            570 to GameMetadataSeed(570, "Dota 2", positiveReviews = 1800000, negativeReviews = 200000, genres = listOf("Action"))
        )

        // Act
        val result = givenSource().getCombinedRanking()

        // Assert
        val entry = result.entries.single()
        assertEquals(570, entry.appId)
        assertEquals("Dota 2", entry.name)
        assertEquals(400000, entry.concurrentPlayers)
        assertEquals(1_700L, result.lastUpdate)
    }

    @Test
    fun `given appId missing from bulk, when getCombinedRanking, then fetches it via per-game fallback`() = runTest {
        // Arrange
        coEvery { liveRankingSource.getLiveRanking() } returns LiveRanking(
            entries = listOf(LiveRankEntry(appId = 999, concurrentPlayers = 50000, rank = 1)),
            lastUpdate = 1_700L
        )
        coEvery { rankingMetadataSource.getBulkMetadata() } returns emptyMap()
        coEvery { rankingMetadataSource.getMetadata(999) } returns
            GameMetadataSeed(999, "New Hit", positiveReviews = 9000, negativeReviews = 1000, genres = listOf("RPG"))

        // Act
        val result = givenSource().getCombinedRanking()

        // Assert
        assertEquals("New Hit", result.entries.single().name)
        coVerify(exactly = 1) { rankingMetadataSource.getMetadata(999) }
    }
}
