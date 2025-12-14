package com.example.newapp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.newapp.MainActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private var rewardedAd: RewardedAd? = null
    // REAL ID for Rewarded Video
    private const val AD_UNIT_ID = "ca-app-pub-2159601373823175/6788941996"
    private const val TAG = "AdManager"

    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        // Use Application Context to avoid leaking Activity if ad loads slowly
        RewardedAd.load(context.applicationContext, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.message}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad loaded successfully")
                rewardedAd = ad
            }
        })
    }

    fun showAd(activity: Activity, onReward: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    rewardedAd = null
                    loadAd(activity) // Preload next
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    rewardedAd = null
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            
            rewardedAd?.show(activity) { rewardItem ->
                // Handle the reward.
                Log.d(TAG, "User earned the reward: ${rewardItem.amount} ${rewardItem.type}")
                onReward()
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            // Try formatting a toast or fallback?
            loadAd(activity) // Try loading again
        }
    }
}
