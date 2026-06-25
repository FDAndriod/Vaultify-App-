package com.dyiz.vaultify.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyiz.vaultify.Database.NoteEntity
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.data.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NotesListUiState(
    val notes: List<NoteEntity> = emptyList(),
    val query: String = "",
    /** Id of the most recently saved note — used by the list to briefly highlight it. */
    val lastSavedId: Long? = null
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val analytics: VaultifyAnalytics
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val lastSavedId = MutableStateFlow<Long?>(null)

    /**
     * Combines the live Room stream with the in-memory search query and the "just saved" id so
     * the list filters + highlights without the composable having to do it itself.
     */
    val uiState: StateFlow<NotesListUiState> = combine(
        repository.observeAll(),
        query,
        lastSavedId
    ) { notes, q, savedId ->
        val filtered = if (q.isBlank()) {
            notes
        } else {
            val needle = q.trim().lowercase()
            notes.filter { note ->
                note.title.lowercase().contains(needle) ||
                        note.content.lowercase().contains(needle)
            }
        }
        NotesListUiState(notes = filtered, query = q, lastSavedId = savedId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotesListUiState()
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun clearLastSavedId() {
        lastSavedId.value = null
    }

    /** Saves a new note or updates an existing one. */
    fun save(
        id: Long?,
        title: String,
        content: String,
        colorArgb: Int?,
        pinned: Boolean,
        onSaved: (Long) -> Unit = {}
    ): Job = viewModelScope.launch {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (id == null) {
            val newId = repository.create(trimmedTitle, trimmedContent, colorArgb, pinned)
            lastSavedId.value = newId
            analytics.logEvent(
                "vaultify_note_saved",
                mapOf("action" to "create", "has_title" to trimmedTitle.isNotEmpty())
            )
            onSaved(newId)
        } else {
            repository.update(id, trimmedTitle, trimmedContent, colorArgb, pinned)
            lastSavedId.value = id
            analytics.logEvent(
                "vaultify_note_saved",
                mapOf("action" to "update", "has_title" to trimmedTitle.isNotEmpty())
            )
            onSaved(id)
        }
    }

    fun togglePin(id: Long) {
        viewModelScope.launch { repository.togglePin(id) }
    }

    fun delete(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(id)
            if (lastSavedId.value == id) lastSavedId.value = null
            analytics.logEvent("vaultify_note_deleted", mapOf("id" to id))
            onDeleted()
        }
    }

    fun deleteAllNotes(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllNotes()
            withContext(Dispatchers.Main) {
                onDone()
            }
        }
    }

    suspend fun getById(id: Long): NoteEntity? = repository.getById(id)
}
