package com.Sourish25.MineBucks.ads

import android.app.Activity
import android.content.Context
import android.util.Log

object AdManager {

    private const val TAG = "AdManager"
    const val areAdsEnabled = false


    // FOSS Stub: No-op
    fun initialize(context: Context) {
        Log.d(TAG, "AdManager (FOSS): initialize called - Ignored")
    }

    // FOSS Stub: No-op
    fun loadAd(context: Context) {
        Log.d(TAG, "AdManager (FOSS): loadAd called - Ignored")
    }

    // FOSS Stub: Immediately grant reward
    fun showAd(activity: Activity, onReward: () -> Unit) {
        Log.d(TAG, "AdManager (FOSS): showAd called - Granting reward immediately")
        onReward()
    }
}
