package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

interface GetGeneralNewsUseCase {
    suspend fun execute(query: String): AppResult<Stale<List<NewsArticle>>>
}
