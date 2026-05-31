package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.remote.cheapshark.CheapSharkProxy
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.repository.DealsRepository

class DealsRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val cheapSharkProxy: CheapSharkProxy
) : DealsRepository {

    override suspend fun getDeals(gameName: String): List<Deal> {
        val cached = localDataSource.getDeals(gameName)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.DEALS_TTL_MS)) {
            return cached.first
        }

        return runCatching {
            cheapSharkProxy.getDeals(gameName)
                .also { localDataSource.saveDeals(gameName, it) }
        }.getOrDefault(cached?.first ?: emptyList())
    }
}
