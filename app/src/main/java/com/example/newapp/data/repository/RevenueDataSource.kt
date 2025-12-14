package com.example.newapp.data.repository

import com.example.newapp.data.repository.RevenueRepository.RevenueResult

interface RevenueDataSource {
    suspend fun getModrinthRevenue(): RevenueResult
    suspend fun getCurseForgeRevenueInUSD(): Double
}
