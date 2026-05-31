package com.hitlist.data.remote

import com.hitlist.domain.entity.Deal

interface GameDealsSource {
    suspend fun getDeals(gameName: String): List<Deal>
}
