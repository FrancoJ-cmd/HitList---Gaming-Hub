package com.hitlist.detail.data

import com.hitlist.common.data.CacheEntry
import com.hitlist.common.data.JsonFileStore
import com.hitlist.detail.domain.GameDetail

class GameDetailFileCache(private val store: JsonFileStore) : GameDetailCacheSource {

    override fun getGameDetail(appId: Int): Pair<GameDetail, Long>? =
        store.read<CacheEntry<SerializableGameDetail>>(detailFile(appId))
            ?.let { entry -> entry.data.toDomain() to entry.cachedAt }

    override fun saveGameDetail(detail: GameDetail) {
        store.write(
            detailFile(detail.steamAppId),
            CacheEntry(System.currentTimeMillis(), SerializableGameDetail.fromDomain(detail))
        )
    }

    private fun detailFile(appId: Int) = store.file("game_detail_$appId.json")
}