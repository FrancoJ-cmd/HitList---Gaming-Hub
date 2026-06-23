package com.hitlist.detail.presentation

import com.hitlist.common.presentation.UiState
import com.hitlist.detail.domain.GameDetail

data class DetailUiState(
    val detailState: UiState<GameDetail> = UiState.Loading
)
