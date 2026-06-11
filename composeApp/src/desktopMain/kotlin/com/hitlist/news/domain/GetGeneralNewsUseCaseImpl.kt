package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

class GetGeneralNewsUseCaseImpl(
    private val newsRepository: NewsRepository
) : GetGeneralNewsUseCase {
    override suspend fun execute(query: String): AppResult<List<NewsArticle>> =
        newsRepository.getNews(query.trim().ifBlank { DEFAULT_QUERY })

    companion object {
        const val DEFAULT_QUERY = "gaming"
    }
}
