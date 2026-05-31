package com.hitlist.presentation.ranking

import com.hitlist.domain.entity.RankedGame
import com.hitlist.presentation.common.UiState

data class RankingUiState(
    val gamesState: UiState<List<RankedGame>> = UiState.Loading,
    val selectedGenre: String? = null,
    val availableGenres: List<String> = emptyList()
)
