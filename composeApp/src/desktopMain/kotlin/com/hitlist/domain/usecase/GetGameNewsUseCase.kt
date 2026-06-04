package com.hitlist.domain.usecase

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.result.AppResult

interface GetGameNewsUseCase {
    suspend fun execute(query: String, appId: Int? = null): AppResult<List<NewsArticle>>
}
