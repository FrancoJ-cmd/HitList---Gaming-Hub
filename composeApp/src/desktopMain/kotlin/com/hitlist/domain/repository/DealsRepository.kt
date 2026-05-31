package com.hitlist.domain.repository

import com.hitlist.domain.entity.Deal

interface DealsRepository {
    suspend fun getDeals(gameName: String): List<Deal>
}
