package com.dyiz.vaultify.analytics

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideVaultifyAnalytics(
        @ApplicationContext context: Context
    ): VaultifyAnalytics = VaultifyAnalytics(context)
}
