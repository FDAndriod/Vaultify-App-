package com.dyiz.vaultify.ads

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.EntryPointAccessors

/**
 * Runs UMP consent update, shows the consent form when required, then initializes the Mobile Ads SDK.
 * Call after the activity UI is visible (e.g. from [android.view.Window.getDecorView] post) so the user
 * never sees a blank screen while waiting for consent or the network.
 */
object AdsConsent {

    fun prepareAndInitializeMobileAds(activity: FragmentActivity) {
        val appContext = activity.applicationContext
        val consentInformation = UserMessagingPlatform.getConsentInformation(appContext)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    finishAdsSetup(appContext, activity)
                }
            },
            { _ ->
                finishAdsSetup(appContext, activity)
            }
        )
    }

    private fun finishAdsSetup(
        appContext: android.content.Context,
        activity: FragmentActivity
    ) {
        MobileAds.initialize(appContext) {
            MobileAds.setAppMuted(true)
            EntryPointAccessors.fromApplication(appContext, AdsEntryPoint::class.java)
                .interstitialAdManager()
                .preload()
        }
    }
}
