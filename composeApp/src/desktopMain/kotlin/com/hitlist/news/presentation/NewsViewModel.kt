package com.hitlist.news.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitlist.common.domain.AppResult
import com.hitlist.common.presentation.UiState
import com.hitlist.news.domain.GetGameNewsUseCase
import com.hitlist.news.domain.GetGeneralNewsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    private val getGeneralNewsUseCase: GetGeneralNewsUseCase,
    private val getGameNewsUseCase: GetGameNewsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    fun loadGeneralNews(query: String) {
        _uiState.update { it.copy(articlesState = UiState.Loading) }
        viewModelScope.launch {
            when (val result = getGeneralNewsUseCase.execute(query)) {
                is AppResult.Success -> _uiState.update { it.copy(articlesState = UiState.Success(result.data)) }
                is AppResult.Failure -> _uiState.update { it.copy(articlesState = UiState.Error(result.error)) }
            }
        }
    }

    fun loadGameNews(appId: Int) {
        _uiState.update { it.copy(articlesState = UiState.Loading) }
        viewModelScope.launch {
            when (val result = getGameNewsUseCase.execute(appId)) {
                is AppResult.Success -> _uiState.update { it.copy(articlesState = UiState.Success(result.data)) }
                is AppResult.Failure -> _uiState.update { it.copy(articlesState = UiState.Error(result.error)) }
            }
        }
    }

    fun setApiKeyMissing() {
        _uiState.update {
            it.copy(
                isApiKeyMissing = true,
                articlesState = UiState.Success(emptyList())
            )
        }
    }
}
