package com.hitlist.domain.usecase

import com.hitlist.domain.entity.NewsArticle
import com.hitlist.domain.repository.NewsRepository
import com.hitlist.domain.result.AppResult

class GetGameNewsUseCaseImpl(
    private val newsRepository: NewsRepository
) : GetGameNewsUseCase {

    override suspend fun execute(query: String, appId: Int?): AppResult<List<NewsArticle>> =
        if (appId != null) {
            newsRepository.getNewsForGame(appId)
        } else {
            newsRepository.getNews(query.trim().ifBlank { DEFAULT_QUERY })
        }

    companion object {
        const val DEFAULT_QUERY = "gaming"
    }
}
