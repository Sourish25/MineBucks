package com.example.newapp

import android.app.Application
import com.example.newapp.data.database.AppDatabase
import com.example.newapp.data.repository.DataStoreManager
import com.example.newapp.BuildConfig

class ModRevenueApplication : Application() {
    // Database instance, keeping it open effectively
    val database by lazy { com.example.newapp.data.database.AppDatabase.getDatabase(this) }
    
    // DataStore instance (if we want to centralize it)
    val dataStoreManager by lazy { DataStoreManager(this) }
    
    // Repository Singleton
    val repository by lazy { 
        val dataSource = com.example.newapp.data.repository.RealRevenueDataSource(
             com.example.newapp.data.network.ModrinthClient.service,
             dataStoreManager
        )
        
        com.example.newapp.data.repository.RevenueRepository(
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

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.newapp.worker.RevenueWorker>(
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
