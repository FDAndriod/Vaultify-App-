package com.dyiz.vaultify.ads

import com.dyiz.vaultify.NavRoutes

/**
 * Interstitials only when returning to home from vault/media flows — never from PIN, splash, or setup.
 */
object InterstitialNavPolicy {

    private val vaultSectionBases = setOf(
        NavRoutes.IMAGES_SCREEN,
        NavRoutes.VIDEOS_SCREEN,
        NavRoutes.FILES_SCREEN,
        NavRoutes.AUDIOS_SCREEN,
        NavRoutes.DOCS_SCREEN,
        NavRoutes.DISGUISE_ICON_SCREEN,
        NavRoutes.EXTRACT_IMAGE_SCREEN,
        NavRoutes.CROP_IMAGE_SCREEN,
        NavRoutes.EXTRACTED_TEXT_SCREEN,
    )

    fun shouldOfferInterstitial(previousRoute: String?): Boolean {
        if (previousRoute.isNullOrEmpty()) return false
        val base = previousRoute.substringBefore("?").substringBefore("/")
        if (base in vaultSectionBases) return true
        return previousRoute.startsWith("${NavRoutes.IMAGE_VIEWER_SCREEN}/") ||
            previousRoute.startsWith("${NavRoutes.VIDEO_PLAYING_SCREEN}/") ||
            previousRoute.startsWith("${NavRoutes.AUDIO_PLAYING_SCREEN}/")
    }
}
