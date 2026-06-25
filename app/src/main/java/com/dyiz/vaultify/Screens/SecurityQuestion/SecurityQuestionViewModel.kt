package com.dyiz.vaultify.Screens.SecurityQuestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyiz.vaultify.data.SecurityQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityQuestionUiState(
    val question: String = "",
    val answer: String = "",
    val hint: String = ""
)

@HiltViewModel
class SecurityQuestionViewModel @Inject constructor(
    private val securityQuestionRepository: SecurityQuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityQuestionUiState())
    val uiState: StateFlow<SecurityQuestionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }
    private fun loadData() {
        viewModelScope.launch {
            val entity = securityQuestionRepository.get()
            entity?.let {
                _uiState.value = SecurityQuestionUiState(
                    question = it.question,
                    answer = it.answer,
                    hint = it.hint ?: ""
                )
            }
        }
    }
    fun updateQuestion(value: String) {
        _uiState.value = _uiState.value.copy(question = value)
    }

    fun updateAnswer(value: String) {
        _uiState.value = _uiState.value.copy(answer = value)
    }

    fun updateHint(value: String) {
        _uiState.value = _uiState.value.copy(hint = value)
    }

    fun onSetClick(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.question.isBlank() || state.answer.isBlank()) return
        viewModelScope.launch {
            securityQuestionRepository.save(
                question = state.question.trim(),
                answer = state.answer.trim(),
                hint = state.hint.trim()
            )
            onSaved()
        }
    }
}
