package com.Sourish25.MineBucks

import android.app.Application
import com.Sourish25.MineBucks.data.database.AppDatabase
import com.Sourish25.MineBucks.data.repository.DataStoreManager
import com.Sourish25.MineBucks.BuildConfig

class ModRevenueApplication : Application() {
    // Database instance, keeping it open effectively
    val database by lazy { com.Sourish25.MineBucks.data.database.AppDatabase.getDatabase(this) }
    
    // DataStore instance (if we want to centralize it)
    val dataStoreManager by lazy { DataStoreManager(this) }
    
    // Repository Singleton
    val repository by lazy { 
        val dataSource = com.Sourish25.MineBucks.data.repository.RealRevenueDataSource(
             com.Sourish25.MineBucks.data.network.ModrinthClient.service,
             dataStoreManager
        )
        
        com.Sourish25.MineBucks.data.repository.RevenueRepository(
            dataSource, 
            dataStoreManager,
            database.revenueDao()
        ) 
    }

    override fun onCreate() {
        super.onCreate()
        
        // WorkManager
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.Sourish25.MineBucks.worker.RevenueWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RevenueSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


}
