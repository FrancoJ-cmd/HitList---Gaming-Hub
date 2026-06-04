package com.hitlist.data.remote.steamspy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SteamSpyGameDto(
    @SerialName("appid") val appId: Int,
    @SerialName("name") val name: String,
    @SerialName("ccu") val ccu: Int = 0,
    @SerialName("positive") val positive: Int = 0,
    @SerialName("negative") val negative: Int = 0,
    @SerialName("genre") val genre: String = ""
)
