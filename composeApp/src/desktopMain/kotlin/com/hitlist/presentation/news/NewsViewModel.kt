package com.hitlist.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitlist.domain.usecase.GetGameNewsUseCase
import com.hitlist.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
    private val getGameNewsUseCase: GetGameNewsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    fun loadNews(query: String = "", appId: Int? = null) {
        _uiState.update { it.copy(articlesState = UiState.Loading) }
        viewModelScope.launch {
            getGameNewsUseCase.execute(query, appId)
                .onSuccess { articles ->
                    _uiState.update { it.copy(articlesState = UiState.Success(articles)) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(articlesState = UiState.Error(error.message ?: "Unknown error"))
                    }
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
