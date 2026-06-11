package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

class GetGeneralNewsUseCaseFake(
    private val result: AppResult<List<NewsArticle>> = AppResult.Success(emptyList())
) : GetGeneralNewsUseCase {
    var lastQuery: String? = null

    override suspend fun execute(query: String): AppResult<List<NewsArticle>> {
        lastQuery = query
        return result
    }
}
