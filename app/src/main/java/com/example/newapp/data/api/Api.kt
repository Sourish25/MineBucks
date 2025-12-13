package com.example.newapp.data.api

import com.example.newapp.data.model.CurrencyResponse
import com.example.newapp.data.model.ModrinthPayout
import com.example.newapp.data.model.ModrinthUser
import com.example.newapp.data.model.ModrinthProject
import com.example.newapp.data.model.ModrinthAnalytics
import com.example.newapp.data.model.ModrinthRevenueAnalyticsResponse
import com.example.newapp.data.model.ModrinthPayoutHistory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface ModrinthService {
    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") token: String
    ): ModrinthUser

    // Endpoint A: The list of transactions
    @GET("user/{id}/payouts")
    suspend fun getPayouts(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): List<ModrinthPayout>

    // Endpoint B: The summary with all_time revenue
    @GET("user/{id}/payout_history")
    suspend fun getPayoutHistory(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): ModrinthPayoutHistory

    @GET("user/{id}/projects")
    suspend fun getProjects(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): List<ModrinthProject>

    @GET("project/{slug}/analytics")
    suspend fun getProjectAnalytics(
        @Header("Authorization") token: String,
        @Path("slug") slug: String
    ): ModrinthAnalytics
}

interface ModrinthServiceV3 {
    @GET("analytics/revenue")
    suspend fun getRevenueAnalytics(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): Map<String, Map<String, Double>>
}



interface CurrencyService {
    @GET("latest")
    suspend fun getRates(
        @Query("from") from: String = "USD",
        @Query("to") to: String
    ): CurrencyResponse
}
