package com.hitlist.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hitlist.data.local.LocalDataSourceImpl
import com.hitlist.data.remote.cheapshark.CheapSharkProxy
import com.hitlist.data.remote.newsapi.NewsApiProxy
import com.hitlist.data.remote.steamcharts.SteamChartsProxy
import com.hitlist.data.remote.steamnews.SteamNewsProxy
import com.hitlist.data.remote.steamspy.SteamSpyProxy
import com.hitlist.data.remote.steamstore.SteamStoreProxy
import com.hitlist.data.remote.steamweb.SteamWebProxy
import com.hitlist.data.repository.DealsRepositoryImpl
import com.hitlist.data.repository.GameRepositoryImpl
import com.hitlist.data.repository.NewsRepositoryImpl
import com.hitlist.data.repository.RankingSourceBroker
import com.hitlist.domain.usecase.GetGameDetailUseCaseImpl
import com.hitlist.domain.usecase.GetGameNewsUseCaseImpl
import com.hitlist.domain.usecase.GetRankedGamesUseCaseImpl
import com.hitlist.presentation.detail.DetailViewModel
import com.hitlist.presentation.news.NewsViewModel
import com.hitlist.presentation.ranking.RankingViewModel
import java.io.File
import java.util.Properties

object HitListDependencyInjector {

    private val newsApiKey: String by lazy {
        val props = Properties()
        val file = File("local.properties")
        if (file.exists()) props.load(file.inputStream())
        props.getProperty("NEWS_API_KEY", "")
    }

    private val localDataSource = LocalDataSourceImpl()

    private val steamChartsProxy = SteamChartsProxy.create()
    private val steamSpyProxy = SteamSpyProxy.create()
    private val steamWebProxy = SteamWebProxy.create()
    private val steamStoreProxy = SteamStoreProxy.create()
    private val cheapSharkProxy = CheapSharkProxy.create()
    private val steamNewsProxy = SteamNewsProxy.create()
    private val newsApiProxy by lazy { NewsApiProxy.create(newsApiKey) }

    private val rankingSourceBroker = RankingSourceBroker(
        localDataSource,
        liveRankingSource = steamChartsProxy,
        rankingMetadataSource = steamSpyProxy
    )

    private val gameRepository = GameRepositoryImpl(
        localDataSource,
        rankingSource = rankingSourceBroker,
        playerCountSource = steamWebProxy,
        metadataSource = steamStoreProxy,
        reviewSource = steamStoreProxy,
        dealsSource = cheapSharkProxy
    )
    private val newsRepository by lazy {
        NewsRepositoryImpl(localDataSource, newsApiProxy, steamNewsProxy)
    }
    private val dealsRepository = DealsRepositoryImpl(localDataSource, cheapSharkProxy)

    private val getRankedGamesUseCase = GetRankedGamesUseCaseImpl(gameRepository)
    private val getGameDetailUseCase = GetGameDetailUseCaseImpl(gameRepository)
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
        viewModel { NewsViewModel(getGameNewsUseCase) }
}
