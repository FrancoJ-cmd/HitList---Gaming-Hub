package com.hitlist.detail.domain

data class Deal(
    val storeName: String,
    val currentPrice: String,
    val retailPrice: String,
    val savingsPercent: Double,
    val cheapestEverPrice: String
)
