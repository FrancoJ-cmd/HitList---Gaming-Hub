package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class GetGeneralNewsUseCaseFake(
    private val result: AppResult<Stale<List<NewsArticle>>> = AppResult.Success(Stale(emptyList(), false))
) : GetGeneralNewsUseCase {
    var lastQuery: String? = null

    override suspend fun execute(query: String): AppResult<Stale<List<NewsArticle>>> {
        lastQuery = query
        return result
    }
}
