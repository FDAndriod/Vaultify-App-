package com.dyiz.vaultify

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.dyiz.vaultify.DisguiseIconHelper.DEFAULT_ALIAS
import com.dyiz.vaultify.DisguiseIconHelper.DISGUISE_ALIASES
import com.dyiz.vaultify.DisguiseIconHelper.applyDisguise

object DisguiseIconHelper {
    private const val PREFS_NAME = "disguise_prefs"
    private const val KEY_SELECTED_ALIAS = "selected_alias"

    /**
     * The canonical, manifest-default alias. Kept outside [DISGUISE_ALIASES] order-wise as a
     * constant because several code paths (debug safeguard, recovery reset, default preference
     * value) need a stable reference to it.
     */
    const val DEFAULT_ALIAS = "Vaultify"

    /**
     * Every alias declared as an `<activity-alias>` in `AndroidManifest.xml`. [applyDisguise]
     * walks this list on each change to enable the chosen one and disable the rest, so any new
     * alias added to the manifest must also be added here or the component state will go stale.
     */
    val DISGUISE_ALIASES = listOf(
        DEFAULT_ALIAS,
        "Weather",
        "Converter",
        "Music",
        "Calendar",
        "Mealio",
        "Calculator",
        "Notes",
        "Clock",
        "Flashlight",
        "Wallet",
        "Fitness"
    )

    fun getSelectedAlias(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ALIAS, DEFAULT_ALIAS) ?: DEFAULT_ALIAS
    }

    fun setSelectedAlias(context: Context, alias: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_ALIAS, alias)
            .apply()
    }

    /**
     * Enables `alias` and disables every other alias.
     *
     * Debug-build safeguard: the canonical [DEFAULT_ALIAS] (`.MainActivity.Vaultify`) is NEVER
     * disabled in debug builds. Android Studio's default run configuration is pinned to that
     * component at install time; disabling it makes subsequent `am start` / "Run 'app'" invocations
     * fail with `Activity class {…MainActivity.Vaultify} does not exist`. In debug builds we
     * accept the cosmetic side-effect of an extra launcher icon during development so that the
     * run/debug flow always works. Release builds keep the strict single-launcher behavior.
     */
    fun applyDisguise(context: Context, alias: String) {
        val packageName = context.packageName
        val pm = context.packageManager
        val isDebug = (context.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        for (name in DISGUISE_ALIASES) {
            val componentName = ComponentName(packageName, "$packageName.MainActivity.$name")
            val shouldEnable = name == alias ||
                (isDebug && name == DEFAULT_ALIAS)
            val state = if (shouldEnable) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        setSelectedAlias(context, alias)
    }

    /**
     * Recovery path for users stuck on `Activity class … does not exist` (e.g. an old launcher
     * shortcut pointing to a now-disabled alias). Forces the state back to [DEFAULT_ALIAS]
     * without going through the UI. Safe to call on every app start as a self-healing step.
     */
    fun resetToDefault(context: Context) {
        applyDisguise(context, DEFAULT_ALIAS)
    }
}
