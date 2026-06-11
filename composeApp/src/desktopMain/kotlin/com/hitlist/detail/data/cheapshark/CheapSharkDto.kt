package com.hitlist.detail.data.cheapshark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheapSharkSearchResultDto(
    @SerialName("gameID") val gameId: String = ""
)

@Serializable
data class CheapSharkGameDto(
    val deals: List<CheapSharkDealDto> = emptyList(),
    @SerialName("cheapestPriceEver") val cheapestPriceEver: CheapestPriceDto? = null
)

@Serializable
data class CheapSharkDealDto(
    @SerialName("storeID") val storeId: String = "",
    val price: String = "0.00",
    val retailPrice: String = "0.00",
    val savings: String = "0"
)

@Serializable
data class CheapestPriceDto(
    val price: String = "0.00"
)
