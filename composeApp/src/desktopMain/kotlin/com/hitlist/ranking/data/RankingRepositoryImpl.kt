package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.data.RankingCacheSource
import com.hitlist.common.data.toAppResult
import com.hitlist.common.domain.AppResult
import com.hitlist.ranking.domain.RankedGame
import com.hitlist.ranking.domain.RankingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class RankingRepositoryImpl(
    private val rankingCacheSource: RankingCacheSource,
    private val rankingSource: CombinedRankingSource
) : RankingRepository {

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
        val cached = rankingCacheSource.getRankedGames()
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
                val withTrending = markTrending(fresh.data.games, previous)
                rankingCacheSource.saveRankedGames(withTrending)
                RankingPoll(
                    AppResult.Success(withTrending to false),
                    nextDelayFromSteam(fresh.data.lastUpdate)
                )
            }
            is AppResult.Failure -> {
                if (cached != null) RankingPoll(AppResult.Success(cached.first to true), RETRY_INTERVAL_MS)
                else RankingPoll(fresh, RETRY_INTERVAL_MS)
            }
        }
    }

    private data class FreshRanking(val games: List<RankedGame>, val lastUpdate: Long)

    private suspend fun fetchFreshRanking(): FreshRanking {
        val combined = rankingSource.getCombinedRanking()
        val maxPlayers = combined.entries.maxOfOrNull { it.concurrentPlayers } ?: 0
        val games = combined.entries.mapNotNull { entry ->
            val totalReviews = entry.positiveReviews + entry.negativeReviews
            val positiveRatio = if (totalReviews > 0) entry.positiveReviews.toDouble() / totalReviews else 0.0
            val score = calculateScore(entry.concurrentPlayers, maxPlayers, positiveRatio, totalReviews)
            if (score == 0.0 && totalReviews < MIN_REVIEWS_THRESHOLD) return@mapNotNull null
            RankedGame(
                steamAppId = entry.appId,
                name = entry.name,
                headerImageUrl = entry.headerImageUrl,
                score = score,
                currentPlayers = entry.concurrentPlayers,
                positiveRatio = positiveRatio,
                reviewScoreDesc = describeReviewScore(entry.positiveReviews, entry.negativeReviews),
                totalReviews = totalReviews,
                genres = entry.genres,
                isTrending = false
            )
        }.sortedByDescending { it.score }
        return FreshRanking(games, combined.lastUpdate)
    }

    private fun calculateScore(
        currentPlayers: Int,
        maxPlayersInDataset: Int,
        positiveRatio: Double,
        totalReviews: Int
    ): Double {
        if (totalReviews < MIN_REVIEWS_THRESHOLD) return 0.0
        val trendScore = if (maxPlayersInDataset == 0) 0.0
                         else currentPlayers.toDouble() / maxPlayersInDataset
        return TREND_WEIGHT * trendScore + RATING_WEIGHT * positiveRatio
    }

    private fun markTrending(current: List<RankedGame>, previous: List<RankedGame>): List<RankedGame> {
        val prevPositions = previous.mapIndexed { idx, g -> g.steamAppId to idx }.toMap()
        return current.mapIndexed { currentIdx, game ->
            val prevIdx = prevPositions[game.steamAppId]
            game.copy(isTrending = prevIdx != null && currentIdx < prevIdx)
        }
    }

    private fun describeReviewScore(positive: Int, negative: Int): String {
        val total = positive + negative
        if (total < 10) return ""
        val ratio = positive.toDouble() / total
        return when {
            ratio >= 0.95 && total >= 500 -> "Overwhelmingly Positive"
            ratio >= 0.80 -> "Very Positive"
            ratio >= 0.70 -> "Mostly Positive"
            ratio >= 0.40 -> "Mixed"
            ratio >= 0.20 -> "Mostly Negative"
            else -> "Overwhelmingly Negative"
        }
    }

    private fun nextDelayFromSteam(lastUpdateSeconds: Long): Long {
        val nextAtMs = lastUpdateSeconds * 1000 + STEAM_REFRESH_PERIOD_MS + POLL_JITTER_MS
        val deltaMs = nextAtMs - System.currentTimeMillis()
        return deltaMs.coerceIn(MIN_POLL_INTERVAL_MS, STEAM_REFRESH_PERIOD_MS + POLL_JITTER_MS)
    }

    companion object {
        private const val TREND_WEIGHT = 0.6
        private const val RATING_WEIGHT = 0.4
        private const val MIN_REVIEWS_THRESHOLD = 50
        private const val STEAM_REFRESH_PERIOD_MS = 60 * 1000L
        private const val POLL_JITTER_MS = 5 * 1000L
        private const val MIN_POLL_INTERVAL_MS = 20 * 1000L
        private const val RETRY_INTERVAL_MS = 20 * 1000L
    }
}
