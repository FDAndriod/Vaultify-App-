package com.dyiz.vaultify.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {

            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName.toString()
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown".toString()
    } catch (e: Exception) {
        "Error".toString()
    }
}
