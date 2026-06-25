package com.dyiz.vaultify.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * Single source of truth for the app's re-lock policy.
 *
 * The vault used to call `PinEnter` every time the activity was paused/resumed on a "protected"
 * route — which meant showing an interstitial ad, firing `MediaStore.createDeleteRequest`, or
 * briefly flipping to another app for a second all triggered a fresh PIN challenge.
 *
 * The new model has three primitives:
 *
 *  - [KEY_SHOULD_LOCK_ON_RESUME]: armed in `ON_PAUSE` while on a protected route. Unchanged.
 *  - [KEY_PAUSED_AT_MS]:          timestamp of that pause. Unchanged by skip-flag consumption.
 *  - [KEY_SKIP_LOCK_ON_RESUME]:   one-shot bypass for known-safe flows (pickers, ads, system
 *                                 delete dialog). Consumed on the next resume.
 *
 * Policy on resume:
 *   1. If `skip` flag is set → clear it, do NOT prompt (known-safe external flow).
 *   2. If `should_lock` is set AND we've been paused longer than [GRACE_PERIOD_MS] → prompt.
 *   3. Otherwise → clear the arm and do NOT prompt (grace window: covers ad overlays, system
 *      dialogs, permission prompts, and <30s app switches).
 *
 * The grace window is what fixes the user-reported "PIN → ad → PIN again" loop: the interstitial
 * pauses the activity briefly, but returning within 30s silently resumes the session.
 */
object AuthSession {
    private const val PREFS_NAME = "app_prefs"

    const val KEY_SHOULD_LOCK_ON_RESUME = "should_lock_on_resume"
    const val KEY_SKIP_LOCK_ON_RESUME = "skip_lock_on_resume"
    const val KEY_PAUSED_AT_MS = "paused_at_ms"

    /**
     * Maximum time a user can be off-activity before we still trust the session on return. Chosen
     * to comfortably swallow interstitial ads (typically 5–15s), system consent dialogs, permission
     * prompts, notification shades, and brief app-switches, while still re-locking after a real
     * departure.
     */
    const val GRACE_PERIOD_MS: Long = 30_000L

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Marks the next `ON_RESUME` as a trusted return — the lock will be bypassed exactly once.
     * Use this right before launching a system UI that we know pauses the activity (SAF pickers,
     * interstitial ads, `MediaStore.createDeleteRequest`, runtime permission dialogs, share sheet,
     * camera, etc.).
     */
    fun markSkipLock(context: Context) {
        prefs(context).edit().putBoolean(KEY_SKIP_LOCK_ON_RESUME, true).apply()
    }
}
