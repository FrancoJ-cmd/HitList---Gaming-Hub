package com.hitlist.news.data

import com.hitlist.common.data.CacheEntry
import com.hitlist.common.data.JsonFileStore
import com.hitlist.news.domain.NewsArticle

class NewsFileCache(private val store: JsonFileStore) : NewsCacheSource {

    override fun getNews(query: String): Pair<List<NewsArticle>, Long>? =
        store.read<CacheEntry<List<SerializableNewsArticle>>>(newsFile(query))
            ?.let { entry -> entry.data.map { it.toDomain() } to entry.cachedAt }

    override fun saveNews(query: String, articles: List<NewsArticle>) {
        store.write(
            newsFile(query),
            CacheEntry(System.currentTimeMillis(), articles.map { SerializableNewsArticle.fromDomain(it) })
        )
    }

    private fun newsFile(query: String) = store.file("news_${query.hashCode()}.json")
}