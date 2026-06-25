package com.dyiz.vaultify.ads

import android.app.Activity
import android.content.Context
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.auth.AuthSession
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preloads interstitials and shows them on safe navigation transitions only (never on PIN/security).
 * Frequency-capped to avoid disrupting the vault experience.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: VaultifyAnalytics
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var interstitialAd: InterstitialAd? = null

    fun preload() {
        val unitId = AdUnitIds.interstitialAdUnitId()
        analytics.logAdRequest("interstitial", unitId, placement = "preload")
        InterstitialAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    analytics.logAdLoaded("interstitial", unitId, placement = "preload")
                    ad.setOnPaidEventListener { adValue ->
                        analytics.logAdRevenuePaid(
                            adValue,
                            adFormat = "interstitial",
                            adUnitId = unitId,
                            mediationNetwork = null
                        )
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    analytics.logAdFailedToLoad(
                        "interstitial",
                        unitId,
                        error.code,
                        error.message,
                        placement = "preload"
                    )
                }
            }
        )
    }

    /**
     * Shows a loaded interstitial if frequency cap allows. Returns true if [InterstitialAd.show] was invoked.
     */
    fun showIfReady(activity: Activity, placement: String): Boolean {
        if (!frequencyAllowsShow()) return false
        val ad = interstitialAd ?: run {
            preload()
            return false
        }
        val unitId = AdUnitIds.interstitialAdUnitId()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                markShownNow()
                analytics.logInterstitialShown(placement)
            }

            override fun onAdClicked() {
                analytics.logAdClicked("interstitial", unitId, placement)
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                analytics.logInterstitialDismissed(placement)
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                analytics.logAdFailedToShow("interstitial", unitId, adError.message)
                preload()
            }
        }
        analytics.logInterstitialShowRequested(placement)
        // The ad fragment will pause our activity. We're the ones choosing to show it, so the
        // return trip is trusted — bypass the re-lock so users don't land on the PIN screen
        // immediately after the ad is dismissed.
        AuthSession.markSkipLock(context)
        ad.show(activity)
        return true
    }

    private fun frequencyAllowsShow(): Boolean {
        val last = prefs.getLong(KEY_LAST_INTERSTITIAL_MS, 0L)
        if (last == 0L) return true
        return System.currentTimeMillis() - last >= MIN_INTERVAL_MS
    }

    private fun markShownNow() {
        prefs.edit().putLong(KEY_LAST_INTERSTITIAL_MS, System.currentTimeMillis()).apply()
    }

    private companion object {
        const val PREFS_NAME = "vaultify_ad_prefs"
        const val KEY_LAST_INTERSTITIAL_MS = "last_interstitial_ms"
        const val MIN_INTERVAL_MS = 3 * 60 * 1000L
    }
}
