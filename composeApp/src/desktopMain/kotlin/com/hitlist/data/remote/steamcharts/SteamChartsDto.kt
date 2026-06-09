package com.hitlist.data.remote.steamcharts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MostPlayedResponseDto(
    @SerialName("response") val response: MostPlayedDataDto
)

@Serializable
data class MostPlayedDataDto(
    @SerialName("last_update") val lastUpdate: Long = 0,
    @SerialName("ranks") val ranks: List<MostPlayedRankDto> = emptyList()
)

@Serializable
data class MostPlayedRankDto(
    @SerialName("rank") val rank: Int = 0,
    @SerialName("appid") val appId: Int = 0,
    @SerialName("concurrent_in_game") val concurrentInGame: Int = 0,
    @SerialName("peak_in_game") val peakInGame: Int = 0
)
