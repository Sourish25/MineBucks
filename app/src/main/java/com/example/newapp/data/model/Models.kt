package com.example.newapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthUser(
    val id: String,
    val username: String,
    val name: String? = null,
    val avatar_url: String? = null,
    val payout_data: ModrinthPayoutData? = null
)

@Serializable
data class ModrinthPayoutData(
    val balance: Double
)

@Serializable
data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String
)

@Serializable
data class ModrinthAnalytics(
    val revenue: Double,
    val downloads: Int,
    val views: Int
)

@Serializable
data class ModrinthRevenueAnalyticsResponse(
    val revenue: Double,
    val date: String
)

@Serializable
data class ModrinthPayoutHistory(
    val all_time: String, // API returns string "10.11..."
    val last_month: String,
    val payouts: List<ModrinthPayout>
)

@Serializable
data class ModrinthPayout(
    val id: String,
    val amount: Double, // In USD
    val created: String,
    val status: String
)

@Serializable
data class CurrencyResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)
