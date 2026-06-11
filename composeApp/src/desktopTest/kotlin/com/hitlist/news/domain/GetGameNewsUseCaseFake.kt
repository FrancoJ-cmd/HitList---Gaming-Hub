package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

class GetGameNewsUseCaseFake(
    private val result: AppResult<List<NewsArticle>> = AppResult.Success(emptyList())
) : GetGameNewsUseCase {
    var lastAppId: Int? = null

    override suspend fun execute(appId: Int): AppResult<List<NewsArticle>> {
        lastAppId = appId
        return result
    }
}
