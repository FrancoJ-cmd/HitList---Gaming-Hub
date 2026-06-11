package com.hitlist.news.presentation

import com.hitlist.common.presentation.UiState
import com.hitlist.news.domain.NewsArticle

data class NewsUiState(
    val articlesState: UiState<List<NewsArticle>> = UiState.Loading,
    val isApiKeyMissing: Boolean = false
)
