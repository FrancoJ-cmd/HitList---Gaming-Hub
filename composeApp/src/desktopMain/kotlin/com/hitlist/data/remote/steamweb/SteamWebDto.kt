package com.hitlist.data.remote.steamweb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentPlayersResponseDto(
    @SerialName("response") val response: CurrentPlayersDto
)

@Serializable
data class CurrentPlayersDto(
    @SerialName("player_count") val playerCount: Int = 0,
    @SerialName("result") val result: Int = 0
)
