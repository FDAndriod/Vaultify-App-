package com.dyiz.vaultify.Screens

import androidx.lifecycle.ViewModel
import com.dyiz.vaultify.data.PinRepository
import com.dyiz.vaultify.data.SecurityQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val pinRepository: PinRepository,
    private val securityQuestionRepository: SecurityQuestionRepository
) : ViewModel() {

    /** True only when both PIN and Security Question are saved (setup complete). */
    suspend fun isSetupComplete(): Boolean = withContext(Dispatchers.IO) {
        pinRepository.getPin() != null && securityQuestionRepository.get() != null
    }
}
