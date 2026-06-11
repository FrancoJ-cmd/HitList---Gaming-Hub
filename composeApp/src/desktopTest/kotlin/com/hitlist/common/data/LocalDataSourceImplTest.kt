package com.hitlist.common.data

import com.hitlist.detail.domain.GameDetail
import com.hitlist.news.domain.NewsArticle
import com.hitlist.ranking.domain.RankedGame
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class LocalDataSourceImplTest {

    private lateinit var tempDir: File
    private lateinit var dataSource: LocalDataSourceImpl

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("hitlist_test").toFile()
        dataSource = LocalDataSourceImpl(tempDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun givenGame(appId: Int = 1) = RankedGame(
        appId, "Game $appId", "https://img.com", 0.7, 1000, 0.8, "Positive", 500, listOf("Action"), false
    )

    private fun givenDetail(appId: Int = 570) = GameDetail(
        appId, "Dota 2", "Desc", "https://img.com", emptyList(), null,
        listOf("Action"), listOf("Valve"), "2013", true, 400000, 0.8, "Very Positive", 2000000, emptyList()
    )

    private fun givenArticle() = NewsArticle("Title", "Desc", "Source", "https://url.com", null, "2024-01-01")

    @Test
    fun `given saved games, they are retrieved correctly`() {
        val games = listOf(givenGame(1), givenGame(2))
        dataSource.saveRankedGames(games)
        val loaded = dataSource.getRankedGames()
        assertNotNull(loaded)
        assertEquals(games, loaded.first)
    }

    @Test
    fun `given no saved data, getRankedGames returns null`() {
        assertNull(dataSource.getRankedGames())
    }

    @Test
    fun `given saved data overwritten with same key, new data is stored`() {
        dataSource.saveRankedGames(listOf(givenGame(1)))
        dataSource.saveRankedGames(listOf(givenGame(2), givenGame(3)))
        val loaded = dataSource.getRankedGames()
        assertNotNull(loaded)
        assertEquals(2, loaded.first.size)
        assertEquals(2, loaded.first[0].steamAppId)
    }

    @Test
    fun `given saved game detail, it is retrieved by appId`() {
        val detail = givenDetail()
        dataSource.saveGameDetail(detail)
        val loaded = dataSource.getGameDetail(570)
        assertNotNull(loaded)
        assertEquals(detail, loaded.first)
    }

    @Test
    fun `given cached timestamp, isCacheValid returns false when expired`() {
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1000
        assertFalse(CachePolicy.isValid(expiredAt, CachePolicy.NEWS_TTL_MS))
    }

    @Test
    fun `given cached timestamp, isCacheValid returns true when fresh`() {
        val freshAt = System.currentTimeMillis()
        assertTrue(CachePolicy.isValid(freshAt, CachePolicy.NEWS_TTL_MS))
    }

    @Test
    fun `given saved news, they are retrieved by query`() {
        val articles = listOf(givenArticle())
        dataSource.saveNews("gaming", articles)
        val loaded = dataSource.getNews("gaming")
        assertNotNull(loaded)
        assertEquals(articles, loaded.first)
    }
}
