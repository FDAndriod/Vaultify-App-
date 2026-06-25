package com.dyiz.vaultify.ads

import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

enum class VaultifyBannerFormat {
    /** Standard anchored adaptive banner (full width, short height). */
    AdaptiveAnchored,
    /** 300×250 dp medium rectangle — use when the layout has room (e.g. home). */
    MediumRectangle,
}

/**
 * Banner ad for Compose. Collapses when load fails.
 */
@Composable
fun VaultifyAdBanner(
    analytics: VaultifyAnalytics,
    modifier: Modifier = Modifier,
    format: VaultifyBannerFormat = VaultifyBannerFormat.AdaptiveAnchored,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val unitId = remember { AdUnitIds.bannerAdUnitId() }
    var visible by remember(unitId, screenWidthDp, format) { mutableStateOf(true) }

    if (!visible) return

    val adFormatLabel = when (format) {
        VaultifyBannerFormat.AdaptiveAnchored -> "banner"
        VaultifyBannerFormat.MediumRectangle -> "medium_rectangle"
    }

    val placement = "home"

    key(screenWidthDp, unitId, format) {
        AndroidView(
            modifier = when (format) {
                VaultifyBannerFormat.MediumRectangle ->
                    modifier.defaultMinSize(minHeight = 250.dp)
                VaultifyBannerFormat.AdaptiveAnchored ->
                    modifier
            },
            factory = { ctx ->
                val adView = AdView(ctx).apply {
                    when (format) {
                        VaultifyBannerFormat.AdaptiveAnchored -> setAdSize(
                            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp)
                        )
                        VaultifyBannerFormat.MediumRectangle -> setAdSize(AdSize.MEDIUM_RECTANGLE)
                    }
                    adUnitId = unitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            analytics.logAdLoaded(adFormatLabel, unitId, placement = placement)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            visible = false
                            analytics.logAdFailedToLoad(
                                adFormatLabel,
                                unitId,
                                error.code,
                                error.message,
                                placement = placement
                            )
                        }

                        override fun onAdOpened() {
                            analytics.logAdOpened(adFormatLabel, unitId, placement = placement)
                        }

                        override fun onAdClicked() {
                            analytics.logAdClicked(adFormatLabel, unitId, placement = placement)
                        }

                        override fun onAdImpression() {
                            analytics.logAdBannerImpression(unitId, placement = placement)
                        }
                    }
                    setOnPaidEventListener { adValue ->
                        analytics.logAdRevenuePaid(
                            adValue,
                            adFormat = adFormatLabel,
                            adUnitId = unitId,
                            mediationNetwork = null
                        )
                    }
                }
                analytics.logAdRequest(adFormatLabel, unitId, placement = placement)
                adView.loadAd(AdRequest.Builder().build())
                FrameLayout(ctx).apply {
                    val lp = when (format) {
                        VaultifyBannerFormat.AdaptiveAnchored -> FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        VaultifyBannerFormat.MediumRectangle -> FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_HORIZONTAL
                        )
                    }
                    addView(adView, lp)
                }
            },
            update = { frame ->
                val adView = frame.getChildAt(0) as? AdView ?: return@AndroidView
                when (format) {
                    VaultifyBannerFormat.AdaptiveAnchored -> {
                        val newSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                            adView.context,
                            screenWidthDp
                        )
                        if (adView.adSize != newSize) {
                            adView.setAdSize(newSize)
                        }
                    }
                    VaultifyBannerFormat.MediumRectangle -> {
                        if (adView.adSize != AdSize.MEDIUM_RECTANGLE) {
                            adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
                        }
                    }
                }
            }
        )
    }
}
