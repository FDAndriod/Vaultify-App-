package com.dyiz.vaultify.analytics

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VaultifyAnalyticsEntryPoint {
    fun vaultifyAnalytics(): VaultifyAnalytics
}
