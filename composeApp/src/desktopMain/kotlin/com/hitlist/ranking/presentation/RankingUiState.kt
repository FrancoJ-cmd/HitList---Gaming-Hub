package com.hitlist.ranking.presentation

import com.hitlist.common.presentation.UiState
import com.hitlist.ranking.domain.RankedGame

data class RankingUiState(
    val gamesState: UiState<List<RankedGame>> = UiState.Loading,
    val selectedGenre: String? = null,
    val availableGenres: List<String> = emptyList()
)
