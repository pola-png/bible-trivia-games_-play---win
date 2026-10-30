package com.bibletriviaquiz.playandwin.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages loading and displaying Google AdMob Rewarded video ads.
 */
class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    fun loadAd(onLoaded: (() -> Unit)? = null) {
        if (rewardedAd != null) {
            onLoaded?.invoke()
            return
        }
        if (onLoaded != null) {
            pendingCallbacks.add(onLoaded)
        }
        if (isLoading) return

        isLoading = true
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdMobConfig.REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    _isAdLoaded.value = true
                    Log.d("RewardedAdManager", "Rewarded ad loaded successfully.")
                    val callbacks = pendingCallbacks.toList()
                    pendingCallbacks.clear()
                    callbacks.forEach { it.invoke() }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    _isAdLoaded.value = false
                    Log.w("RewardedAdManager", "Rewarded ad failed to load: ${loadAdError.message}")
                    pendingCallbacks.clear()
                }
            }
        )
    }

    fun isReady(): Boolean {
        return rewardedAd != null
    }

    fun showAd(
        activity: Activity,
        onUserEarnedReward: (Int) -> Unit,
        onAdDismissed: () -> Unit = {},
        onAdNotReady: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.w("RewardedAdManager", "Rewarded ad not loaded yet.")
            loadAd()
            onAdNotReady()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isAdLoaded.value = false
                loadAd()
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _isAdLoaded.value = false
                loadAd()
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("RewardedAdManager", "Rewarded ad shown.")
            }
        }

        ad.show(activity) { rewardItem ->
            val rewardPoints = if (rewardItem.amount > 0) rewardItem.amount else 25
            Log.d("RewardedAdManager", "User earned reward: $rewardPoints")
            onUserEarnedReward(rewardPoints)
        }
    }
}
