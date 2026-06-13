package com.hitlist.data.remote.cheapshark

import com.hitlist.data.remote.GameDealsSource
import com.hitlist.domain.entity.Deal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class CheapSharkProxy(private val client: HttpClient) : GameDealsSource {

    override suspend fun getDeals(gameName: String): List<Deal> = runCatching {
        val encoded = gameName.encodeURLParameter()
        val searchResults = client.get("/api/1.0/games?title=$encoded&limit=3")
            .body<List<CheapSharkSearchResultDto>>()

        val gameId = searchResults.firstOrNull()?.gameId ?: return@runCatching emptyList()

        val game = client.get("/api/1.0/games?id=$gameId").body<CheapSharkGameDto>()
        val cheapestEver = game.cheapestPriceEver?.price ?: "N/A"

        game.deals.map { deal ->
            Deal(
                storeName = storeIdToName(deal.storeId),
                currentPrice = deal.price,
                retailPrice = deal.retailPrice,
                savingsPercent = deal.savings.toDoubleOrNull() ?: 0.0,
                cheapestEverPrice = cheapestEver
            )
        }
    }.getOrDefault(emptyList())

    private fun storeIdToName(storeId: String): String =
        STORE_NAMES[storeId] ?: "Store $storeId"

    private companion object {
        val STORE_NAMES = mapOf(
            "1"  to "Steam",
            "25" to "GOG",
            "11" to "Humble Store",
            "7"  to "GamersGate",
            "6"  to "Dealgame"
        )
    }
}
