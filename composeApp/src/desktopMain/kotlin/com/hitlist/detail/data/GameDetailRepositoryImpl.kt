package com.hitlist.detail.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.data.toAppError
import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.detail.domain.GameDetail
import com.hitlist.detail.domain.GameDetailRepository

class GameDetailRepositoryImpl(
    private val gameDetailCacheSource: GameDetailCacheSource,
    private val playerCountSource: PlayerCountSource,
    private val metadataSource: GameMetadataSource,
    private val reviewSource: GameReviewSource,
    private val dealsSource: GameDealsSource
) : GameDetailRepository {

    override suspend fun getGameDetail(appId: Int, name: String): AppResult<Stale<GameDetail>> {
        val cached = gameDetailCacheSource.getGameDetail(appId)
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.METADATA_TTL_MS)) {
            return AppResult.Success(Stale(cached.first, isStale = false))
        }

        val fresh = runCatching { fetchFreshDetail(appId, name) }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { error ->
                if (error is GameNotFoundException) AppResult.Failure(AppError.NotFound)
                else AppResult.Failure(error.toAppError())
            }
        )

        return when (fresh) {
            is AppResult.Success -> AppResult.Success(Stale(fresh.data, isStale = false))
            is AppResult.Failure ->
                if (cached != null) AppResult.Success(Stale(cached.first, isStale = true))
                else fresh
        }
    }

    private suspend fun fetchFreshDetail(appId: Int, name: String): GameDetail {
        val metadata = metadataSource.getGameMetadata(appId) ?: throw GameNotFoundException(appId)
        val review = reviewSource.getGameReviews(appId)
        val deals = dealsSource.getDeals(name)
        val totalReviews = review?.totalReviews ?: 0
        val positiveRatio = if (review != null && totalReviews > 0)
            review.totalPositive.toDouble() / totalReviews
        else 0.0

        return GameDetail(
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
            reviewScoreDesc = if (review != null) review.reviewScoreDesc else "",
            totalReviews = totalReviews,
            deals = deals
        ).also { gameDetailCacheSource.saveGameDetail(it) }
    }
}

private class GameNotFoundException(appId: Int) : Exception("Game details not available for appId=$appId")
