package com.dyiz.vaultify.Screens

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyiz.vaultify.Database.VaultItemEntity
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.data.VaultItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val items: List<VaultItemEntity> = emptyList(),
    val isDeleteMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** After saving to vault, we need to hide original from gallery. UI should request delete for this URI. */
    val pendingDeleteUri: Uri? = null,
    /** Batch of URIs to add to pending delete queue (for multi-select add). */
    val pendingDeleteUris: List<Uri> = emptyList()
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultItemRepository,
    private val analytics: VaultifyAnalytics,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var currentType: String = ""

    fun loadForType(type: String) {
        currentType = type
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            _uiState.value = _uiState.value.copy(
                items = repository.getItemsByType(type),
                isLoading = false,
                isDeleteMode = false,
                selectedIds = emptySet()
            )
        }
    }

    /**
     * Re-reads the current vault slice WITHOUT clobbering UI state like delete-mode or
     * selection. Use this when an out-of-band screen (e.g. ImageViewer) mutates items and we
     * want the list to reflect the change on resume.
     */
    fun refresh() {
        if (currentType.isEmpty()) return
        viewModelScope.launch {
            val items = repository.getItemsByType(currentType)
            _uiState.value = _uiState.value.copy(items = items)
        }
    }

    //old logic
 /*   fun addFromUri(uri: Uri, displayName: String?, mimeType: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.saveFromUri(uri, currentType, displayName, mimeType)
                .onSuccess {
                    loadForType(currentType)
                    _uiState.value = _uiState.value.copy(pendingDeleteUri = uri)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
        }
    }*/

    //new logic
    fun addFromUri(uri: Uri, displayName: String?, mimeType: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.saveFromUri(uri, currentType, displayName, mimeType)
                .onSuccess {
                    analytics.logVaultItemAdded(currentType, 1)
                    val items = repository.getItemsByType(currentType)
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false,
                        pendingDeleteUri = uri
                    )
                }
                .onFailure {
                    val msg = it.message ?: it.javaClass.simpleName
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                }
        }
    }

    /**
     * Saves in-memory [bytes] as a new vault item of [type] (e.g. the OCR screen's generated
     * PDF). Deliberately does NOT set [VaultUiState.pendingDeleteUri] because we generated the
     * bytes ourselves — there is no original on the device to unhide, and we don't want to
     * trigger the delete-from-device consent dialog afterwards.
     *
     * [onDone] is invoked on the main thread after persistence completes so callers can navigate
     * / toast based on success. When the surrounding screen is currently displaying the same
     * [type], the UI state is refreshed so the new item shows up immediately in the list.
     */
    fun saveInternalBytes(
        type: String,
        displayName: String,
        mimeType: String?,
        bytes: ByteArray,
        onDone: (success: Boolean, newId: Long?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.saveFromBytes(bytes, type, displayName, mimeType)
                .onSuccess { id ->
                    analytics.logVaultItemAdded(type, 1)
                    if (currentType == type) {
                        val items = repository.getItemsByType(type)
                        _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                    onDone(true, id)
                }
                .onFailure {
                    val msg = it.message ?: it.javaClass.simpleName
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                    onDone(false, null)
                }
        }
    }

    fun clearPendingDeleteUri() {
        _uiState.value = _uiState.value.copy(pendingDeleteUri = null)
    }

    fun clearPendingDeleteUris() {
        _uiState.value = _uiState.value.copy(pendingDeleteUris = emptyList())
    }

    /** Add multiple items from URIs (for multi-select). */
    fun addFromUris(uris: List<Uri>, mimeType: String?) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val successUris = mutableListOf<Uri>()
            var lastError: String? = null
            for (uri in uris) {
                repository.saveFromUri(uri, currentType, null, mimeType)
                    .onSuccess {
                        successUris.add(uri)
                        analytics.logVaultItemAdded(currentType, 1)
                    }
                    .onFailure {
                        lastError = it.message ?: it.javaClass.simpleName
                    }
            }
            val items = repository.getItemsByType(currentType)
            _uiState.value = _uiState.value.copy(
                items = items,
                isLoading = false,
                errorMessage = lastError,
                pendingDeleteUris = successUris
            )
        }
    }

    fun enterDeleteMode() {
        _uiState.value = _uiState.value.copy(isDeleteMode = true, selectedIds = emptySet())
    }

    /**
     * Enters selection mode (if not already in it) and ensures [id] is part of the current
     * selection. Used by long-press on a card so the item the user actually touched is included
     * — a subtle but important UX detail.
     */
    fun startSelectionWith(id: Long) {
        val state = _uiState.value
        val newSelection = state.selectedIds + id
        _uiState.value = state.copy(isDeleteMode = true, selectedIds = newSelection)
    }

    fun exitDeleteMode() {
        _uiState.value = _uiState.value.copy(isDeleteMode = false, selectedIds = emptySet())
    }

    fun toggleSelection(id: Long) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (id in current) current - id else current + id
        )
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            selectedIds = _uiState.value.items.map { it.id }.toSet()
        )
    }

    /**
     * Permanently deletes the currently-selected vault items. Does NOT restore anything to the
     * gallery — the on-disk copy is removed and the DB row is dropped. Caller is responsible for
     * confirming consent (the UI shows a confirmation dialog first).
     */
    fun deleteSelectedPermanently(onDeleted: (Int) -> Unit = {}) {
        val ids = _uiState.value.selectedIds
        val items = _uiState.value.items.filter { it.id in ids }
        if (items.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.deletePermanently(items)
                .onSuccess { count ->
                    analytics.logVaultItemRemoved(currentType, count)
                    loadForType(currentType)
                    onDeleted(count)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
        }
    }

    /**
     * Restores a single [entity] to the gallery/public storage and removes it from the vault.
     * Used by the full-screen preview so users can unlock the item they're currently viewing
     * without having to back out into selection mode.
     */
    fun unhideSingle(
        entity: VaultItemEntity,
        onRestored: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                onError(appContext.getString(R.string.toast_unhide_requires_android10))
                return@launch
            }
            repository.unhideAndDelete(listOf(entity))
                .onSuccess {
                    analytics.logVaultItemUnhidden(entity.type, 1)
                    if (currentType == entity.type) refresh()
                    val location = entity.originalRelativePath?.trim()?.trimEnd('/')
                        ?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.toast_restore_location_default)
                    onRestored(
                        appContext.getString(R.string.toast_item_restored, entity.displayName, location)
                    )
                }
                .onFailure { onError(it.message ?: it.javaClass.simpleName) }
        }
    }

    /**
     * Permanently removes a single [entity] from the vault. No restore. Used by the full-screen
     * preview. Caller is responsible for confirming with the user first.
     */
    fun deleteSinglePermanently(
        entity: VaultItemEntity,
        onDeleted: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.deletePermanently(listOf(entity))
                .onSuccess {
                    analytics.logVaultItemRemoved(entity.type, 1)
                    if (currentType == entity.type) refresh()
                    onDeleted()
                }
                .onFailure { onError(it.message ?: it.javaClass.simpleName) }
        }
    }

    /**
     * Bulk action: permanently deletes EVERY item currently loaded for [currentType], regardless
     * of selection state. Used by the top-bar "delete all" shortcut. Caller is responsible for
     * showing a confirmation dialog first — this method will just run.
     */
    fun deleteAllPermanently(onDeleted: (Int) -> Unit = {}) {
        val items = _uiState.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.deletePermanently(items)
                .onSuccess { count ->
                    analytics.logVaultItemRemoved(currentType, count)
                    loadForType(currentType)
                    onDeleted(count)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                }
        }
    }

    /**
     * Bulk action: unlocks (restores) EVERY item currently loaded for [currentType]. Used by the
     * top-bar "unlock all" shortcut. Caller should confirm first.
     */
    fun unhideAll(onRestored: (List<String>) -> Unit = {}) {
        val items = _uiState.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = appContext.getString(R.string.toast_unhide_requires_android10)
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.unhideAndDelete(items)
                .onSuccess {
                    analytics.logVaultItemUnhidden(currentType, items.size)
                    loadForType(currentType)
                    val restoredInfo = items.map { entity ->
                        val location = entity.originalRelativePath?.trim()?.trimEnd('/')
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.toast_restore_location_default)
                        appContext.getString(R.string.toast_item_restored, entity.displayName, location)
                    }
                    onRestored(restoredInfo)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
        }
    }

    fun unhideSelected(onRestored: (List<String>) -> Unit = {}) {
        val ids = _uiState.value.selectedIds
        val items = _uiState.value.items.filter { it.id in ids }
        if (items.isEmpty()) return
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = appContext.getString(R.string.toast_unhide_requires_android10)
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.unhideAndDelete(items)
                .onSuccess {
                    analytics.logVaultItemUnhidden(currentType, items.size)
                    loadForType(currentType)
                    val restoredInfo = items.map { entity ->
                        val location = entity.originalRelativePath?.trim()?.trimEnd('/')
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.toast_restore_location_default)
                        appContext.getString(R.string.toast_item_restored, entity.displayName, location)
                    }
                    onRestored(restoredInfo)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
        }
    }
}
