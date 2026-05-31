package com.hitlist.presentation.news

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.presentation.common.UiState

data class NewsUiState(
    val articlesState: UiState<List<NewsArticle>> = UiState.Loading,
    val isApiKeyMissing: Boolean = false
)
