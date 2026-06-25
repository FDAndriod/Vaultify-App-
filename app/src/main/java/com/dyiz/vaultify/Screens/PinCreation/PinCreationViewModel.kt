package com.dyiz.vaultify.Screens.PinCreation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyiz.vaultify.R
import com.dyiz.vaultify.data.PinRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PinCreationUiState(
    val pinDigits: String = "",
    val isPinVisible: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class PinCreationViewModel @Inject constructor(
    private val pinRepository: PinRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinCreationUiState())
    val uiState: StateFlow<PinCreationUiState> = _uiState.asStateFlow()

    fun addDigit(digit: Char) {
        if (_uiState.value.pinDigits.length >= 4) return
        _uiState.value = _uiState.value.copy(
            pinDigits = _uiState.value.pinDigits + digit,
            errorMessage = null
        )
    }

    fun removeDigit() {
        val current = _uiState.value.pinDigits
        if (current.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            pinDigits = current.dropLast(1),
            errorMessage = null
        )
    }

    fun togglePinVisibility() {
        _uiState.value = _uiState.value.copy(isPinVisible = !_uiState.value.isPinVisible)
    }

    fun continueClicked(
        isEnterMode: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val pin = _uiState.value.pinDigits
        if (pin.length != 4) {
            val msg = appContext.getString(R.string.pin_error_required)
            _uiState.value = _uiState.value.copy(errorMessage = msg)
            onError(msg)
            return
        }
        viewModelScope.launch {
            if (isEnterMode) {
                val savedPin = pinRepository.getPin()?.pin
                if (savedPin == pin) {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                    onSuccess()
                } else {
                    val message = appContext.getString(R.string.wrong_pin)
                    _uiState.value = _uiState.value.copy(errorMessage = message)
                    onError(message)
                }
            } else {
                val existingPin = pinRepository.getPin()?.pin
                if (existingPin != null && existingPin == pin) {
                    val message = appContext.getString(R.string.pin_error_same_as_current)
                    _uiState.value = _uiState.value.copy(errorMessage = message)
                    onError(message)
                    return@launch
                }
                pinRepository.savePin(pin)
                _uiState.value = _uiState.value.copy(
                    isSaved = true,
                    errorMessage = null
                )
                onSuccess()
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
