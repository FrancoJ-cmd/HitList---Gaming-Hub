package com.hitlist.ranking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitlist.common.domain.AppResult
import com.hitlist.common.presentation.UiState
import com.hitlist.ranking.domain.GetRankedGamesUseCase
import com.hitlist.ranking.domain.RankedGame
import kotlinx.coroutines.Job
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

    private var allGames = emptyList<RankedGame>()
    private var observeJob: Job? = null

    init {
        observeRanking()
    }

    fun retry() = observeRanking()

    fun selectGenre(genre: String?) {
        _uiState.update { state ->
            state.copy(gamesState = filteredState(genre, currentIsStale()), selectedGenre = genre)
        }
    }

    private fun observeRanking() {
        observeJob?.cancel()
        _uiState.update {
            val keepData = it.gamesState as? UiState.Success
            it.copy(gamesState = keepData ?: UiState.Loading)
        }
        observeJob = viewModelScope.launch {
            getRankedGamesUseCase.observe().collect { result ->
                when (result) {
                    is AppResult.Success -> onGamesLoaded(result.data.first, result.data.second)
                    is AppResult.Failure -> _uiState.update {
                        if (it.gamesState is UiState.Success) it
                        else it.copy(gamesState = UiState.Error(result.error))
                    }
                }
            }
        }
    }

    private fun onGamesLoaded(games: List<RankedGame>, isStale: Boolean) {
        allGames = games
        _uiState.update { state ->
            state.copy(
                gamesState = filteredState(state.selectedGenre, isStale),
                availableGenres = games.flatMap { it.genres }.distinct().sorted()
            )
        }
    }

    private fun filteredState(genre: String?, isStale: Boolean): UiState<List<RankedGame>> {
        val visible = if (genre == null) allGames else allGames.filter { it.genres.contains(genre) }
        return UiState.Success(visible, isStale)
    }

    private fun currentIsStale(): Boolean =
        (_uiState.value.gamesState as? UiState.Success)?.isStale ?: false
}
