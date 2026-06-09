package com.hitlist.data.repository

import com.hitlist.data.local.CachePolicy
import com.hitlist.data.local.LocalDataSource
import com.hitlist.data.mapper.toAppResult
import com.hitlist.data.remote.GameDealsSource
import com.hitlist.data.remote.GameMetadataSeed
import com.hitlist.data.remote.GameMetadataSource
import com.hitlist.data.remote.GameReviewSource
import com.hitlist.data.remote.LiveRankingSource
import com.hitlist.data.remote.PlayerCountSource
import com.hitlist.data.remote.RankingMetadataSource
import com.hitlist.domain.entity.GameDetail
import com.hitlist.domain.entity.RankedGame
import com.hitlist.domain.repository.GameRepository
import com.hitlist.domain.result.AppResult
import com.hitlist.domain.util.RankingCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class GameRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val liveRankingSource: LiveRankingSource,
    private val rankingMetadataSource: RankingMetadataSource,
    private val playerCountSource: PlayerCountSource,
    private val metadataSource: GameMetadataSource,
    private val reviewSource: GameReviewSource,
    private val dealsSource: GameDealsSource
) : GameRepository {

    override fun observeRankedGames(): Flow<AppResult<Pair<List<RankedGame>, Boolean>>> = flow {
        while (coroutineContext.isActive) {
            val poll = pollRanking()
            emit(poll.result)
            delay(poll.nextDelayMs)
        }
    }

    private data class RankingPoll(
        val result: AppResult<Pair<List<RankedGame>, Boolean>>,
        val nextDelayMs: Long
    )

    private suspend fun pollRanking(): RankingPoll {
        val cached = localDataSource.getRankedGames()
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.LIVE_PLAYERS_TTL_MS)) {
            val remaining = cached.second + CachePolicy.LIVE_PLAYERS_TTL_MS - System.currentTimeMillis()
            return RankingPoll(
                AppResult.Success(cached.first to false),
                remaining.coerceIn(MIN_POLL_INTERVAL_MS, CachePolicy.LIVE_PLAYERS_TTL_MS)
            )
        }

        return when (val fresh = runCatching { fetchFreshRanking() }.toAppResult()) {
            is AppResult.Success -> {
                val previous = cached?.first ?: emptyList()
                val withTrending = RankingCalculator.markTrending(fresh.data.games, previous)
                localDataSource.saveRankedGames(withTrending)
                RankingPoll(AppResult.Success(withTrending to false), nextDelayFromSteam(fresh.data.lastUpdate))
            }
            is AppResult.Failure -> {
                if (cached != null) RankingPoll(AppResult.Success(cached.first to true), RETRY_INTERVAL_MS)
                else RankingPoll(fresh, RETRY_INTERVAL_MS)
            }
        }
    }

    private data class FreshRanking(val games: List<RankedGame>, val lastUpdate: Long)

    private suspend fun fetchFreshRanking(): FreshRanking {
        val live = liveRankingSource.getLiveRanking()
        val metadata = loadRankingMetadata(live.entries.map { it.appId })
        val maxPlayers = live.entries.maxOfOrNull { it.concurrentPlayers } ?: 0
        val games = live.entries.mapNotNull { entry ->
            val seed = metadata[entry.appId] ?: return@mapNotNull null
            val totalReviews = seed.positiveReviews + seed.negativeReviews
            val positiveRatio = if (totalReviews > 0) seed.positiveReviews.toDouble() / totalReviews else 0.0
            val score = RankingCalculator.calculateScore(entry.concurrentPlayers, maxPlayers, positiveRatio, totalReviews)
            if (score == 0.0 && totalReviews < RankingCalculator.MIN_REVIEWS_THRESHOLD) return@mapNotNull null
            RankedGame(
                steamAppId = entry.appId,
                name = seed.name,
                headerImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/${entry.appId}/header.jpg",
                score = score,
                currentPlayers = entry.concurrentPlayers,
                positiveRatio = positiveRatio,
                reviewScoreDesc = RankingCalculator.describeReviewScore(seed.positiveReviews, seed.negativeReviews),
                totalReviews = totalReviews,
                genres = seed.genres,
                isTrending = false
            )
        }.sortedByDescending { it.score }
        return FreshRanking(games, live.lastUpdate)
    }

    private suspend fun loadRankingMetadata(appIds: List<Int>): Map<Int, GameMetadataSeed> {
        val cached = localDataSource.getRankingMetadata()
        val valid = cached != null && CachePolicy.isValid(cached.second, CachePolicy.SEED_LIST_TTL_MS)
        val base = if (valid) cached!!.first else rankingMetadataSource.getBulkMetadata()
        val cachedAt = if (valid) cached!!.second else System.currentTimeMillis()

        val missing = appIds.filterNot { base.containsKey(it) }
        val merged = if (missing.isEmpty()) base else base + fetchMissingMetadata(missing)
        localDataSource.saveRankingMetadata(merged, cachedAt)
        return merged
    }

    private suspend fun fetchMissingMetadata(appIds: List<Int>): Map<Int, GameMetadataSeed> = coroutineScope {
        appIds
            .map { id -> async { rankingMetadataSource.getMetadata(id) } }
            .awaitAll()
            .filterNotNull()
            .associateBy { it.appId }
    }

    private fun nextDelayFromSteam(lastUpdateSeconds: Long): Long {
        val nextAtMs = lastUpdateSeconds * 1000 + STEAM_REFRESH_PERIOD_MS + POLL_JITTER_MS
        val deltaMs = nextAtMs - System.currentTimeMillis()
        return deltaMs.coerceIn(MIN_POLL_INTERVAL_MS, STEAM_REFRESH_PERIOD_MS + POLL_JITTER_MS)
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

    companion object {
        private const val STEAM_REFRESH_PERIOD_MS = 60 * 1000L
        private const val POLL_JITTER_MS = 5 * 1000L
        private const val MIN_POLL_INTERVAL_MS = 20 * 1000L
        private const val RETRY_INTERVAL_MS = 20 * 1000L
    }
}
