package com.hitlist.domain.usecase

import com.hitlist.domain.entity.NewsArticle

interface GetGameNewsUseCase {
    suspend fun execute(query: String, appId: Int? = null): Result<List<NewsArticle>>
}
