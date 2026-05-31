package com.hitlist.data.remote.steamnews

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SteamNewsResponseDto(
    @SerialName("appnews") val appNews: AppNewsDto? = null
)

@Serializable
data class AppNewsDto(
    @SerialName("newsitems") val newsItems: List<SteamNewsItemDto> = emptyList()
)

@Serializable
data class SteamNewsItemDto(
    val title: String = "",
    val url: String = "",
    val author: String = "",
    val contents: String = "",
    @SerialName("feedlabel") val feedLabel: String = "",
    val date: Long = 0L
)
