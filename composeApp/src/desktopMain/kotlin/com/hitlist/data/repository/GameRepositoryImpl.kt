package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.remote.cheapshark.CheapSharkProxy
import com.hitlist.data.remote.steamspy.SteamSpyProxy
import com.hitlist.data.remote.steamstore.SteamStoreProxy
import com.hitlist.data.remote.steamweb.SteamWebProxy
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.usecase.GetRankedGamesUseCaseImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GameRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val steamSpyProxy: SteamSpyProxy,
    private val steamWebProxy: SteamWebProxy,
    private val steamStoreProxy: SteamStoreProxy,
    private val cheapSharkProxy: CheapSharkProxy
) : GameRepository {

    override suspend fun getRankedGames(): Result<Pair<List<RankedGame>, Boolean>> {
        val cached = localDataSource.getRankedGames()
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.LIVE_PLAYERS_TTL_MS)) {
            return Result.success(cached.first to false)
        }

        return runCatching {
            val games = fetchFreshRanking()
            val previous = cached?.first ?: emptyList()
            val withTrending = GetRankedGamesUseCaseImpl.markTrending(games, previous)
            localDataSource.saveRankedGames(withTrending)
            withTrending to false
        }.recoverCatching { exception ->
            if (cached != null) {
                cached.first to true
            } else {
                throw exception
            }
        }
    }

    private suspend fun fetchFreshRanking(): List<RankedGame> = coroutineScope {
        val seedGames = steamSpyProxy.getTop100Games()

        val playerCountsDeferred = seedGames.map { game ->
            async { game.appId to steamWebProxy.getCurrentPlayers(game.appId) }
        }
        val reviewsDeferred = seedGames.map { game ->
            async { game.appId to steamStoreProxy.getAppReviews(game.appId) }
        }

        val playerCounts = playerCountsDeferred.awaitAll().toMap()
        val reviews = reviewsDeferred.awaitAll().toMap()

        val maxPlayers = playerCounts.values.maxOrNull() ?: 0

        seedGames.mapNotNull { game ->
            val players = playerCounts[game.appId] ?: 0
            val review = reviews[game.appId]
            val totalReviews = review?.totalReviews ?: 0
            val positiveRatio = if (totalReviews > 0)
                (review?.totalPositive ?: 0).toDouble() / totalReviews
            else 0.0

            val score = GetRankedGamesUseCaseImpl.calculateScore(
                players, maxPlayers, positiveRatio, totalReviews
            )
            if (score == 0.0 && totalReviews < GetRankedGamesUseCaseImpl.MIN_REVIEWS_THRESHOLD) return@mapNotNull null

            RankedGame(
                steamAppId = game.appId,
                name = game.name,
                headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/${game.appId}/header.jpg",
                score = score,
                currentPlayers = players,
                positiveRatio = positiveRatio,
                reviewScoreDesc = review?.reviewScoreDesc ?: "",
                totalReviews = totalReviews,
                genres = emptyList(),
                isTrending = false
            )
        }.sortedByDescending { it.score }
    }

    override suspend fun getGameDetail(appId: Int, name: String): Result<GameDetail> {
        val cached = localDataSource.getGameDetail(appId)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.METADATA_TTL_MS)) {
            return Result.success(cached.first)
        }

        return runCatching {
            val appDetails = steamStoreProxy.getAppDetails(appId)
                ?: throw Exception("Game details not available for appId=$appId")
            val review = steamStoreProxy.getAppReviews(appId)
            val deals = cheapSharkProxy.getDeals(name)

            val totalReviews = review?.totalReviews ?: 0
            val positiveRatio = if (totalReviews > 0)
                (review?.totalPositive ?: 0).toDouble() / totalReviews
            else 0.0

            GameDetail(
                steamAppId = appId,
                name = appDetails.name,
                shortDescription = appDetails.shortDescription,
                headerImageUrl = appDetails.headerImage,
                screenshots = appDetails.screenshots.take(5).map { it.pathFull },
                metacriticScore = appDetails.metacritic?.score,
                genres = appDetails.genres.map { it.description },
                developers = appDetails.developers,
                releaseDate = appDetails.releaseDate?.date,
                isFree = appDetails.isFree,
                currentPlayers = steamWebProxy.getCurrentPlayers(appId),
                positiveRatio = positiveRatio,
                reviewScoreDesc = review?.reviewScoreDesc ?: "",
                totalReviews = totalReviews,
                deals = deals
            ).also { localDataSource.saveGameDetail(it) }
        }
    }
}
