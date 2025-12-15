package com.Sourish25.MineBucks.data.repository

import com.Sourish25.MineBucks.data.repository.RevenueRepository.RevenueResult

interface RevenueDataSource {
    suspend fun getModrinthRevenue(): RevenueResult
    suspend fun getCurseForgeRevenueInUSD(): Double
}
