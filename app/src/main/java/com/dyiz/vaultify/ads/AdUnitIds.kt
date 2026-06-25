package com.dyiz.vaultify.ads

import com.dyiz.vaultify.BuildConfig

object AdUnitIds {
    private const val PROD_BANNER = "ca-app-pub-7259063205484413/3571039958"
    private const val PROD_INTERSTITIAL = "ca-app-pub-7259063205484413/9353849674"

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    fun bannerAdUnitId(): String =
        if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER

    fun interstitialAdUnitId(): String =
        if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL
}
