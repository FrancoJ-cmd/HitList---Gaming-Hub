package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

interface GetGeneralNewsUseCase {
    suspend fun execute(query: String): AppResult<List<NewsArticle>>
}
