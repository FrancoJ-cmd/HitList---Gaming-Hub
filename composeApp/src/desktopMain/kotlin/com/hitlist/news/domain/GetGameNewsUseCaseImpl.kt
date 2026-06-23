package com.hitlist.news.domain

import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale

class GetGameNewsUseCaseImpl(
    private val newsRepository: NewsRepository
) : GetGameNewsUseCase {
    override suspend fun execute(appId: Int): AppResult<Stale<List<NewsArticle>>> =
        newsRepository.getNewsForGame(appId)
}
