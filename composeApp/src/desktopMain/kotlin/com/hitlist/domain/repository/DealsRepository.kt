package com.hitlist.domain.repository

import com.hitlist.domain.entity.Deal
import com.hitlist.domain.result.AppResult

interface DealsRepository {
    suspend fun getDeals(gameName: String): AppResult<List<Deal>>
}
