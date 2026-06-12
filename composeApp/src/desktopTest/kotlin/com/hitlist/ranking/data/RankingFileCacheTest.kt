package com.hitlist.ranking.data

import com.hitlist.common.data.JsonFileStore
import com.hitlist.ranking.domain.RankedGame
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RankingFileCacheTest {

    private lateinit var tempDir: File
    private lateinit var cache: RankingFileCache

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("hitlist_test").toFile()
        cache = RankingFileCache(JsonFileStore(tempDir))
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun givenGame(appId: Int = 1) = RankedGame(
        appId, "Game $appId", "https://img.com", 0.7, 1000, 0.8, "Positive", 500, listOf("Action"), false
    )

    @Test
    fun `given saved games, they are retrieved correctly`() {
        val games = listOf(givenGame(1), givenGame(2))
        cache.saveRankedGames(games)
        val loaded = cache.getRankedGames()
        assertNotNull(loaded)
        assertEquals(games, loaded.first)
    }

    @Test
    fun `given no saved data, getRankedGames returns null`() {
        assertNull(cache.getRankedGames())
    }

    @Test
    fun `given saved data overwritten, new data is stored`() {
        cache.saveRankedGames(listOf(givenGame(1)))
        cache.saveRankedGames(listOf(givenGame(2), givenGame(3)))
        val loaded = cache.getRankedGames()
        assertNotNull(loaded)
        assertEquals(2, loaded.first.size)
        assertEquals(2, loaded.first[0].steamAppId)
    }

    @Test
    fun `given saved metadata with explicit timestamp, it is retrieved with that timestamp`() {
        val metadata = mapOf(
            570 to GameMetadataSeed(570, "Dota 2", 1800000, 200000, listOf("Action"))
        )
        cache.saveRankingMetadata(metadata, cachedAt = 42L)
        val loaded = cache.getRankingMetadata()
        assertNotNull(loaded)
        assertEquals(metadata, loaded.first)
        assertEquals(42L, loaded.second)
    }
}