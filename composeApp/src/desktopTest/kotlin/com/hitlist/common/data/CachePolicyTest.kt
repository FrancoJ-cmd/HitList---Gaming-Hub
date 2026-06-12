package com.hitlist.common.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CachePolicyTest {

    @Test
    fun `given cached timestamp, isValid returns false when expired`() {
        val expiredAt = System.currentTimeMillis() - CachePolicy.NEWS_TTL_MS - 1000
        assertFalse(CachePolicy.isValid(expiredAt, CachePolicy.NEWS_TTL_MS))
    }

    @Test
    fun `given cached timestamp, isValid returns true when fresh`() {
        val freshAt = System.currentTimeMillis()
        assertTrue(CachePolicy.isValid(freshAt, CachePolicy.NEWS_TTL_MS))
    }
}