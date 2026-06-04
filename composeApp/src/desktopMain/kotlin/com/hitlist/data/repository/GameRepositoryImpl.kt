package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.mapper.toAppResult
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.data.remote.GameMetadataSource
import com.hitlist.data.remote.GameRankingSource
import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.PlayerCountSource
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.usecase.GetRankedGamesUseCaseImpl

class GameRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val rankingSource: GameRankingSource,
    private val playerCountSource: PlayerCountSource,
    private val metadataSource: GameMetadataSource,
    private val reviewSource: GameReviewSource,
    private val dealsSource: GameDealsSource
) : GameRepository {

    override suspend fun getRankedGames(): AppResult<Pair<List<RankedGame>, Boolean>> {
        val cached = localDataSource.getRankedGames()
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.LIVE_PLAYERS_TTL_MS)) {
            return AppResult.Success(cached.first to false)
        }

        val freshResult = runCatching { fetchFreshRanking() }.toAppResult()

        return when (freshResult) {
            is AppResult.Success -> {
                val previous = cached?.first ?: emptyList()
                val withTrending = GetRankedGamesUseCaseImpl.markTrending(freshResult.data, previous)
                localDataSource.saveRankedGames(withTrending)
                AppResult.Success(withTrending to false)
            }
            is AppResult.Failure -> {
                if (cached != null) AppResult.Success(cached.first to true)
                else freshResult
            }
        }
    }

    private suspend fun fetchFreshRanking(): List<RankedGame> {
        val seeds = rankingSource.getTopGames()
        val maxPlayers = seeds.maxOfOrNull { it.currentPlayers } ?: 0
        return seeds.mapNotNull { seed ->
            val totalReviews = seed.positiveReviews + seed.negativeReviews
            val positiveRatio = if (totalReviews > 0) seed.positiveReviews.toDouble() / totalReviews else 0.0
            val score = GetRankedGamesUseCaseImpl.calculateScore(seed.currentPlayers, maxPlayers, positiveRatio, totalReviews)
            if (score == 0.0 && totalReviews < GetRankedGamesUseCaseImpl.MIN_REVIEWS_THRESHOLD) return@mapNotNull null
            RankedGame(
                steamAppId = seed.appId,
                name = seed.name,
                headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/${seed.appId}/header.jpg",
                score = score,
                currentPlayers = seed.currentPlayers,
                positiveRatio = positiveRatio,
                reviewScoreDesc = GetRankedGamesUseCaseImpl.describeReviewScore(seed.positiveReviews, seed.negativeReviews),
                totalReviews = totalReviews,
                genres = seed.genres,
                isTrending = false
            )
        }.sortedByDescending { it.score }
    }

    override suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail> {
        val cached = localDataSource.getGameDetail(appId)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.METADATA_TTL_MS)) {
            return AppResult.Success(cached.first)
        }

        return runCatching {
            val metadata = metadataSource.getGameMetadata(appId)
                ?: throw Exception("Game details not available for appId=$appId")
            val review = reviewSource.getGameReviews(appId)
            val deals = dealsSource.getDeals(name)
            val totalReviews = review?.totalReviews ?: 0
            val positiveRatio = if (totalReviews > 0 && review != null)
                review.totalPositive.toDouble() / totalReviews
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
        }.toAppResult()
    }
}
