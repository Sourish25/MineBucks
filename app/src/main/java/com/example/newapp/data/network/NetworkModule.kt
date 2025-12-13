package com.example.newapp.data.network

import com.example.newapp.data.api.ModrinthService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object ModrinthClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "ModRevenueTracker/1.0 (author@example.com)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.modrinth.com/v2/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val service: ModrinthService = retrofit.create(ModrinthService::class.java)
}

object CurseForgeClient {
    // Basic implementation for now, or just placeholder if we only scrape
    // But since Repository asks for it, we provide a placeholder or real if we have API
    // The previous code implied we might scrape, but let's provide the service interface if it exists.
    // If CurseForgeService interface is just a scraper helper, we need to see what it is.
    // "RevenueRepository(..., private val curseForgeService: CurseForgeService, ...)"
    // I need to know what CurseForgeService IS.
    
    // Assuming it's a Retrofit service as well, or I'll create a dummy one if it is not used for real API calls yet.
    // Wait, the plan says "Implement CurseForge WebView Login & Scraper".
    // Does RevenueRepository actually USE curseForgeService?
    // In my previous `view_file` of `RevenueRepository`, I saw `getCurseForgeRevenueInUSD` used `dataStoreManager`.
    // It did NOT use a service.
    // SO, why did I add it to the constructor?
    // "private val curseForgeService: com.example.newapp.data.api.CurseForgeService"
    // I probably shouldn't have added it if it's not used. 
    // BUT, for consistency, if I have it, I should provide it.
    // Let's create a minimal Service or Mock.
    // Actually, checking `Api.kt` again... it only had `ModrinthService` and `CurrencyService`.
    // It did NOT have `CurseForgeService`.
    // So `CurseForgeService` definitely does not exist in `Api.kt`.
    // I made a mistake adding it to the Constructor of `RevenueRepository`.
    
    // REVISION: I will NOT include CurseForgeClient service if the interface doesn't exist.
    // I will REMOVE it from RevenueRepository constructor in the next step.
    // But first, let's create this file for ModrinthClient.
    
    // Keeping this object empty or creating a basic service if needed later.
    // For now, I'll allow ModrinthClient.
}
