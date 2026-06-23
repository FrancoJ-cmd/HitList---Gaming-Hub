package com.hitlist.ranking.data

import com.hitlist.common.data.CachePolicy
import com.hitlist.common.data.toAppResult
import com.hitlist.common.domain.AppResult
import com.hitlist.common.domain.Stale
import com.hitlist.ranking.domain.RankedGame
import com.hitlist.ranking.domain.Ranking
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

    override fun observeRankedGames(): Flow<AppResult<Stale<Ranking>>> = flow {
        while (coroutineContext.isActive) {
            val poll = pollRanking()
            emit(poll.result)
            delay(poll.nextDelayMs)
        }
    }

    private data class RankingPoll(
        val result: AppResult<Stale<Ranking>>,
        val nextDelayMs: Long
    )

    private suspend fun pollRanking(): RankingPoll {
        val cached = rankingCacheSource.getRankedGames()
        if (cached != null && CachePolicy.isValid(cached.second, CachePolicy.LIVE_PLAYERS_TTL_MS)) {
            val remaining = cached.second + CachePolicy.LIVE_PLAYERS_TTL_MS - System.currentTimeMillis()
            return RankingPoll(
                AppResult.Success(Stale(Ranking(cached.first), isStale = false)),
                remaining.coerceIn(MIN_POLL_INTERVAL_MS, CachePolicy.LIVE_PLAYERS_TTL_MS)
            )
        }

        return when (val fresh = runCatching { fetchFreshRanking(cached?.first) }.toAppResult()) {
            is AppResult.Success -> {
                rankingCacheSource.saveRankedGames(fresh.data.ranking.games)
                RankingPoll(
                    AppResult.Success(Stale(fresh.data.ranking, isStale = false)),
                    nextDelayFromSteam(fresh.data.lastUpdate)
                )
            }
            is AppResult.Failure -> {
                if (cached != null) RankingPoll(AppResult.Success(Stale(Ranking(cached.first), isStale = true)), RETRY_INTERVAL_MS)
                else RankingPoll(fresh, RETRY_INTERVAL_MS)
            }
        }
    }

    private data class FreshRanking(val ranking: Ranking, val lastUpdate: Long)

    private suspend fun fetchFreshRanking(previous: List<RankedGame>?): FreshRanking {
        val combined = rankingSource.getCombinedRanking()
        val ranking = Ranking.from(combined.entries, previous?.let { Ranking(it) })
        return FreshRanking(ranking, combined.lastUpdate)
    }

    private fun nextDelayFromSteam(lastUpdateSeconds: Long): Long {
        val nextAtMs = lastUpdateSeconds * 1000 + STEAM_REFRESH_PERIOD_MS + POLL_JITTER_MS
        val deltaMs = nextAtMs - System.currentTimeMillis()
        return deltaMs.coerceIn(MIN_POLL_INTERVAL_MS, STEAM_REFRESH_PERIOD_MS + POLL_JITTER_MS)
    }

    companion object {
        private const val STEAM_REFRESH_PERIOD_MS = 60 * 1000L
        private const val POLL_JITTER_MS = 5 * 1000L
        private const val MIN_POLL_INTERVAL_MS = 20 * 1000L
        private const val RETRY_INTERVAL_MS = 20 * 1000L
    }
}
