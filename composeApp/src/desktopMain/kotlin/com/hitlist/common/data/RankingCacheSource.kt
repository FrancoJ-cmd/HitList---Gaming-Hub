package com.hitlist.common.data

import com.hitlist.ranking.domain.RankedGame

interface RankingCacheSource {
    fun getRankedGames(): Pair<List<RankedGame>, Long>?
    fun saveRankedGames(games: List<RankedGame>)
}
