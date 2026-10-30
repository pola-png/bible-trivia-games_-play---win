package com.bibletriviaquiz.playandwin.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bibletriviaquiz.playandwin.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Jetpack Compose wrapper for Google AdMob Native Advanced Ads.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.NATIVE_AD_UNIT_ID
) {
    val context = LocalContext.current
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: NativeAd ->
                nativeAdState?.destroy()
                nativeAdState = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Fail gracefully
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAdState?.destroy()
        }
    }

    val currentAd = nativeAdState
    if (currentAd != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val view = LayoutInflater.from(ctx).inflate(R.layout.layout_admob_native, null) as NativeAdView

                    val headlineView = view.findViewById<TextView>(R.id.ad_headline)
                    val bodyView = view.findViewById<TextView>(R.id.ad_body)
                    val ctaView = view.findViewById<Button>(R.id.ad_call_to_action)
                    val iconView = view.findViewById<ImageView>(R.id.ad_app_icon)
                    val advertiserView = view.findViewById<TextView>(R.id.ad_advertiser)

                    view.headlineView = headlineView
                    view.bodyView = bodyView
                    view.callToActionView = ctaView
                    view.iconView = iconView
                    view.advertiserView = advertiserView

                    populateNativeAdView(currentAd, view)
                    view
                },
                update = { view ->
                    populateNativeAdView(currentAd, view)
                }
            )
        }
    }
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // Headline
    (adView.headlineView as? TextView)?.apply {
        text = nativeAd.headline
        visibility = if (nativeAd.headline != null) View.VISIBLE else View.INVISIBLE
    }

    // Body
    (adView.bodyView as? TextView)?.apply {
        text = nativeAd.body
        visibility = if (nativeAd.body != null) View.VISIBLE else View.GONE
    }

    // Call to action
    (adView.callToActionView as? Button)?.apply {
        text = nativeAd.callToAction
        visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.GONE
    }

    // Icon
    (adView.iconView as? ImageView)?.apply {
        if (nativeAd.icon?.drawable != null) {
            setImageDrawable(nativeAd.icon?.drawable)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    // Advertiser
    (adView.advertiserView as? TextView)?.apply {
        text = nativeAd.advertiser
        visibility = if (nativeAd.advertiser != null) View.VISIBLE else View.GONE
    }

    adView.setNativeAd(nativeAd)
}
