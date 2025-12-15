package com.Sourish25.MineBucks.data.network

import com.Sourish25.MineBucks.data.api.ModrinthService
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

// CurseForgeClient removed as it is not used directly (WebScraping via WebView).
