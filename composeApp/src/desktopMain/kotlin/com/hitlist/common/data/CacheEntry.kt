package com.hitlist.common.data

import kotlinx.serialization.Serializable

@Serializable
data class CacheEntry<T>(val cachedAt: Long, val data: T)