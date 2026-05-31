package com.hitlist.presentation.detail

import com.hitlist.domain.entity.GameDetail
import com.hitlist.presentation.common.UiState

data class DetailUiState(
    val detailState: UiState<GameDetail> = UiState.Loading
)
