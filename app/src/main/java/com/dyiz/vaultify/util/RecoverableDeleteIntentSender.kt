package com.dyiz.vaultify.util

import android.app.PendingIntent
import android.content.IntentSender
import android.os.Build
import java.lang.reflect.InvocationTargetException

private const val RECOVERABLE_SECURITY_EXCEPTION = "android.app.RecoverableSecurityException"

/**
 * [android.app.RecoverableSecurityException] exists from API 29. Referencing that type in code
 * that runs on older devices causes [NoClassDefFoundError] when the VM resolves `instanceof`.
 * This helper uses reflection only after an API check so minSdk < 29 stays safe.
 */
fun Throwable.recoverableDeleteIntentSender(): IntentSender? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val causeSnapshot = cause
    val target = when {
        javaClass.name == RECOVERABLE_SECURITY_EXCEPTION -> this
        causeSnapshot != null && causeSnapshot.javaClass.name == RECOVERABLE_SECURITY_EXCEPTION ->
            causeSnapshot
        else -> return null
    }
    return extractIntentSenderFromRecoverable(target)
}

private fun extractIntentSenderFromRecoverable(recoverable: Throwable): IntentSender? {
    return try {
        val getUserAction = recoverable.javaClass.getMethod("getUserAction")
        val userAction = getUserAction.invoke(recoverable) ?: return null
        val getActionIntent = userAction.javaClass.getMethod("getActionIntent")
        val pendingIntent = getActionIntent.invoke(userAction) as? PendingIntent ?: return null
        pendingIntent.intentSender
    } catch (_: ReflectiveOperationException) {
        null
    } catch (_: InvocationTargetException) {
        null
    } catch (_: ClassCastException) {
        null
    }
}
