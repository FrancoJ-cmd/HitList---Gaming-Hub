package com.hitlist.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.usecase.GetGameDetailUseCase
import com.hitlist.presentation.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val getGameDetailUseCase: GetGameDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadDetail(appId: Int, name: String) {
        _uiState.update { it.copy(detailState = UiState.Loading) }
        viewModelScope.launch {
            when (val result = getGameDetailUseCase.execute(appId, name)) {
                is AppResult.Success -> _uiState.update { it.copy(detailState = UiState.Success(result.data)) }
                is AppResult.Failure -> _uiState.update { it.copy(detailState = UiState.Error(result.error)) }
            }
        }
    }
}
