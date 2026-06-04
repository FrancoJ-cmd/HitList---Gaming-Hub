package com.hitlist.domain.fakes

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.usecase.GetGameNewsUseCase

class GetGameNewsUseCaseFake(
    private val result: AppResult<List<NewsArticle>> = AppResult.Success(emptyList())
) : GetGameNewsUseCase {
    var lastQuery: String? = null
    var lastAppId: Int? = null

    override suspend fun execute(query: String, appId: Int?): AppResult<List<NewsArticle>> {
        lastQuery = query
        lastAppId = appId
        return result
    }
}
