package com.dyiz.vaultify.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyiz.vaultify.data.NoteRepository
import com.dyiz.vaultify.data.VaultItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeCounts(
    val imageCount: Int = 0,
    val videoCount: Int = 0,
    val fileCount: Int = 0,
    val audioCount: Int = 0,
    val noteCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VaultItemRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _counts = MutableStateFlow(HomeCounts())
    val counts: StateFlow<HomeCounts> = _counts.asStateFlow()

    init {
        loadCounts()
    }

    fun loadCounts() {
        viewModelScope.launch {
            val imageCount = repository.getCountByType("IMAGE")
            val videoCount = repository.getCountByType("VIDEO")
            val fileCount = repository.getCountByType("FILE")
            val audioCount = repository.getCountByType("AUDIO")
            val noteCount = noteRepository.count()
            _counts.value = HomeCounts(
                imageCount = imageCount,
                videoCount = videoCount,
                fileCount = fileCount,
                audioCount = audioCount,
                noteCount = noteCount
            )
        }
    }
}
