package com.example.newapp

import android.app.Application
import com.example.newapp.data.database.AppDatabase
import com.example.newapp.data.repository.DataStoreManager

class ModRevenueApplication : Application() {
    // Database instance, keeping it open effectively
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // DataStore instance (if we want to centralize it)
    val dataStoreManager by lazy { DataStoreManager(this) }
    
    // Repository Singleton
    val repository by lazy { 
        com.example.newapp.data.repository.RevenueRepository(
            com.example.newapp.data.network.ModrinthClient.service, 
            dataStoreManager,
            database.revenueDao()
        ) 
    }

    override fun onCreate() {
        super.onCreate()
         // Schedule Background Work
        scheduleRevenueSync()
    }

    private fun scheduleRevenueSync() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.newapp.worker.RevenueWorker>(
            6, java.util.concurrent.TimeUnit.HOURS
        )
        .setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
        )
        .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RevenueSyncWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
