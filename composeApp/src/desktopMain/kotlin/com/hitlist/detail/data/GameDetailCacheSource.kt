package com.hitlist.detail.data

import com.hitlist.detail.domain.GameDetail

interface GameDetailCacheSource {
    fun getGameDetail(appId: Int): Pair<GameDetail, Long>?
    fun saveGameDetail(detail: GameDetail)
}