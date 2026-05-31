package com.hitlist.presentation.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            getRankedGamesUseCase.execute()
                .onSuccess { (games, isStale) ->
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
                .onFailure { error ->
                    _uiState.update {
                        it.copy(gamesState = UiState.Error(error.message ?: "Unknown error"))
                    }
                }
        }
    }

    fun selectGenre(genre: String?) {
        val filtered = if (genre == null) allGames
                       else allGames.filter { it.genres.contains(genre) }
        val currentState = _uiState.value.gamesState
        val isStale = (currentState as? UiState.Success)?.isStale ?: false
        _uiState.update {
            it.copy(
                gamesState = UiState.Success(filtered, isStale),
                selectedGenre = genre
            )
        }
    }
}
