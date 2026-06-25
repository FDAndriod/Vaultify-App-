package com.dyiz.vaultify.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dyiz.vaultify.R

private const val TOAST_OVERLAY_TAG = "vaultify_branded_toast_overlay"

/**
 * Brief message with the **Vaultify** in-app mark ([R.drawable.vaultifysplashicon] in layout), not the
 * platform launcher icon (adaptive icons still ship the default Android Studio robot foreground).
 */
fun Context.showBrandedToast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    val activity = findActivity()
    if (activity == null || activity.isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)) {
        Toast.makeText(this, message, duration).show()
        return
    }
    val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: run {
        Toast.makeText(this, message, duration).show()
        return
    }

    val delayMs = if (duration == Toast.LENGTH_LONG) 3500L else 2200L
    val density = activity.resources.displayMetrics.density
    val horizontalMargin = (20 * density).toInt()
    val verticalMargin = (16 * density).toInt()

    root.post {
        if (activity.isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)) {
            return@post
        }
        for (i in root.childCount - 1 downTo 0) {
            val child = root.getChildAt(i)
            if (child.tag == TOAST_OVERLAY_TAG) {
                root.removeView(child)
            }
        }

        val bar = LayoutInflater.from(activity).inflate(R.layout.view_branded_snackbar, root, false)
        bar.tag = TOAST_OVERLAY_TAG
        bar.findViewById<TextView>(R.id.branded_toast_text).text = message

        val bottomInset = ViewCompat.getRootWindowInsets(root)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())
            ?.bottom
            ?: 0

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin + bottomInset)
        }
        root.addView(bar, lp)
        bar.postDelayed({
            if (bar.parent === root) {
                root.removeView(bar)
            }
        }, delayMs)
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        val next = ctx.baseContext
        if (next === ctx) return null
        ctx = next
    }
    return this as? Activity
}
