package com.hitlist.detail.data

import com.hitlist.common.data.JsonFileStore
import com.hitlist.detail.domain.GameDetail
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameDetailFileCacheTest {

    private lateinit var tempDir: File
    private lateinit var cache: GameDetailFileCache

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("hitlist_test").toFile()
        cache = GameDetailFileCache(JsonFileStore(tempDir))
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun givenDetail(appId: Int = 570) = GameDetail(
        appId, "Dota 2", "Desc", "https://img.com", emptyList(), null,
        listOf("Action"), listOf("Valve"), "2013", true, 400000, 0.8, "Very Positive", 2000000, emptyList()
    )

    @Test
    fun `given saved game detail, it is retrieved by appId`() {
        val detail = givenDetail()
        cache.saveGameDetail(detail)
        val loaded = cache.getGameDetail(570)
        assertNotNull(loaded)
        assertEquals(detail, loaded.first)
    }

    @Test
    fun `given no saved data, getGameDetail returns null`() {
        assertNull(cache.getGameDetail(570))
    }
}