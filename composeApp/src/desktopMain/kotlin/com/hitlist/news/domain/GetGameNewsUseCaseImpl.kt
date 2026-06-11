package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult

class GetGameNewsUseCaseImpl(
    private val newsRepository: NewsRepository
) : GetGameNewsUseCase {
    override suspend fun execute(appId: Int): AppResult<List<NewsArticle>> =
        newsRepository.getNewsForGame(appId)
}
