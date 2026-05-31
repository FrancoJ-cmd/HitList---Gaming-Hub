package com.hitlist.domain.usecase

import com.hitlist.domain.entity.RankedGame

interface GetRankedGamesUseCase {
    suspend fun execute(): Result<Pair<List<RankedGame>, Boolean>>
}
