package com.Sourish25.MineBucks.data.repository

import com.Sourish25.MineBucks.data.api.ModrinthService
import kotlinx.coroutines.flow.first
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import com.Sourish25.MineBucks.data.repository.RevenueRepository.RevenueResult

class RealRevenueDataSource(
    private val api: ModrinthService,
    private val dataStoreManager: DataStoreManager
) : RevenueDataSource {

    // Internal Client for V3
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val modrinthClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "ModRevenueTracker/1.0 (author@example.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val modrinthRetrofitV3 = retrofit2.Retrofit.Builder()
        .baseUrl("https://api.modrinth.com/v3/") 
        .client(modrinthClient) 
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        
    private val modrinthServiceV3 = modrinthRetrofitV3.create(com.Sourish25.MineBucks.data.api.ModrinthServiceV3::class.java)
    
    // Internal Debug Log
    private var lastExecutionLog = "No execution yet."

    override suspend fun getModrinthRevenue(): RevenueResult {
        val sb = StringBuilder()
        sb.append("Start Fetch @ ${System.currentTimeMillis()}\n")
        
        val token = dataStoreManager.modrinthToken.first() ?: run { sb.append("No Token\n"); lastExecutionLog = sb.toString(); return RevenueResult(0.0) }
        if (token.isBlank()) { sb.append("Blank Token\n"); lastExecutionLog = sb.toString(); return RevenueResult(0.0) }
        
        try {
            val user = api.getAuthenticatedUser(token)
            sb.append("User: ${user.username}, ID: ${user.id}\n")
            
            // Save User Info
            dataStoreManager.saveUserName(user.username)
            dataStoreManager.saveUserId(user.id)
            user.avatar_url?.let { dataStoreManager.saveUserAvatar(it) }
            
            // PRIORITY 1: Payout History
            try {
                 val ph = api.getPayoutHistory(token, user.id)
                 sb.append("Priority 1 (History): val=${ph.all_time}\n")
                 val allTime = ph.all_time.toDoubleOrNull()
                 if (allTime != null && allTime > 0.0) {
                     lastExecutionLog = sb.toString()
                     return RevenueResult(allTime)
                 }
            } catch (e: Exception) {
                 sb.append("Priority 1 Fail: ${e.message}\n")
            }
            
            // PRIORITY 3: V3 Analytics
            try {
                 val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                 dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                 
                 val startDateStr = "2020-01-01T00:00:00Z" 
                 val endDateStr = dateFormat.format(java.util.Date())
                 
                 sb.append("Priority 3 (V3): Requesting $startDateStr to $endDateStr\n")
                 
                 val analyticsMap = modrinthServiceV3.getRevenueAnalytics(token, startDateStr, endDateStr)
                 
                 var totalAnalyticsRevenue = 0.0
                 var last24hRevenue = 0.0
                 var entries = 0
                 
                 val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                 
                 analyticsMap.values.forEach { dateMap ->
                     dateMap.forEach { (dateStr, amount) ->
                         entries++
                         totalAnalyticsRevenue += amount
                         
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
            // If we reached here, both Priority 1 and 3 failed to return valid data BUT didn't throw yet?
            // Actually Priority 3 catch block just logged.
            // We should treat this as a failure if we didn't get data.
            throw Exception("Failed to fetch Modrinth Revenue (All Priorities Failed)")
        } catch (e: Exception) {
            sb.append("CRITICAL FAIL: ${e.message}\n")
            lastExecutionLog = sb.toString()
            e.printStackTrace()
            throw e // Re-throw to signal failure to ViewModel
        }
    }

    override suspend fun getCurseForgeRevenueInUSD(): Double {
        val points = dataStoreManager.curseForgePoints.first()
        return points * 0.05
    }

    fun getDebugLog(): String = lastExecutionLog
}
