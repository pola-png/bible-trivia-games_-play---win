package com.bibletriviaquiz.playandwin.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * Manages loading and displaying Google AdMob App Open ads.
 */
class AppOpenAdManager(private val context: Context) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0

    fun loadAd(onLoaded: (() -> Unit)? = null) {
        if (isLoadingAd || isAdAvailable()) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AdMobConfig.APP_OPEN_AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d("AppOpenAdManager", "App open ad loaded successfully.")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.w("AppOpenAdManager", "App open ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun wasLoadTimeLessThan4HoursAgo(): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * 4
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThan4HoursAgo()
    }

    fun showAdIfAvailable(activity: Activity, onShowComplete: () -> Unit = {}) {
        if (isShowingAd) {
            Log.d("AppOpenAdManager", "App open ad is already showing.")
            return
        }

        if (!isAdAvailable()) {
            Log.d("AppOpenAdManager", "App open ad is not ready yet.")
            onShowComplete()
            loadAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                Log.d("AppOpenAdManager", "App open ad dismissed.")
                onShowComplete()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Log.w("AppOpenAdManager", "App open ad failed to show: ${adError.message}")
                onShowComplete()
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d("AppOpenAdManager", "App open ad shown.")
            }
        }
        appOpenAd?.show(activity)
    }
}
