package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

interface GetGameNewsUseCase {
    suspend fun execute(appId: Int): AppResult<Stale<List<NewsArticle>>>
}
