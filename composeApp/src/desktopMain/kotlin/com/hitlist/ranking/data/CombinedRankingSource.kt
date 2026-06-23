package com.hitlist.ranking.data

import com.hitlist.ranking.domain.CombinedRanking

interface CombinedRankingSource {
    suspend fun getCombinedRanking(): CombinedRanking
}
