package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

interface GetGameNewsUseCase {
    suspend fun execute(appId: Int): AppResult<List<NewsArticle>>
}
