package com.dyiz.vaultify.analytics

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdValue
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Firebase Analytics logging for Vaultify.
 * Use for screen views, button clicks, and flow events to trace usage and scalability.
 */
@Singleton
class VaultifyAnalytics @Inject constructor(
    private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    // ---------- Screen names (for screen_view) ----------
    object Screen {
        const val SPLASH = "splash"
        const val PIN_CREATION = "pin_creation"
        const val PIN_ENTER = "pin_enter"
        const val SECURITY_QUESTION = "security_question"
        const val SECURITY_QUESTION_RECOVERY = "security_question_recovery"
        const val RESET_PIN = "reset_pin"
        const val FINGERPRINT_SETUP = "fingerprint_setup"
        const val HOME = "home"
        const val IMAGES = "images_vault"
        const val VIDEOS = "videos_vault"
        const val FILES = "files_vault"
        const val AUDIOS = "audios_vault"
        const val DOCS = "docs_vault"
        const val DISGUISE_ICON = "disguise_icon"
        const val EXTRACT_IMAGE = "extract_image"
        const val CROP_IMAGE = "crop_image"
        const val EXTRACTED_TEXT = "extracted_text"
        const val IMAGE_VIEWER = "image_viewer"
        const val VIDEO_PLAYING = "video_playing"
        const val AUDIO_PLAYING = "audio_playing"
        const val FILE_PREVIEW = "file_preview"
        const val NOTES_LIST = "notes_list"
        const val NOTE_EDIT = "note_edit"
    }

    // ---------- Custom event names ----------
    object Event {
        const val BUTTON_CLICK = "vaultify_button_click"
        const val FLOW_START = "vaultify_flow_start"
        const val FLOW_COMPLETE = "vaultify_flow_complete"
        const val VAULT_ITEM_ADDED = "vaultify_vault_item_added"
        const val VAULT_ITEM_REMOVED = "vaultify_vault_item_removed"
        const val VAULT_ITEM_UNHIDDEN = "vaultify_vault_item_unhidden"
        const val DISGUISE_ICON_CHANGED = "vaultify_disguise_icon_changed"
        const val OCR_SCAN_START = "vaultify_ocr_scan_start"
        const val OCR_SCAN_COMPLETE = "vaultify_ocr_scan_complete"
        const val SETTINGS_OPENED = "vaultify_settings_opened"
        const val SETTINGS_ACTION = "vaultify_settings_action"
        const val AD_REQUEST = "vaultify_ad_request"
        const val AD_LOADED = "vaultify_ad_loaded"
        const val AD_FAILED_LOAD = "vaultify_ad_failed"
        const val AD_FAILED_SHOW = "vaultify_ad_failed_show"
        const val AD_OPENED = "vaultify_ad_opened"
        const val AD_CLICKED = "vaultify_ad_clicked"
        const val AD_BANNER_IMPRESSION = "vaultify_ad_banner_impression"
        const val INTERSTITIAL_SHOW_REQUESTED = "vaultify_interstitial_show_requested"
        const val INTERSTITIAL_SHOWN = "vaultify_interstitial_shown"
        const val INTERSTITIAL_DISMISSED = "vaultify_interstitial_dismissed"
    }

    object Param {
        const val SCREEN_NAME = "screen_name"
        const val SCREEN_CLASS = "screen_class"
        const val BUTTON_ID = "button_id"
        const val FLOW_NAME = "flow_name"
        const val ITEM_TYPE = "item_type"
        const val ITEM_COUNT = "item_count"
        const val DISGUISE_ALIAS = "disguise_alias"
        const val SOURCE = "source" // e.g. "camera", "gallery"
        const val SUCCESS = "success"
        const val SETTING_KEY = "setting_key"
        const val AD_FORMAT = "ad_format"
        const val AD_UNIT_ID = "ad_unit_id"
        const val PLACEMENT = "placement"
        const val ERROR_CODE = "error_code"
        const val ERROR_MESSAGE = "error_message"
        const val MEDIATION_NETWORK = "mediation_network"
    }

    /** Log standard screen_view for Firebase Analytics. */
    fun logScreenView(screenName: String, screenClass: String = "Composable") {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    /** Log a button or UI element click. */
    fun logButtonClick(buttonId: String, screenName: String? = null) {
        val bundle = Bundle().apply {
            putString(Param.BUTTON_ID, buttonId)
            screenName?.let { putString(Param.SCREEN_NAME, it) }
        }
        firebaseAnalytics.logEvent(Event.BUTTON_CLICK, bundle)
    }

    /** Log start of a user flow (e.g. onboarding, sign up). */
    fun logFlowStart(flowName: String, screenName: String? = null) {
        val bundle = Bundle().apply {
            putString(Param.FLOW_NAME, flowName)
            screenName?.let { putString(Param.SCREEN_NAME, it) }
        }
        firebaseAnalytics.logEvent(Event.FLOW_START, bundle)
    }

    /** Log successful completion of a flow. */
    fun logFlowComplete(flowName: String, success: Boolean = true, screenName: String? = null) {
        val bundle = Bundle().apply {
            putString(Param.FLOW_NAME, flowName)
            putBoolean(Param.SUCCESS, success)
            screenName?.let { putString(Param.SCREEN_NAME, it) }
        }
        firebaseAnalytics.logEvent(Event.FLOW_COMPLETE, bundle)
    }

    /** Log when user adds item(s) to vault. */
    fun logVaultItemAdded(itemType: String, count: Int = 1) {
        val bundle = Bundle().apply {
            putString(Param.ITEM_TYPE, itemType)
            putInt(Param.ITEM_COUNT, count)
        }
        firebaseAnalytics.logEvent(Event.VAULT_ITEM_ADDED, bundle)
    }

    /** Log when user removes item(s) from vault (delete). */
    fun logVaultItemRemoved(itemType: String, count: Int = 1) {
        val bundle = Bundle().apply {
            putString(Param.ITEM_TYPE, itemType)
            putInt(Param.ITEM_COUNT, count)
        }
        firebaseAnalytics.logEvent(Event.VAULT_ITEM_REMOVED, bundle)
    }

    /** Log when user unhides/restores item(s) from vault. */
    fun logVaultItemUnhidden(itemType: String, count: Int = 1) {
        val bundle = Bundle().apply {
            putString(Param.ITEM_TYPE, itemType)
            putInt(Param.ITEM_COUNT, count)
        }
        firebaseAnalytics.logEvent(Event.VAULT_ITEM_UNHIDDEN, bundle)
    }

    /** Log disguise icon change. */
    fun logDisguiseIconChanged(alias: String) {
        val bundle = Bundle().apply {
            putString(Param.DISGUISE_ALIAS, alias)
        }
        firebaseAnalytics.logEvent(Event.DISGUISE_ICON_CHANGED, bundle)
    }

    /** Log OCR/text scan started. */
    fun logOcrScanStart(source: String) {
        val bundle = Bundle().apply {
            putString(Param.SOURCE, source)
        }
        firebaseAnalytics.logEvent(Event.OCR_SCAN_START, bundle)
    }

    /** Log OCR/text scan completed. */
    fun logOcrScanComplete(success: Boolean, source: String? = null) {
        val bundle = Bundle().apply {
            putBoolean(Param.SUCCESS, success)
            source?.let { putString(Param.SOURCE, it) }
        }
        firebaseAnalytics.logEvent(Event.OCR_SCAN_COMPLETE, bundle)
    }

    /** Log settings opened (sidebar/drawer). */
    fun logSettingsOpened() {
        firebaseAnalytics.logEvent(Event.SETTINGS_OPENED, null)
    }

    /** Log a specific settings action (e.g. change_pin, security_question). */
    fun logSettingsAction(settingKey: String) {
        val bundle = Bundle().apply {
            putString(Param.SETTING_KEY, settingKey)
        }
        firebaseAnalytics.logEvent(Event.SETTINGS_ACTION, bundle)
    }

    fun logAdRequest(adFormat: String, adUnitId: String, placement: String) {
        val bundle = Bundle().apply {
            putString(Param.AD_FORMAT, adFormat)
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.PLACEMENT, placement)
        }
        firebaseAnalytics.logEvent(Event.AD_REQUEST, bundle)
    }

    fun logAdLoaded(adFormat: String, adUnitId: String, placement: String) {
        val bundle = Bundle().apply {
            putString(Param.AD_FORMAT, adFormat)
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.PLACEMENT, placement)
        }
        firebaseAnalytics.logEvent(Event.AD_LOADED, bundle)
    }

    fun logAdFailedToLoad(
        adFormat: String,
        adUnitId: String,
        errorCode: Int,
        errorMessage: String,
        placement: String
    ) {
        val bundle = Bundle().apply {
            putString(Param.AD_FORMAT, adFormat)
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.PLACEMENT, placement)
            putInt(Param.ERROR_CODE, errorCode)
            putString(Param.ERROR_MESSAGE, errorMessage)
        }
        firebaseAnalytics.logEvent(Event.AD_FAILED_LOAD, bundle)
    }

    fun logAdFailedToShow(adFormat: String, adUnitId: String, errorMessage: String) {
        val bundle = Bundle().apply {
            putString(Param.AD_FORMAT, adFormat)
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.ERROR_MESSAGE, errorMessage)
        }
        firebaseAnalytics.logEvent(Event.AD_FAILED_SHOW, bundle)
    }

    fun logAdOpened(adFormat: String, adUnitId: String, placement: String) {
        val bundle = Bundle().apply {
            putString(Param.AD_FORMAT, adFormat)
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.PLACEMENT, placement)
        }
        firebaseAnalytics.logEvent(Event.AD_OPENED, bundle)
    }

    fun logAdClicked(adFormat: String, adUnitId: String, placement: String) {
        val bundle = Bundle().apply {
            putString(Param.AD_FORMAT, adFormat)
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.PLACEMENT, placement)
        }
        firebaseAnalytics.logEvent(Event.AD_CLICKED, bundle)
    }

    fun logAdBannerImpression(adUnitId: String, placement: String) {
        val bundle = Bundle().apply {
            putString(Param.AD_UNIT_ID, adUnitId)
            putString(Param.PLACEMENT, placement)
        }
        firebaseAnalytics.logEvent(Event.AD_BANNER_IMPRESSION, bundle)
    }

    fun logInterstitialShowRequested(placement: String) {
        val bundle = Bundle().apply { putString(Param.PLACEMENT, placement) }
        firebaseAnalytics.logEvent(Event.INTERSTITIAL_SHOW_REQUESTED, bundle)
    }

    fun logInterstitialShown(placement: String) {
        val bundle = Bundle().apply { putString(Param.PLACEMENT, placement) }
        firebaseAnalytics.logEvent(Event.INTERSTITIAL_SHOWN, bundle)
    }

    fun logInterstitialDismissed(placement: String) {
        val bundle = Bundle().apply { putString(Param.PLACEMENT, placement) }
        firebaseAnalytics.logEvent(Event.INTERSTITIAL_DISMISSED, bundle)
    }

    /**
     * Logs standard GA4 [ad_impression](https://firebase.google.com/docs/analytics/measure-ad-revenue)
     * with AdMob paid event payload.
     */
    fun logAdRevenuePaid(
        adValue: AdValue,
        adFormat: String,
        adUnitId: String,
        mediationNetwork: String?
    ) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob")
            putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat)
            putString("ad_unit_id", adUnitId)
            putDouble(FirebaseAnalytics.Param.VALUE, adValue.valueMicros / 1_000_000.0)
            putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            mediationNetwork?.let { putString(Param.MEDIATION_NETWORK, it) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle)
    }

    /** Generic custom event with optional params. */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = params?.toBundle() ?: Bundle()
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    private fun Map<String, Any>.toBundle(): Bundle {
        return Bundle().apply {
            forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
    }
}

/** Get [VaultifyAnalytics] in a Composable (e.g. from Compose screens). */
@Composable
fun rememberVaultifyAnalytics(): VaultifyAnalytics {
    val context = LocalContext.current
    return remember(context) {
        val app = context.applicationContext as Application
        EntryPointAccessors.fromApplication(app, VaultifyAnalyticsEntryPoint::class.java)
            .vaultifyAnalytics()
    }
}
