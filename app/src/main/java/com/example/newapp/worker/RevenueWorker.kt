package com.example.newapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.newapp.ModRevenueApplication
import com.example.newapp.data.database.RevenueSnapshot
import kotlinx.coroutines.flow.first
import java.util.UUID

class RevenueWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as ModRevenueApplication
        val repository = application.repository
        val dataStoreManager = application.dataStoreManager
        val revenueDao = application.database.revenueDao()

        return try {
            // 1. Fetch Data
            val modResult = try {
                repository.getModrinthRevenue()
            } catch (e: Exception) {
                // Return Retry if network failure? For now just log and fail specific platform
                e.printStackTrace()
                com.example.newapp.data.repository.RevenueRepository.RevenueResult(0.0)
            }
            
            val cfRevenue = try {
                repository.getCurseForgeRevenueInUSD()
            } catch (e: Exception) {
                e.printStackTrace()
                0.0
            }
            
            // 2. Validate User Context (Account Isolation)
             val token = dataStoreManager.modrinthToken.first()
             if (token.isNullOrBlank()) {
                 // No user logged in, cannot track.
                 return Result.failure()
             }
             
             // Get User ID from saved token (Ideally we saved ID in prefs too, but we can fetch it or use token hash as key if rigorous isolation needed. 
             // Plan said "Detect User ID change".
             // We can fetch user again or rely on what's active. 
             // Repository fetches user profile inside getModrinthRevenue() -> saves to DataStore.
             // We need a stable ID. Let's use the one from DataStoreUserName for now or fetch it cleanly.
             // Better: Repository already fetches it. We should assume the CURRENT active token defines the "User".
             // For now, let's use a hashed token or just "default_user" if we haven't implemented multi-user fully yet. 
             // Wait, the plan said "userId -> Critical".
             // Let's grab it from the API call if we can, OR store it in DataStore.
             // Simplest compliant fix:
             val user = try { 
                // We don't expose getUser in repo easily without making another call.
                // Let's rely on "UserName" from DataStore as the ID key for now? No, unqiue ID is better.
                // Let's add `saveUserId` to DataStoreManager later. For now, we will assume single active user or use Token hash.
                // Actually, let's just use "current_user" string or fetch it.
                 "current_user" 
             } catch(e: Exception) { "unknown" }
             
             // Use stable User ID if possible.
             // Let's proceed with capturing the snapshot.

            // 3. Get User ID (Updated by repository.getModrinthRevenue)
            val userId = dataStoreManager.userId.first() ?: "unknown"
            
            // 4. Create Snapshot
            val snapshot = RevenueSnapshot(
                timestamp = System.currentTimeMillis(),
                modrinthRevenue = modResult.totalAmount,
                curseForgeRevenue = cfRevenue,
                currency = dataStoreManager.targetCurrency.first() ?: "USD",
                userId = userId
            )
            
            // Check for Increase (Smart Notification)
            // Fetch the *previous* snapshot for this user
            // Our DAO has getRecentSnapshots.
            val recent = revenueDao.getRecentSnapshots(userId, limit = 1).first().firstOrNull()
            
            // 3. Save to DB
            revenueDao.insertSnapshot(snapshot)
            
            // WIDGET UPDATE (Passive)
            updateWidget(applicationContext, snapshot.modrinthRevenue + snapshot.curseForgeRevenue, snapshot.currency)

            // NOTIFICATION (Smart)
            if (recent != null) {
                val oldTotal = recent.modrinthRevenue + recent.curseForgeRevenue
                val newTotal = snapshot.modrinthRevenue + snapshot.curseForgeRevenue
                if (newTotal > oldTotal) {
                    val diff = newTotal - oldTotal
                    sendRevenueNotification(applicationContext, diff, snapshot.currency)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun updateWidget(context: Context, total: Double, currency: String) {
        com.example.newapp.widget.RevenueWidget.updateRevenueWidget(context, total, currency)
    }

    private fun sendRevenueNotification(context: Context, amountDiff: Double, currency: String) {
        val channelId = "revenue_updates"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Create Channel (Safe to re-create)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "Revenue Updates", android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val formatted = if (currency == "USD") "$%.2f".format(amountDiff) else "$amountDiff $currency"
        
        val intent = android.content.Intent(context, com.example.newapp.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            // Use safe system icon or ensure App Icon is valid (transparent).
            // For safety after the debug session confirmed it, we use the system info icon.
            // Ideally we should import a proper monochrome icon later.
            .setSmallIcon(com.example.newapp.R.drawable.ic_notification_pixel) // Pixel Art Dollar Icon 
            .setContentTitle("Revenue Increase! \uD83D\uDCC8")
            .setContentText("You made +$formatted since the last check.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Open App on Click
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(1001, notification)
    }
}
