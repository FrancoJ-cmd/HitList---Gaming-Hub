package com.hitlist.detail.data

import com.hitlist.detail.domain.Deal

interface GameDealsSource {
    suspend fun getDeals(gameName: String): List<Deal>
}
