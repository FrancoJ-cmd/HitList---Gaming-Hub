package com.hitlist.news.data

import com.hitlist.common.data.JsonFileStore
import com.hitlist.news.domain.NewsArticle
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NewsFileCacheTest {

    private lateinit var tempDir: File
    private lateinit var cache: NewsFileCache

    @BeforeTest
    fun setUp() {
        tempDir = Files.createTempDirectory("hitlist_test").toFile()
        cache = NewsFileCache(JsonFileStore(tempDir))
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun givenArticle() = NewsArticle("Title", "Desc", "Source", "https://url.com", null, "2024-01-01")

    @Test
    fun `given saved news, they are retrieved by query`() {
        // Arrange
        val articles = listOf(givenArticle())
        cache.saveNews("gaming", articles)
        // Act
        val loaded = cache.getNews("gaming")
        // Assert
        assertNotNull(loaded)
        assertEquals(articles, loaded.first)
    }

    @Test
    fun `given no saved data, getNews returns null`() {
        // Act & Assert
        assertNull(cache.getNews("gaming"))
    }
}
