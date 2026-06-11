package com.hitlist.detail.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.data.GameDetailCacheSource
import com.hitlist.common.data.toAppResult
import com.hitlist.common.domain.AppResult
import com.hitlist.detail.domain.GameDetail
import com.hitlist.detail.domain.GameDetailRepository

class GameDetailRepositoryImpl(
    private val gameDetailCacheSource: GameDetailCacheSource,
    private val playerCountSource: PlayerCountSource,
    private val metadataSource: GameMetadataSource,
    private val reviewSource: GameReviewSource,
    private val dealsSource: GameDealsSource
) : GameDetailRepository {

    override suspend fun getGameDetail(appId: Int, name: String): AppResult<GameDetail> {
        val cached = gameDetailCacheSource.getGameDetail(appId)
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
            ).also { gameDetailCacheSource.saveGameDetail(it) }
        }.toAppResult()
    }
}
