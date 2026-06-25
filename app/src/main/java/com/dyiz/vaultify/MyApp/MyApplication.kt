package com.dyiz.vaultify.MyApp

import android.app.Application
import com.dyiz.vaultify.DisguiseIconHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            DisguiseIconHelper.applyDisguise(
                this@MyApplication,
                DisguiseIconHelper.getSelectedAlias(this@MyApplication)
            )
        }
    }
}