package com.hitlist.domain.fakes

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.usecase.GetGameNewsUseCase

class GetGameNewsUseCaseFake(
    private val result: Result<List<NewsArticle>> = Result.success(emptyList())
) : GetGameNewsUseCase {
    var lastQuery: String? = null
    var lastAppId: Int? = null

    override suspend fun execute(query: String, appId: Int?): Result<List<NewsArticle>> {
        lastQuery = query
        lastAppId = appId
        return result
    }
}
