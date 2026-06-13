package com.hitlist.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hitlist.data.local.LocalDataSourceImpl
import com.hitlist.data.remote.cheapshark.CheapSharkProxy
import com.hitlist.data.remote.newsapi.NewsApiProxy
import com.hitlist.data.remote.steamnews.SteamNewsProxy
import com.hitlist.data.remote.steamspy.SteamSpyProxy
import com.hitlist.data.remote.steamstore.SteamStoreMetadataProxy
import com.hitlist.data.remote.steamstore.SteamStoreReviewProxy
import com.hitlist.data.remote.steamweb.SteamWebProxy
import com.hitlist.data.repository.DealsRepositoryImpl
import com.hitlist.data.repository.GameRepositoryImpl
import com.hitlist.data.repository.NewsRepositoryImpl
import com.hitlist.domain.usecase.GetGameDealsUseCase
import com.hitlist.domain.usecase.GetGameDealsUseCaseImpl
import com.hitlist.domain.usecase.GetGameDetailUseCaseImpl
import com.hitlist.domain.usecase.GetGameNewsUseCaseImpl
import com.hitlist.domain.usecase.GetRankedGamesUseCaseImpl
import com.hitlist.presentation.detail.DetailViewModel
import com.hitlist.presentation.news.NewsViewModel
import com.hitlist.presentation.ranking.RankingViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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

    private fun buildClient(host: String, timeoutMs: Long = 10_000L): HttpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(DefaultRequest) {
            url { protocol = URLProtocol.HTTPS; this.host = host }
        }
        install(HttpTimeout) { requestTimeoutMillis = timeoutMs }
    }

    private val steamSpyProxy    = SteamSpyProxy(buildClient("steamspy.com"))
    private val steamWebProxy    = SteamWebProxy(buildClient("api.steampowered.com", 5_000L))
    private val steamStoreMetadataProxy = SteamStoreMetadataProxy(buildClient("store.steampowered.com"))
    private val steamStoreReviewProxy   = SteamStoreReviewProxy(buildClient("store.steampowered.com"))
    private val cheapSharkProxy  = CheapSharkProxy(buildClient("www.cheapshark.com"))
    private val steamNewsProxy   = SteamNewsProxy(buildClient("api.steampowered.com"))
    private val newsApiProxy     by lazy { NewsApiProxy(buildClient("newsapi.org"), newsApiKey) }

    private val gameRepository = GameRepositoryImpl(
        localDataSource,
        rankingSource = steamSpyProxy,
        playerCountSource = steamWebProxy,
        metadataSource = steamStoreMetadataProxy,
        reviewSource = steamStoreReviewProxy,
        dealsSource = cheapSharkProxy
    )
    private val newsRepository by lazy {
        NewsRepositoryImpl(localDataSource, newsApiProxy, steamNewsProxy)
    }
    private val dealsRepository = DealsRepositoryImpl(localDataSource, cheapSharkProxy)

    private val getRankedGamesUseCase = GetRankedGamesUseCaseImpl(gameRepository)
    private val getGameDetailUseCase = GetGameDetailUseCaseImpl(gameRepository)
    private val getGameNewsUseCase by lazy { GetGameNewsUseCaseImpl(newsRepository) }
    private val getGameDealsUseCase: GetGameDealsUseCase = GetGameDealsUseCaseImpl(dealsRepository)

    val isNewsApiConfigured: Boolean get() = newsApiKey.isNotBlank()
    val gameDealsUseCase: GetGameDealsUseCase get() = getGameDealsUseCase

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
