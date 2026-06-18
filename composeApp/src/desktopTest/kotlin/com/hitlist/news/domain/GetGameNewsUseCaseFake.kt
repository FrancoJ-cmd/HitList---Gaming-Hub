package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class GetGameNewsUseCaseFake(
    private val result: AppResult<Stale<List<NewsArticle>>> = AppResult.Success(Stale(emptyList(), false))
) : GetGameNewsUseCase {
    var lastAppId: Int? = null

    override suspend fun execute(appId: Int): AppResult<Stale<List<NewsArticle>>> {
        lastAppId = appId
        return result
    }
}
