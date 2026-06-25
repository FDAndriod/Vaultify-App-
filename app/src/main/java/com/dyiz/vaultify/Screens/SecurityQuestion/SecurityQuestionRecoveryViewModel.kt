package com.dyiz.vaultify.Screens.SecurityQuestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyiz.vaultify.data.PinRepository
import com.dyiz.vaultify.data.SecurityQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityQuestionRecoveryUiState(
    val question: String = "",
    val hint: String = "",
    val answer: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SecurityQuestionRecoveryViewModel @Inject constructor(
    private val securityQuestionRepository: SecurityQuestionRepository,
    private val pinRepository: PinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityQuestionRecoveryUiState())
    val uiState: StateFlow<SecurityQuestionRecoveryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val entity = securityQuestionRepository.get()
            _uiState.value = _uiState.value.copy(
                question = entity?.question ?: "",
                hint = entity?.hint ?: "",
                isLoading = false
            )
        }
    }

    fun updateAnswer(value: String) {
        _uiState.value = _uiState.value.copy(
            answer = value,
            errorMessage = null
        )
    }

    fun onSubmit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.answer.isBlank()) return
        viewModelScope.launch {
            val entity = securityQuestionRepository.get()
            if (entity == null) {
                _uiState.value = state.copy(errorMessage = "Wrong Answer Try Again")
                return@launch
            }
            if (state.answer.trim().equals(entity.answer.trim(), ignoreCase = false)) {
                pinRepository.deletePin()
                _uiState.value = state.copy(errorMessage = null)
                onSuccess()
            } else {
                _uiState.value = state.copy(errorMessage = "Wrong Answer Try Again")
            }
        }
    }
}
