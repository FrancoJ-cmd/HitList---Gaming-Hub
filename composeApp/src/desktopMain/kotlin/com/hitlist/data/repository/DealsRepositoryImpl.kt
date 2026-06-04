package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.mapper.toAppResult
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.repository.DealsRepository
import com.hitlist.domain.result.AppResult

class DealsRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val dealsSource: GameDealsSource
) : DealsRepository {

    override suspend fun getDeals(gameName: String): AppResult<List<Deal>> {
        val cached = localDataSource.getDeals(gameName)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.DEALS_TTL_MS)) {
            return AppResult.Success(cached.first)
        }

        val freshResult = runCatching {
            dealsSource.getDeals(gameName).also { localDataSource.saveDeals(gameName, it) }
        }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> freshResult
            is AppResult.Failure -> if (cached != null) AppResult.Success(cached.first) else freshResult
        }
    }
}
