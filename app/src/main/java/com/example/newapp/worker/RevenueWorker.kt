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
             
             // 3. Get Robust User ID
             // Try to get explicit ID (updated by repository), but fallback to Helper.generateUserId(token)
             // This ensures that even if API fails, we use a consistent ID for this token, preventing data mixing.
             val userId = dataStoreManager.userId.first() ?: ""
            
            // Generate deterministic ID if missing
            val cleanUserId = if (userId.isBlank()) {
                val tokenForHash = dataStoreManager.modrinthToken.first() ?: ""
                val hash = try {
                     val digest = java.security.MessageDigest.getInstance("SHA-256")
                     val bytes = digest.digest(tokenForHash.toByteArray())
                     // Convert to Hex
                     bytes.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    "fallback-device-id"
                }
                "auto_generated_$hash"
            } else {
                userId
            }
            
            // 4. Create Snapshot Object
            val snapshot = RevenueSnapshot(
                timestamp = System.currentTimeMillis(),
                modrinthRevenue = modResult.totalAmount,
                curseForgeRevenue = cfRevenue,
                currency = dataStoreManager.targetCurrency.first(),
                userId = cleanUserId
            )
            
            // 5. Smart Save (Debounce)
            // Fetch the *latest* snapshot for this user to check for changes
            val lastSnapshot = revenueDao.getLatestSnapshot(userId)
            val currentTotal = modResult.totalAmount + cfRevenue
            val lastTotal = (lastSnapshot?.modrinthRevenue ?: 0.0) + (lastSnapshot?.curseForgeRevenue ?: 0.0)
            val timeDiff = System.currentTimeMillis() - (lastSnapshot?.timestamp ?: 0L)

            // Save ONLY if: No previous data OR Value Changed (> 0.001) OR > 1 Hour passed
            if (lastSnapshot == null || kotlin.math.abs(currentTotal - lastTotal) > 0.001 || timeDiff > 3600000) {
                revenueDao.insertSnapshot(snapshot)
                
                 // NOTIFICATION (Only if increased)
                if (lastSnapshot != null && currentTotal > lastTotal) {
                    val diff = currentTotal - lastTotal
                    sendRevenueNotification(applicationContext, diff, snapshot.currency)
                }
            }
            
            // WIDGET UPDATE (Always update widget to show freshness)
            updateWidget(applicationContext, currentTotal, snapshot.currency)
            
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
