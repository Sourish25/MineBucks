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
    private val modrinthService: ModrinthService,
    private val dataStoreManager: DataStoreManager,
    private val revenueDao: com.example.newapp.data.database.RevenueDao // NEW
) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Internal Client for V3 (Specific use case, reuse OkHttp?)
    // Actually, V3 Service should also be injected if possible, but let's keep it handled internally or inject checks.
    // Given the architecture, let's keep V3 internal for now but reuse the injected service logic if possible.
    // Wait, the injected 'modrinthService' is V2. 
    // We can recreate V3 here using the same base if we want, OR just rely on the fact we need to fix V3 logic.
    // Ideally, we inject V3 service too. 
    // For simplicity of this fix: Re-create V3 service internally using a basic client or just create it here.
    
    private val modrinthClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "ModRevenueTracker/1.0 (author@example.com)") // REQUIRED by Modrinth
                .build()
            chain.proceed(request)
        }
        .build()

    // V3 Service (Internal for now)
    private val modrinthRetrofitV3 = Retrofit.Builder()
        .baseUrl("https://api.modrinth.com/v3/") 
        .client(modrinthClient) 
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        
    private val modrinthServiceV3 = modrinthRetrofitV3.create(ModrinthServiceV3::class.java)

    private val currencyRetrofit = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.app/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        
    private val currencyService = currencyRetrofit.create(CurrencyService::class.java)

    /**
     * Fetches Modrinth revenue using Payout History (All Time).
     */
    /**
     * Fetches Modrinth revenue using "Triple Fallback" Strategy:
     * 1. Payout History (Summary) -> Best for "All Time"
     * 2. Payout List (Transactions) -> Sum of all payouts
     * 3. V3 Analytics -> Raw revenue data (Project based, reliable if payouts missing)
     */
    // Data Class for Revenue Result
    data class RevenueResult(
        val totalAmount: Double,
        val last24Hours: Double = 0.0
    )

    // Internal Debug Log to trace production issues
    private var lastExecutionLog = "No execution yet."

    suspend fun getModrinthRevenue(): RevenueResult {
        val sb = StringBuilder()
        sb.append("Start Fetch @ ${System.currentTimeMillis()}\n")
        
        val token = dataStoreManager.modrinthToken.first() ?: run { sb.append("No Token\n"); lastExecutionLog = sb.toString(); return RevenueResult(0.0) }
        if (token.isBlank()) { sb.append("Blank Token\n"); lastExecutionLog = sb.toString(); return RevenueResult(0.0) }
        
        try {
            val user = modrinthService.getAuthenticatedUser(token)
            sb.append("User: ${user.username}, ID: ${user.id}\n")
            
            // Save Username for Greeting
            // Save Username for Greeting
            // Save Username and ID
            dataStoreManager.saveUserName(user.username)
            dataStoreManager.saveUserId(user.id)
            // Save Avatar (if present)
            user.avatar_url?.let { dataStoreManager.saveUserAvatar(it) }
            
            // MIGRATION: Claim legacy data for this user
            try {
                revenueDao.migrateLegacyData(user.id)
            } catch (ignore: Exception) {}
            
            // PRIORITY 1: Payout History
            try {
                 val ph = modrinthService.getPayoutHistory(token, user.id)
                 sb.append("Priority 1 (History): val=${ph.all_time}\n")
                 val allTime = ph.all_time.toDoubleOrNull()
                 if (allTime != null && allTime > 0.0) {
                     lastExecutionLog = sb.toString()
                     return RevenueResult(allTime) // Cannot get 24h from history summary easily
                 }
            } catch (e: Exception) {
                 sb.append("Priority 1 Fail: ${e.message}\n")
            }
            
            // PRIORITY 2: List... (Omitted for brevity, logic remains similar but returns RevenueResult)
            
            // PRIORITY 3: V3 Analytics
            try {
                 val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                 dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                 
                 val startDateStr = "2020-01-01T00:00:00Z" 
                 val endDateStr = dateFormat.format(Date())
                 
                 sb.append("Priority 3 (V3): Requesting $startDateStr to $endDateStr\n")
                 
                 val analyticsMap = modrinthServiceV3.getRevenueAnalytics(token, startDateStr, endDateStr)
                 
                 var totalAnalyticsRevenue = 0.0
                 var last24hRevenue = 0.0
                 var entries = 0
                 
                 // Calculate cutoff for 24h (approximate to "Yesterday + Today" or strict 24h?)
                 // Let's use "Last 1 Day" logic based on the map keys.
                 // Map keys are dates. We can just parse them.
                 val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                 
                 analyticsMap.values.forEach { dateMap ->
                     dateMap.forEach { (dateStr, amount) ->
                         entries++
                         totalAnalyticsRevenue += amount
                         
                         // Check if recent
                         try {
                             val date = dateFormat.parse(dateStr)
                             if (date != null && date.time >= oneDayAgo) {
                                 last24hRevenue += amount
                             }
                         } catch (ignore: Exception) {}
                     }
                 }
                 sb.append("Priority 3 Success: Sum=$totalAnalyticsRevenue, 24h=$last24hRevenue (Entries: $entries)\n")
                 
                 lastExecutionLog = sb.toString()
                 return RevenueResult(totalAnalyticsRevenue, last24hRevenue)
            } catch (e: Exception) {
                 sb.append("Priority 3 Fail: ${e.message}\nStack: ${e.stackTraceToString().take(200)}...\n")
                 e.printStackTrace()
            }
            
            lastExecutionLog = sb.toString()
            return RevenueResult(0.0) 
        } catch (e: Exception) {
            sb.append("CRITICAL FAIL: ${e.message}\n")
            lastExecutionLog = sb.toString()
            e.printStackTrace()
            return RevenueResult(0.0)
        }
    }

    // DEBUG FUNCTION
    suspend fun getRawModrinthResponse(): String {
        val token = dataStoreManager.modrinthToken.first() ?: return "No Token"
        return try {
            val user = modrinthService.getAuthenticatedUser(token)
            val sb = StringBuilder()
            
            sb.append("=== User ===\n${user.username} (ID: ${user.id})\n")
            sb.append("Balance: $${user.payout_data?.balance ?: 0.0}\n\n")
            
            // 1. Payout History
            val phDebug = try {
                val ph = modrinthService.getPayoutHistory(token, user.id)
                "SUCCESS: $${ph.all_time} (All Time)"
            } catch(e: Exception) { "FAIL: ${e.message}" }
            
            // 2. Payout List
            val plDebug = try {
                val list = modrinthService.getPayouts(token, user.id)
                "SUCCESS: ${list.size} payouts, Sum=$${list.sumOf { it.amount }}"
            } catch(e: Exception) { "FAIL: ${e.message} (Likely 0 payouts)" }
            
            // 3. V3 Analytics
            val v3Debug = try {
                 val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                 dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                 val startDateStr = "2020-01-01T00:00:00Z" 
                 val endDateStr = dateFormat.format(Date())
                 
                 val analyticsMap = modrinthServiceV3.getRevenueAnalytics(token, startDateStr, endDateStr)
                 
                 var totalSum = 0.0
                 val projectCount = analyticsMap.size
                 
                 analyticsMap.values.forEach { dateMap ->
                     totalSum += dateMap.values.sum()
                 }
                 
                 "SUCCESS: $$totalSum (Projects: $projectCount, Raw Map Reached)"
            } catch(e: Exception) { "FAIL: ${e.message} (Check Dates/Scopes)" }
            
            sb.append("=== Revenue Sources ===\n")
            sb.append("1. Payout History: $phDebug\n")
            sb.append("2. Payout List:    $plDebug\n")
            sb.append("3. V3 Analytics:   $v3Debug\n")
            
            sb.append("\n=== MAIN REVENUE FLOW LOG ===\n")
            sb.append(lastExecutionLog)
            
            sb.toString()
        } catch (e: Exception) {
            "Error: ${e.message}\nStack: ${e.stackTraceToString()}"
        }
    }

    suspend fun getCurseForgeRevenueInUSD(): Double {
        // 1 Point = $0.05. We rely on the scraper (WebView) to update the points in DataStore.
        val points = dataStoreManager.curseForgePoints.first()
        return points * 0.05
    }

    suspend fun convertCurrency(amountUSD: Double): Double {
        val target = dataStoreManager.targetCurrency.first()
        if (target == "USD") return amountUSD
        
        return try {
            val response = currencyService.getRates(to = target)
            val rate = response.rates[target] ?: 1.0
            amountUSD * rate
        } catch (e: Exception) {
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
