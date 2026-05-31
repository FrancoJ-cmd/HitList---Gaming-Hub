package com.hitlist.presentation.common

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T, val isStale: Boolean = false) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
