package com.Sourish25.MineBucks.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "revenue_snapshots")
data class RevenueSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val modrinthRevenue: Double,
    val curseForgeRevenue: Double,
    val currency: String,
    val userId: String // To separate accounts
)
