package com.example.newapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RevenueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: RevenueSnapshot)

    @Query("SELECT * FROM revenue_snapshots WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSnapshots(userId: String, limit: Int = 30): Flow<List<RevenueSnapshot>>

    @Query("SELECT * FROM revenue_snapshots WHERE userId = :userId AND timestamp >= :startTime ORDER BY timestamp ASC")
    fun getSnapshotsSince(userId: String, startTime: Long): Flow<List<RevenueSnapshot>>
    
    @Query("SELECT * FROM revenue_snapshots WHERE userId = :userId AND timestamp >= :startTime ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstSnapshotOfDay(userId: String, startTime: Long): RevenueSnapshot?

    @Query("DELETE FROM revenue_snapshots WHERE timestamp < :threshold")
    suspend fun deleteOldSnapshots(threshold: Long)
    @Query("DELETE FROM revenue_snapshots WHERE userId = :userId")
    suspend fun deleteHistoryForUser(userId: String)
    
    @Query("UPDATE revenue_snapshots SET userId = :newUserId WHERE userId = 'primary_user' OR userId = 'unknown_user'")
    suspend fun migrateLegacyData(newUserId: String)
    
    @Query("DELETE FROM revenue_snapshots")
    suspend fun deleteAll()
}
