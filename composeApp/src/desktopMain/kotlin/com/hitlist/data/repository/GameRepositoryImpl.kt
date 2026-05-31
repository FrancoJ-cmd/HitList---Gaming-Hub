package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.data.remote.GameMetadataSource
import com.hitlist.data.remote.GameRankingSource
import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.PlayerCountSource
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.usecase.GetRankedGamesUseCaseImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GameRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val rankingSource: GameRankingSource,
    private val playerCountSource: PlayerCountSource,
    private val metadataSource: GameMetadataSource,
    private val reviewSource: GameReviewSource,
    private val dealsSource: GameDealsSource
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
            if (cached != null) cached.first to true else throw exception
        }
    }

    private suspend fun fetchFreshRanking(): List<RankedGame> = coroutineScope {
        val seeds = rankingSource.getTopGames()

        val playerCountsDeferred = seeds.map { seed ->
            async { seed.appId to playerCountSource.getCurrentPlayers(seed.appId) }
        }
        val reviewsDeferred = seeds.map { seed ->
            async { seed.appId to reviewSource.getGameReviews(seed.appId) }
        }

        val playerCounts = playerCountsDeferred.awaitAll().toMap()
        val reviews = reviewsDeferred.awaitAll().toMap()
        val maxPlayers = playerCounts.values.maxOrNull() ?: 0

        seeds.mapNotNull { seed ->
            val players = playerCounts[seed.appId] ?: 0
            val review = reviews[seed.appId]
            val totalReviews = review?.totalReviews ?: 0
            val positiveRatio = if (totalReviews > 0)
                review!!.totalPositive.toDouble() / totalReviews
            else 0.0

            val score = GetRankedGamesUseCaseImpl.calculateScore(
                players, maxPlayers, positiveRatio, totalReviews
            )
            if (score == 0.0 && totalReviews < GetRankedGamesUseCaseImpl.MIN_REVIEWS_THRESHOLD) return@mapNotNull null

            RankedGame(
                steamAppId = seed.appId,
                name = seed.name,
                headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/${seed.appId}/header.jpg",
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
            val metadata = metadataSource.getGameMetadata(appId)
                ?: throw Exception("Game details not available for appId=$appId")
            val review = reviewSource.getGameReviews(appId)
            val deals = dealsSource.getDeals(name)

            val totalReviews = review?.totalReviews ?: 0
            val positiveRatio = if (totalReviews > 0)
                review!!.totalPositive.toDouble() / totalReviews
            else 0.0

            GameDetail(
                steamAppId = appId,
                name = metadata.name,
                shortDescription = metadata.shortDescription,
                headerImageUrl = metadata.headerImageUrl,
                screenshots = metadata.screenshots,
                metacriticScore = metadata.metacriticScore,
                genres = metadata.genres,
                developers = metadata.developers,
                releaseDate = metadata.releaseDate,
                isFree = metadata.isFree,
                currentPlayers = playerCountSource.getCurrentPlayers(appId),
                positiveRatio = positiveRatio,
                reviewScoreDesc = review?.reviewScoreDesc ?: "",
                totalReviews = totalReviews,
                deals = deals
            ).also { localDataSource.saveGameDetail(it) }
        }
    }
}
