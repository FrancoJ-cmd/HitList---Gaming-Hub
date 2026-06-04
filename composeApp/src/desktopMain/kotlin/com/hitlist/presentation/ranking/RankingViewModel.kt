package com.hitlist.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.usecase.GetRankedGamesUseCase
import com.hitlist.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RankingViewModel(
    private val getRankedGamesUseCase: GetRankedGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private var allGames = emptyList<com.hitlist.domain.entity.RankedGame>()

    init {
        loadRanking()
    }

    fun loadRanking() {
        _uiState.update { it.copy(gamesState = UiState.Loading) }
        viewModelScope.launch {
            when (val result = getRankedGamesUseCase.execute()) {
                is AppResult.Success -> {
                    val (games, isStale) = result.data
                    allGames = games
                    val genres = games.flatMap { it.genres }.distinct().sorted()
                    _uiState.update {
                        it.copy(
                            gamesState = UiState.Success(games, isStale),
                            availableGenres = genres,
                            selectedGenre = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(gamesState = UiState.Error(result.error)) }
                }
            }
        }
    }

    fun selectGenre(genre: String?) {
        val filtered = if (genre == null) allGames else allGames.filter { it.genres.contains(genre) }
        val isStale = (_uiState.value.gamesState as? UiState.Success)?.isStale ?: false
        _uiState.update {
            it.copy(
                gamesState = UiState.Success(filtered, isStale),
                selectedGenre = genre
            )
        }
    }
}
