package com.hitlist.common.data

object CachePolicy {
    const val SEED_LIST_TTL_MS = 24 * 60 * 60 * 1000L
    const val LIVE_PLAYERS_TTL_MS = 60 * 1000L
    const val REVIEWS_TTL_MS = 6 * 60 * 60 * 1000L
    const val METADATA_TTL_MS = 24 * 60 * 60 * 1000L
    const val DEALS_TTL_MS = 60 * 60 * 1000L
    const val NEWS_TTL_MS = 30 * 60 * 1000L

    fun isValid(cachedAt: Long, ttlMs: Long): Boolean =
        System.currentTimeMillis() - cachedAt < ttlMs
}
