package com.example.newapp.data.repository

import com.example.newapp.data.api.CurrencyService
import com.example.newapp.data.api.ModrinthService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import com.example.newapp.data.model.ModrinthProject
import com.example.newapp.data.model.ModrinthAnalytics
import com.example.newapp.data.api.ModrinthServiceV3
import com.example.newapp.data.model.ModrinthRevenueAnalyticsResponse
import java.util.Locale



class RevenueRepository(
    private val dataSource: RevenueDataSource, // Changed from Service
    private val dataStoreManager: DataStoreManager,
    private val revenueDao: com.example.newapp.data.database.RevenueDao // NEW
) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Currency Service remains here as it is shared logic
    private val currencyRetrofit = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.app/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        
    private val currencyService = currencyRetrofit.create(CurrencyService::class.java)

    data class RevenueResult(
        val totalAmount: Double,
        val last24Hours: Double = 0.0
    )


    suspend fun getModrinthRevenue(): RevenueResult {
        return dataSource.getModrinthRevenue()
    }
    
    suspend fun getCurseForgeRevenueInUSD(): Double {
        return dataSource.getCurseForgeRevenueInUSD()
    }

    // DEBUG FUNCTION
    suspend fun getRawModrinthResponse(): String {
        return if (dataSource is RealRevenueDataSource) {
             dataSource.getDebugLog()
        } else {
            "Using Mock Data Source"
        }
    }

    suspend fun convertCurrency(amountUSD: Double): Double {
        val target = dataStoreManager.targetCurrency.first()
        if (target == "USD") return amountUSD
        
        return try {
            val response = currencyService.getRates(to = target)
            val rate = response.rates[target] ?: 1.0
            amountUSD * rate
            amountUSD * rate
        } catch (e: Exception) {
            android.util.Log.e("CurrencyError", "Failed to convert $amountUSD USD to $target: ${e.message}")
            amountUSD // Fallback
        }
    }
    suspend fun getExchangeRate(targetCurrency: String): Double {
        if (targetCurrency == "USD") return 1.0
        return try {
            val response = currencyService.getRates(to = targetCurrency)
            response.rates[targetCurrency] ?: 1.0
        } catch (e: Exception) {
            1.0
        }
    }
}
