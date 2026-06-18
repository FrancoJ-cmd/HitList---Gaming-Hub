package com.hitlist.news.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.data.toAppResult
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.news.domain.NewsArticle
import com.hitlist.news.domain.NewsRepository

class NewsRepositoryImpl(
    private val newsCacheSource: NewsCacheSource,
    private val generalNewsSource: GeneralNewsSource,
    private val gameNewsSource: GameNewsSource
) : NewsRepository {

    override suspend fun getNews(query: String): AppResult<Stale<List<NewsArticle>>> =
        loadNews(cacheKey = query) { generalNewsSource.getNews(query) }

    override suspend fun getNewsForGame(appId: Int): AppResult<Stale<List<NewsArticle>>> =
        loadNews(cacheKey = gameNewsCacheKey(appId)) { gameNewsSource.getNewsForGame(appId) }

    private suspend fun loadNews(
        cacheKey: String,
        fetch: suspend () -> List<NewsArticle>
    ): AppResult<Stale<List<NewsArticle>>> {
        val cached = newsCacheSource.getNews(cacheKey)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.NEWS_TTL_MS)) {
            return AppResult.Success(Stale(cached.first, isStale = false))
        }

        val freshResult = runCatching {
            val articles = fetch()
            if (articles.isNotEmpty()) newsCacheSource.saveNews(cacheKey, articles)
            articles
        }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> AppResult.Success(Stale(freshResult.data, isStale = false))
            is AppResult.Failure ->
                if (cached != null) AppResult.Success(Stale(cached.first, isStale = true))
                else freshResult
        }
    }

    private fun gameNewsCacheKey(appId: Int) = "steam_$appId"
}
