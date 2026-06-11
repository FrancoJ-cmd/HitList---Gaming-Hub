package com.hitlist.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hitlist.common.data.LocalDataSourceImpl
import com.hitlist.detail.data.GameDetailRepositoryImpl
import com.hitlist.detail.data.cheapshark.CheapSharkProxy
import com.hitlist.detail.data.steamstore.SteamStoreMetadataProxy
import com.hitlist.detail.data.steamstore.SteamStoreReviewProxy
import com.hitlist.detail.data.steamweb.SteamWebProxy
import com.hitlist.detail.domain.GetGameDetailUseCaseImpl
import com.hitlist.detail.presentation.DetailViewModel
import com.hitlist.news.data.NewsRepositoryImpl
import com.hitlist.news.data.newsapi.NewsApiProxy
import com.hitlist.news.data.steamnews.SteamNewsProxy
import com.hitlist.news.domain.GetGameNewsUseCaseImpl
import com.hitlist.news.domain.GetGeneralNewsUseCaseImpl
import com.hitlist.news.presentation.NewsViewModel
import com.hitlist.ranking.data.CombinedRankingSourceImpl
import com.hitlist.ranking.data.RankingRepositoryImpl
import com.hitlist.ranking.data.steamcharts.SteamChartsProxy
import com.hitlist.ranking.data.steamspy.SteamSpyProxy
import com.hitlist.ranking.domain.GetRankedGamesUseCaseImpl
import com.hitlist.ranking.presentation.RankingViewModel
import java.io.File
import java.util.Properties

object HitListDependencyInjector {
    private val newsApiKey: String by lazy {
        System.getenv("NEWS_API_KEY")?.takeIf { it.isNotBlank() }?.let { return@lazy it }

        val props = Properties()
        val candidates = listOf(
            File("local.properties"),
            File("../local.properties")
        )

        val found = candidates.firstOrNull { it.exists() }
        if (found != null) props.load(found.inputStream())

        props.getProperty("NEWS_API_KEY", "")
    }

    private val localDataSource = LocalDataSourceImpl()

    private val steamChartsProxy = SteamChartsProxy.create()
    private val steamSpyProxy = SteamSpyProxy.create()
    private val steamWebProxy = SteamWebProxy.create()
    private val steamStoreMetadataProxy = SteamStoreMetadataProxy.create()
    private val steamStoreReviewProxy = SteamStoreReviewProxy.create()
    private val cheapSharkProxy = CheapSharkProxy.create()
    private val steamNewsProxy = SteamNewsProxy.create()
    private val newsApiProxy by lazy { NewsApiProxy.create(newsApiKey) }

    private val combinedRankingSource = CombinedRankingSourceImpl(
        liveRankingSource = steamChartsProxy,
        rankingMetadataSource = steamSpyProxy
    )

    private val rankingRepository = RankingRepositoryImpl(
        rankingCacheSource = localDataSource,
        rankingSource = combinedRankingSource
    )
    private val gameDetailRepository = GameDetailRepositoryImpl(
        gameDetailCacheSource = localDataSource,
        playerCountSource = steamWebProxy,
        metadataSource = steamStoreMetadataProxy,
        reviewSource = steamStoreReviewProxy,
        dealsSource = cheapSharkProxy
    )
    private val newsRepository by lazy {
        NewsRepositoryImpl(localDataSource, newsApiProxy, steamNewsProxy)
    }

    private val getRankedGamesUseCase = GetRankedGamesUseCaseImpl(rankingRepository)
    private val getGameDetailUseCase = GetGameDetailUseCaseImpl(gameDetailRepository)
    private val getGeneralNewsUseCase by lazy { GetGeneralNewsUseCaseImpl(newsRepository) }
    private val getGameNewsUseCase by lazy { GetGameNewsUseCaseImpl(newsRepository) }

    val isNewsApiConfigured: Boolean get() = newsApiKey.isNotBlank()

    @Composable
    fun getRankingViewModel(): RankingViewModel =
        viewModel { RankingViewModel(getRankedGamesUseCase) }

    @Composable
    fun getDetailViewModel(): DetailViewModel =
        viewModel { DetailViewModel(getGameDetailUseCase) }

    @Composable
    fun getNewsViewModel(): NewsViewModel =
        viewModel { NewsViewModel(getGeneralNewsUseCase, getGameNewsUseCase) }
}
