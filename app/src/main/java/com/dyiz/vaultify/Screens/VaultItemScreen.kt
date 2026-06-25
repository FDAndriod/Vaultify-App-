package com.dyiz.vaultify.Screens

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dyiz.vaultify.Database.VaultItemEntity
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.auth.AuthSession
import com.dyiz.vaultify.ui.VaultifyConfirmPrimaryStyle
import com.dyiz.vaultify.ui.VaultifyThemedConfirmDialog
import com.dyiz.vaultify.util.recoverableDeleteIntentSender
import com.dyiz.vaultify.utils.ScanFlowState
import com.dyiz.vaultify.utils.showBrandedToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thread-unsafe handoff bag used to pass the viewer its working set without encoding huge
 * entity objects through the nav graph arguments. Holds the full list of vault entities being
 * viewed along with the page to open on. The list screen writes to this right before
 * navigating; the viewer mutates it in place as items are unlocked/deleted so going back
 * updates correctly.
 */
object ImageViewerSharedState {
    var items: List<VaultItemEntity> = emptyList()
    var startIndex: Int = 0
    val paths: List<String> get() = items.map { it.localFilePath }
}

/**
 * One-shot handoff for single-item previews (video / audio). The list screen writes the entity
 * before it calls navigate(...); the preview screen reads it on composition. We don't encode it
 * in the nav route because entities carry DB ids and file metadata that would bloat the URL.
 */
object MediaPreviewSharedState {
    var currentItem: VaultItemEntity? = null
}

/**
 * One-shot handoff for the in-app file preview (PDF / text / images / unsupported). Kept
 * separate from [MediaPreviewSharedState] so the two viewers don't stomp on each other if the
 * user quickly backs out of one and opens the other — each preview has its own state pouch.
 */
object FilePreviewSharedState {
    var currentItem: VaultItemEntity? = null
}

/**
 * Shared bottom action bar for the image / video / audio / file preview screens. Each preview
 * owns the callbacks, but the look (icon + label, split 50/50, branded colors, 48dp tall)
 * stays identical across screens so the gesture for "unlock this" or "delete this" feels
 * consistent. Visibility is [internal] so sibling preview screens in the same module (e.g.
 * [FilePreviewScreen]) can reuse it without duplicating 50 lines of layout.
 */
@Composable
internal fun PreviewActionBar(
    onUnlock: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onUnlock,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.splashhalftextColor),
                contentColor = Color.Black,
                disabledContainerColor = colorResource(id = R.color.deletebtncolor),
                disabledContentColor = colorResource(id = R.color.disbalebtntextcolor)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.unlockicon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Black
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_unlock),
                fontSize = 15.sp,
                fontFamily = FontFamily(Font(R.font.roboto_medium))
            )
        }
        Button(
            onClick = onDelete,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.wronganswercolor),
                contentColor = Color.White,
                disabledContainerColor = colorResource(id = R.color.deletebtncolor),
                disabledContentColor = colorResource(id = R.color.disbalebtntextcolor)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.newvaultdeleicon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_delete),
                fontSize = 15.sp,
                fontFamily = FontFamily(Font(R.font.roboto_medium))
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultItemScreen(
    navController: NavHostController,
    title: String,
    type: String,
    emptyMessage: String,
    buttonText: String,
    emptyImageResId: Int,
    viewModel: VaultViewModel,
    onUploadClick: () -> Unit,
    uploadButtonColor: Color? = null,
    onScanClick: (() -> Unit)? = null,
    /**
     * Id of an item the caller wants briefly flagged in the grid — used by the OCR flow so the
     * freshly-saved PDF is scrolled into view and visually emphasised on return. The screen
     * manages the highlight lifecycle (display for ~2.5s, then clear) internally; callers just
     * supply the id on first composition.
     */
    highlightItemId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val activity = context as? Activity
    var uriToDeleteAfterAllow by remember { mutableStateOf<Uri?>(null) }
    val cdBack = stringResource(R.string.cd_back)
    val cdScanDocument = stringResource(R.string.cd_scan_document)
    val cdExitSelection = stringResource(R.string.cd_exit_selection_mode)
    val cdUnlockAll = stringResource(R.string.cd_unlock_all)
    val cdDeleteAll = stringResource(R.string.cd_delete_all)

    val analyticsScreenName = when (type.uppercase()) {
        "IMAGE" -> VaultifyAnalytics.Screen.IMAGES
        "VIDEO" -> VaultifyAnalytics.Screen.VIDEOS
        "FILE" -> VaultifyAnalytics.Screen.DOCS
        "AUDIO" -> VaultifyAnalytics.Screen.AUDIOS
        else -> title
    }
    LaunchedEffect(analyticsScreenName) {
        analytics.logScreenView(analyticsScreenName, "VaultItemScreen")
    }
    val pendingDeleteQueue = remember { mutableStateListOf<Uri>() }
    var dialogInProgress by remember { mutableStateOf(false) }
    var currentUriForDialog by remember { mutableStateOf<Uri?>(null) }
    // Tracks URIs bundled into a single MediaStore.createDeleteRequest popup (IMAGE/VIDEO, API R+).
    // When non-empty, the next activity result corresponds to a batched delete — not a single URI.
    var batchDeleteUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    // Confirmation dialog state for permanent delete. We ask for explicit consent before removing
    // vault entries, since (unlike unhide) there's no way to recover the file afterwards.
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Top-bar bulk actions: wipe everything / restore everything. Both behind a confirmation
    // dialog since they affect the entire vault slice.
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showUnlockAllConfirm by remember { mutableStateOf(false) }

    // Transient "just saved" highlight driven by [highlightItemId]. We snapshot the id into local
    // state the first time the screen sees it, auto-scroll it into view, and clear after a short
    // window so the pulse animation ends and later compositions don't keep flagging the same
    // item. Independent of selection state — a highlighted item is not selected.
    val gridState = rememberLazyGridState()
    var highlightedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(highlightItemId) {
        if (highlightItemId != null) highlightedId = highlightItemId
    }
    LaunchedEffect(highlightedId, uiState.items) {
        val id = highlightedId ?: return@LaunchedEffect
        val index = uiState.items.indexOfFirst { it.id == id }
        if (index >= 0) {
            // Items are sorted newest-first so this is typically index 0, but scrolling
            // explicitly guards against the case where the list was already scrolled down when
            // the user kicked off the OCR flow.
            try { gridState.animateScrollToItem(index) } catch (_: Exception) {}
        }
        delay(2500)
        highlightedId = null
    }

    LaunchedEffect(uiState.pendingDeleteUri) {
        val uri = uiState.pendingDeleteUri ?: return@LaunchedEffect
        if (!pendingDeleteQueue.contains(uri)) {
            pendingDeleteQueue.add(uri)
        }
        delay(100)
        viewModel.clearPendingDeleteUri()
    }

    LaunchedEffect(uiState.pendingDeleteUris) {
        val uris = uiState.pendingDeleteUris
        if (uris.isEmpty()) return@LaunchedEffect
        uris.forEach { uri ->
            if (!pendingDeleteQueue.contains(uri)) {
                pendingDeleteQueue.add(uri)
            }
        }
        delay(100)
        viewModel.clearPendingDeleteUris()
    }

    val deleteFromDeviceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    )
    { result ->
        val batch = batchDeleteUris
        if (batch.isNotEmpty()) {
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.loadForType(type)
            } else if (type.equals("IMAGE", ignoreCase = true) || type.equals("VIDEO", ignoreCase = true)) {
                context.showBrandedToast(context.getString(R.string.toast_vault_copy_kept_in_gallery))
            }
            pendingDeleteQueue.clear()
            batchDeleteUris = emptyList()
            currentUriForDialog = null
            uriToDeleteAfterAllow = null
            dialogInProgress = false
            viewModel.clearPendingDeleteUri()
            viewModel.clearPendingDeleteUris()
            return@rememberLauncherForActivityResult
        }
        val uriToDelete = uriToDeleteAfterAllow ?: currentUriForDialog
        if (result.resultCode == Activity.RESULT_OK) {
            if (uriToDelete != null) {
                try {
                    val deleted = if (DocumentsContract.isDocumentUri(context, uriToDelete)) {
                        if (DocumentsContract.deleteDocument(context.contentResolver, uriToDelete)) 1 else 0
                    } else {
                        context.contentResolver.delete(uriToDelete, null, null)
                    }
                    if (deleted > 0) viewModel.loadForType(type)
                } catch (_: Exception) { }
            }
            viewModel.loadForType(type)
            uriToDeleteAfterAllow = null
        } else {
            if (uriToDelete != null && (type.equals("IMAGE", ignoreCase = true) || type.equals("VIDEO", ignoreCase = true))) {
                context.showBrandedToast(context.getString(R.string.toast_vault_copy_kept_in_gallery))
            }
        }
        if (pendingDeleteQueue.isNotEmpty()) pendingDeleteQueue.removeAt(0)
        currentUriForDialog = null
        dialogInProgress = false
        viewModel.clearPendingDeleteUri()
    }

    // Thin wrapper around `deleteFromDeviceLauncher.launch(...)` that marks the session as
    // "skip next re-lock" before launching the system consent dialog. Every delete path in this
    // screen goes through this so returning from the Allow/Deny dialog never re-prompts the PIN.
    val launchDeleteDialog: (IntentSenderRequest) -> Unit = remember(deleteFromDeviceLauncher) {
        { req ->
            AuthSession.markSkipLock(context)
            deleteFromDeviceLauncher.launch(req)
        }
    }

    LaunchedEffect(pendingDeleteQueue.size, dialogInProgress) {
        if (dialogInProgress || pendingDeleteQueue.isEmpty()) return@LaunchedEffect
        if (activity == null) {
            pendingDeleteQueue.clear()
            return@LaunchedEffect
        }
        val resolver = context.contentResolver
        val isMediaType = type.equals("IMAGE", ignoreCase = true) || type.equals("VIDEO", ignoreCase = true)

        // Batch path: on Android R+ for IMAGE/VIDEO, consolidate every pending URI into a single
        // MediaStore.createDeleteRequest so the user sees only ONE "Allow/Deny" popup per upload
        // batch — regardless of whether they added 1, 2, or N items.
        if (isMediaType && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val snapshot = pendingDeleteQueue.toList()
            // Only MediaStore-backed URIs are deletable via createDeleteRequest. URIs we can't
            // resolve (e.g. cloud-backed photo-picker URIs) cannot be removed — don't include
            // them in the batch or the whole request will throw and the gallery copy stays.
            val mediaUris = snapshot.mapNotNull { toMediaStoreUriWithId(context, it, type) }
            if (mediaUris.isEmpty()) {
                // Nothing resolvable to a MediaStore URI — e.g. the user picked an item routed
                // through a cloud provider (Drive, Dropbox) via SAF. We already saved an encrypted
                // copy; the original just can't be deleted through MediaStore because it doesn't
                // live there.
                pendingDeleteQueue.clear()
                dialogInProgress = false
                context.showBrandedToast(context.getString(R.string.toast_vault_copy_kept_in_gallery))
                viewModel.clearPendingDeleteUri()
                viewModel.clearPendingDeleteUris()
                return@LaunchedEffect
            }
            dialogInProgress = true
            batchDeleteUris = mediaUris
            currentUriForDialog = mediaUris.first()
            uriToDeleteAfterAllow = null
            try {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, mediaUris)
                withContext(Dispatchers.Main.immediate) {
                    launchDeleteDialog(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    )
                }
            } catch (_: Exception) {
                batchDeleteUris = emptyList()
                pendingDeleteQueue.clear()
                currentUriForDialog = null
                dialogInProgress = false
                context.showBrandedToast(context.getString(R.string.toast_vault_copy_kept_in_gallery))
                viewModel.clearPendingDeleteUri()
                viewModel.clearPendingDeleteUris()
            }
            return@LaunchedEffect
        }

        val uri = pendingDeleteQueue.first()
        currentUriForDialog = uri
        dialogInProgress = true
        val skipSilentDeleteForMedia = isMediaType && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val isDocumentUri = DocumentsContract.isDocumentUri(context, uri)
        if (!skipSilentDeleteForMedia || (type.equals("FILE", ignoreCase = true) && isDocumentUri) || type.equals("AUDIO", ignoreCase = true)) {
            try {
                val deleted = when {
                    type.equals("FILE", ignoreCase = true) && isDocumentUri -> if (DocumentsContract.deleteDocument(resolver, uri)) 1 else 0
                    type.equals("AUDIO", ignoreCase = true) && isDocumentUri -> if (DocumentsContract.deleteDocument(resolver, uri)) 1 else 0
                    else -> resolver.delete(uri, null, null)
                }
                if (deleted > 0) {
                    pendingDeleteQueue.removeAt(0)
                    currentUriForDialog = null
                    dialogInProgress = false
                    viewModel.loadForType(type)
                    return@LaunchedEffect
                }
            } catch (e: SecurityException) {
                val deleteSender = e.recoverableDeleteIntentSender()
                if (deleteSender != null) {
                    uriToDeleteAfterAllow = uri
                    try {
                        withContext(Dispatchers.Main.immediate) {
                            launchDeleteDialog(
                                IntentSenderRequest.Builder(deleteSender).build()
                            )
                        }
                    } catch (_: Exception) {
                        uriToDeleteAfterAllow = null
                        pendingDeleteQueue.removeAt(0)
                        currentUriForDialog = null
                        dialogInProgress = false
                        viewModel.clearPendingDeleteUri()
                    }
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                val deleteSender = e.recoverableDeleteIntentSender()
                if (deleteSender != null && (type.equals("FILE", ignoreCase = true) && isDocumentUri || type.equals("AUDIO", ignoreCase = true))) {
                    uriToDeleteAfterAllow = uri
                    try {
                        withContext(Dispatchers.Main.immediate) {
                            launchDeleteDialog(
                                IntentSenderRequest.Builder(deleteSender).build()
                            )
                        }
                    } catch (_: Exception) {
                        uriToDeleteAfterAllow = null
                        pendingDeleteQueue.removeAt(0)
                        currentUriForDialog = null
                        dialogInProgress = false
                        viewModel.clearPendingDeleteUri()
                    }
                    return@LaunchedEffect
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mediaUri = toMediaStoreUriWithId(context, uri, type)
            val uriToDelete = mediaUri ?: uri
            // Must match the URI passed to createDeleteRequest so the post-consent delete path is consistent (incl. IMAGE).
            uriToDeleteAfterAllow = uriToDelete
            try {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uriToDelete))
                withContext(Dispatchers.Main.immediate) {
                    launchDeleteDialog(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    )
                }
            } catch (_: Exception) {
                if (type.equals("AUDIO", ignoreCase = true)) {
                    try {
                        val deleted = if (DocumentsContract.isDocumentUri(context, uri)) {
                            if (DocumentsContract.deleteDocument(resolver, uri)) 1 else 0
                        } else {
                            resolver.delete(uri, null, null)
                        }
                        if (deleted > 0) {
                            pendingDeleteQueue.removeAt(0)
                            currentUriForDialog = null
                            dialogInProgress = false
                            viewModel.loadForType(type)
                        } else {
                            pendingDeleteQueue.removeAt(0)
                            currentUriForDialog = null
                            dialogInProgress = false
                            viewModel.clearPendingDeleteUri()
                        }
                    } catch (e2: SecurityException) {
                        val deleteSender = e2.recoverableDeleteIntentSender()
                        if (deleteSender != null) {
                            uriToDeleteAfterAllow = uri
                            try {
                                withContext(Dispatchers.Main.immediate) {
                                    launchDeleteDialog(IntentSenderRequest.Builder(deleteSender).build())
                                }
                            } catch (_: Exception) {
                                uriToDeleteAfterAllow = null
                                pendingDeleteQueue.removeAt(0)
                                currentUriForDialog = null
                                dialogInProgress = false
                                viewModel.clearPendingDeleteUri()
                            }
                        } else {
                            uriToDeleteAfterAllow = null
                            pendingDeleteQueue.removeAt(0)
                            currentUriForDialog = null
                            dialogInProgress = false
                            viewModel.clearPendingDeleteUri()
                        }
                    } catch (e2: Exception) {
                        val deleteSender = e2.recoverableDeleteIntentSender()
                        if (deleteSender != null) {
                            uriToDeleteAfterAllow = uri
                            try {
                                withContext(Dispatchers.Main.immediate) {
                                    launchDeleteDialog(IntentSenderRequest.Builder(deleteSender).build())
                                }
                            } catch (_: Exception) {
                                uriToDeleteAfterAllow = null
                                pendingDeleteQueue.removeAt(0)
                                currentUriForDialog = null
                                dialogInProgress = false
                                viewModel.clearPendingDeleteUri()
                            }
                        } else {
                            uriToDeleteAfterAllow = null
                            pendingDeleteQueue.removeAt(0)
                            currentUriForDialog = null
                            dialogInProgress = false
                            viewModel.clearPendingDeleteUri()
                        }
                    }
                } else {
                    pendingDeleteQueue.removeAt(0)
                    currentUriForDialog = null
                    dialogInProgress = false
                    viewModel.clearPendingDeleteUri()
                }
            }
        } else {
            // API 29 and below: no MediaStore#createDeleteRequest — retry delete (first pass may have been skipped for media on Q–).
            try {
                val deleted = when {
                    type.equals("FILE", ignoreCase = true) && DocumentsContract.isDocumentUri(context, uri) ->
                        if (DocumentsContract.deleteDocument(resolver, uri)) 1 else 0
                    type.equals("AUDIO", ignoreCase = true) && DocumentsContract.isDocumentUri(context, uri) ->
                        if (DocumentsContract.deleteDocument(resolver, uri)) 1 else 0
                    else -> resolver.delete(uri, null, null)
                }
                if (deleted > 0) {
                    viewModel.loadForType(type)
                }
            } catch (e: SecurityException) {
                val deleteSender = e.recoverableDeleteIntentSender()
                if (deleteSender != null) {
                    uriToDeleteAfterAllow = uri
                    try {
                        withContext(Dispatchers.Main.immediate) {
                            launchDeleteDialog(
                                IntentSenderRequest.Builder(deleteSender).build()
                            )
                        }
                        return@LaunchedEffect
                    } catch (_: Exception) {
                        uriToDeleteAfterAllow = null
                    }
                }
            } catch (_: Exception) { }
            pendingDeleteQueue.removeAt(0)
            currentUriForDialog = null
            dialogInProgress = false
            viewModel.clearPendingDeleteUri()
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )

    LaunchedEffect(type) {
        viewModel.loadForType(type)
    }

    // Pick up mutations that happen out-of-band (e.g. unlock / delete from ImageViewerScreen
    // or when the user returns from the system picker). refresh() reloads items without
    // resetting delete-mode/selection state, so in-progress workflows are preserved.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        analytics.logButtonClick("vault_back", analyticsScreenName);
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Image(
                            painter = painterResource(id = R.drawable.backarrowbluecolor),
                            contentDescription = cdBack,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                },
                actions = {
                    //Scanner Btn
                    if (onScanClick != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(Color(0xFF222F39), RoundedCornerShape(8.dp))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    analytics.logOcrScanStart("docs_screen"); onScanClick.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.scanericon),
                                contentDescription = cdScanDocument
                            )
                        }
                    }
                    if (uiState.items.isNotEmpty()) {
                        if (uiState.isDeleteMode) {
                            IconButton(onClick = {
                                analytics.logButtonClick("exit_delete_mode", analyticsScreenName)
                                viewModel.exitDeleteMode()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = cdExitSelection,
                                    tint = Color.White
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(36.dp)
                                    .background(Color(0xFF222F39), RoundedCornerShape(8.dp))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        analytics.logButtonClick(
                                            "vault_unhide_all_prompt",
                                            analyticsScreenName
                                        )
                                        showUnlockAllConfirm = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.unlockiconvault),
                                    contentDescription = cdUnlockAll
                                )
                            }
                            // Delete btn
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(36.dp)
                                    .background(Color(0xFF222F39), RoundedCornerShape(8.dp))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        analytics.logButtonClick(
                                            "vault_delete_all_prompt",
                                            analyticsScreenName
                                        )
                                        showDeleteAllConfirm = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.newvaultdeleicon),
                                    contentDescription = cdDeleteAll
                                )
                            }
                            if (onScanClick != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBackground)
                    .padding(paddingValues)
//                    .imePadding()
                .navigationBarsPadding()
            ) {
                if (uiState.isLoading && uiState.items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorResource(id = R.color.splashhalftextColor))
                    }
                    return@Box
                }
                uiState.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = colorResource(id = R.color.wronganswercolor),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                if (uiState.items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = emptyImageResId),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 280.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = emptyMessage,
                                color = colorResource(id = R.color.pinscreesubtextcolor),
                                fontSize = 15.sp,
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                                textAlign = TextAlign.Center
                            )
                        }
                        val emptyBtnColor =
                            uploadButtonColor ?: colorResource(id = R.color.splashhalftextColor)
                        Button(
                            onClick = { analytics.logButtonClick("vault_upload_empty", analyticsScreenName); onUploadClick() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emptyBtnColor,
                                contentColor = if (uploadButtonColor != null) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (uploadButtonColor != null) Color.White else Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buttonText,
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily(Font(R.font.roboto_medium))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                else {
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)) {
                        AnimatedVisibility(
                            visible = uiState.isDeleteMode,
                            enter = fadeIn() + slideInVertically { -it },
                            exit = fadeOut() + slideOutVertically { -it }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { analytics.logButtonClick("vault_select_all", analyticsScreenName); viewModel.selectAll() }) {
                                    Text(
                                        text = stringResource(R.string.action_select_all),
                                        color = colorResource(id = R.color.white),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily(Font(R.font.roboto_medium))
                                    )
                                }
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                        ) {
                            items(uiState.items, key = { it.id }) { item ->
                                VaultItemCard(
                                    item = item,
                                    type = type,
                                    isDeleteMode = uiState.isDeleteMode,
                                    isSelected = item.id in uiState.selectedIds,
                                    isHighlighted = item.id == highlightedId,
                                    onToggleSelection = { viewModel.toggleSelection(item.id) },
                                    onLongPress = {
                                        analytics.logButtonClick(
                                            "vault_long_press_select",
                                            analyticsScreenName
                                        )
                                        viewModel.startSelectionWith(item.id)
                                    },
                                    onItemClick = when {
                                        type == "IMAGE" && !uiState.isDeleteMode -> ({ entity ->
                                            ImageViewerSharedState.items = uiState.items
                                            ImageViewerSharedState.startIndex =
                                                uiState.items.indexOfFirst { it.id == entity.id }
                                                    .coerceAtLeast(0)
                                            navController.navigate("${NavRoutes.IMAGE_VIEWER_SCREEN}/${ImageViewerSharedState.startIndex}")
                                        })
                                        type == "VIDEO" && !uiState.isDeleteMode -> ({ entity ->
                                            MediaPreviewSharedState.currentItem = entity
                                            navController.navigate("${NavRoutes.VIDEO_PLAYING_SCREEN}/${Uri.encode(entity.localFilePath, "")}")
                                        })
                                        type == "AUDIO" && !uiState.isDeleteMode -> ({ entity ->
                                            MediaPreviewSharedState.currentItem = entity
                                            navController.navigate("${NavRoutes.AUDIO_PLAYING_SCREEN}/${Uri.encode(entity.localFilePath, "")}")
                                        })
                                        // FILE covers both the Files and Docs vault slices.
                                        // FilePreviewScreen branches on extension/mime to pick
                                        // the right renderer (PDF / text / image / fallback).
                                        type == "FILE" && !uiState.isDeleteMode -> ({ entity ->
                                            FilePreviewSharedState.currentItem = entity
                                            navController.navigate(NavRoutes.FILE_PREVIEW_SCREEN)
                                        })
                                        else -> null
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimatedVisibility(
                            visible = uiState.isDeleteMode,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            // Bottom action bar in selection mode. Icon + label pair so users
                            // can tell the two operations apart at a glance without reading.
                            //   Unlock (left)  — restores selected items to the gallery / files.
                            //   Delete (right) — permanently removes the vault copy (with confirm).
                            val actionsEnabled = uiState.selectedIds.isNotEmpty()
                            val enabledContent = colorResource(id = R.color.white)
                            val disabledContent = colorResource(id = R.color.disbalebtntextcolor)
                            val disabledContainer = colorResource(id = R.color.deletebtncolor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        analytics.logButtonClick("vault_unhide_selected", analyticsScreenName)
                                        viewModel.unhideSelected { restoredItems ->
                                            restoredItems.forEach { info ->
                                                context.showBrandedToast(info, Toast.LENGTH_LONG)
                                            }
                                        }
                                    },
                                    enabled = actionsEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.splashhalftextColor),
                                        contentColor = Color.Black,
                                        disabledContainerColor = disabledContainer,
                                        disabledContentColor = disabledContent
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.unlockicon),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.action_unlock_selected,
                                                uiState.selectedIds.size
                                            ),
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        analytics.logButtonClick("vault_delete_selected_confirm", analyticsScreenName)
                                        showDeleteConfirm = true
                                    },
                                    enabled = actionsEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.wronganswercolor),
                                        contentColor = enabledContent,
                                        disabledContainerColor = disabledContainer,
                                        disabledContentColor = disabledContent
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.delicon),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = enabledContent
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.action_delete_selected,
                                                uiState.selectedIds.size
                                            ),
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                                        )
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = !uiState.isDeleteMode,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            val btnColor =
                                uploadButtonColor ?: colorResource(id = R.color.splashhalftextColor)
                            Button(
                                onClick = { analytics.logButtonClick("vault_upload", analyticsScreenName); onUploadClick() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = btnColor,
                                    contentColor = if (uploadButtonColor != null) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (uploadButtonColor != null) Color.White else Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = buttonText,
                                        fontSize = 17.sp,
                                        fontFamily = FontFamily(Font(R.font.roboto_medium))
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val count = uiState.selectedIds.size
        val message = if (count == 1) {
            stringResource(R.string.delete_confirm_message_one)
        } else {
            stringResource(R.string.delete_confirm_message_many, count)
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_confirm_title),
                    fontFamily = FontFamily(Font(R.font.roboto_medium))
                )
            },
            text = {
                Text(
                    text = message,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        analytics.logButtonClick("vault_delete_selected", analyticsScreenName)
                        showDeleteConfirm = false
                        viewModel.deleteSelectedPermanently { removed ->
                            val msg = if (removed == 1) {
                                context.getString(R.string.toast_items_deleted_one)
                            } else {
                                context.getString(R.string.toast_items_deleted_many, removed)
                            }
                            context.showBrandedToast(msg, Toast.LENGTH_LONG)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete_confirm_button),
                        color = colorResource(id = R.color.wronganswercolor),
                        fontFamily = FontFamily(Font(R.font.roboto_medium))
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontFamily = FontFamily(Font(R.font.roboto_medium))
                    )
                }
            }
        )
    }

    if (showUnlockAllConfirm) {
        val total = uiState.items.size
        VaultifyThemedConfirmDialog(
            title = stringResource(R.string.unlock_all_confirm_title),
            message = stringResource(R.string.unlock_all_confirm_message, total),
            confirmButtonText = stringResource(R.string.unlock_all_confirm_button),
            dismissButtonText = stringResource(R.string.action_cancel),
            iconResId = R.drawable.unlockicon,
            iconContentDescription = stringResource(R.string.cd_unlock_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Positive,
            onDismiss = { showUnlockAllConfirm = false },
            onConfirm = {
                analytics.logButtonClick("vault_unhide_all", analyticsScreenName)
                showUnlockAllConfirm = false
                viewModel.unhideAll { restoredItems ->
                    restoredItems.forEach { info ->
                        context.showBrandedToast(info, Toast.LENGTH_LONG)
                    }
                }
            }
        )
    }

    if (showDeleteAllConfirm) {
        val total = uiState.items.size
        VaultifyThemedConfirmDialog(
            title = stringResource(R.string.delete_all_confirm_title),
            message = stringResource(R.string.delete_all_confirm_message, total),
            confirmButtonText = stringResource(R.string.delete_all_confirm_button),
            dismissButtonText = stringResource(R.string.action_cancel),
            iconResId = R.drawable.newvaultdeleicon,
            iconContentDescription = stringResource(R.string.cd_delete_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Destructive,
            onDismiss = { showDeleteAllConfirm = false },
            onConfirm = {
                analytics.logButtonClick("vault_delete_all", analyticsScreenName)
                showDeleteAllConfirm = false
                viewModel.deleteAllPermanently { removed ->
                    val msg = if (removed == 1) {
                        context.getString(R.string.toast_items_deleted_one)
                    } else {
                        context.getString(R.string.toast_items_deleted_many, removed)
                    }
                    context.showBrandedToast(msg, Toast.LENGTH_LONG)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    navController: NavHostController,
    startIndex: Int
) {
    val cdBack = stringResource(R.string.cd_back)
    val analytics = rememberVaultifyAnalytics()
    val context = LocalContext.current
    val viewerViewModel: VaultViewModel = hiltViewModel()

    // Working copy of the viewer's item list. Starts from the shared handoff and shrinks as
    // items are unlocked or deleted, so paging/back navigation stays in sync without us having
    // to reload everything from disk.
    var items by remember { mutableStateOf(ImageViewerSharedState.items) }
    val paths by remember { derivedStateOf { items.map { it.localFilePath } } }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.IMAGE_VIEWER, "ImageViewerScreen")
    }
    LaunchedEffect(paths.isEmpty()) {
        if (paths.isEmpty()) navController.popBackStack()
    }
    if (paths.isEmpty()) return

    val initialIndex = startIndex.coerceIn(0, (paths.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val currentPage = listState.firstVisibleItemIndex.coerceIn(0, (paths.size - 1).coerceAtLeast(0))
    val filePath = paths.getOrNull(currentPage) ?: paths.firstOrNull() ?: ""
    val currentEntity = items.getOrNull(currentPage)

    // Removes [entity] from both our local list and the shared handoff, keeping the underlying
    // list-screen VM in sync. If everything is gone we pop; otherwise we make sure the page
    // index stays valid after the removal.
    val removeCurrent: (VaultItemEntity) -> Unit = remember {
        { entity ->
            val updated = items.filterNot { it.id == entity.id }
            items = updated
            ImageViewerSharedState.items = updated
            if (updated.isEmpty()) {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(initialIndex)
    }

    // After an unlock/delete shrinks the list, the lazy row may still be parked on an index
    // that no longer exists. Clamp it to the new tail so the next item slides into view
    // instead of showing an empty frame.
    LaunchedEffect(items.size) {
        if (items.isNotEmpty() && listState.firstVisibleItemIndex >= items.size) {
            listState.scrollToItem(items.size - 1)
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var frameWidthPx by remember { mutableStateOf(0f) }
    var frameHeightPx by remember { mutableStateOf(0f) }
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    LaunchedEffect(filePath) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(filePath)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun clampOffsets() {
        val bmp = bitmap ?: return
        if (frameWidthPx <= 0f || frameHeightPx <= 0f) return
        val fitScale = minOf(
            frameWidthPx / bmp.width,
            frameHeightPx / bmp.height
        ).coerceAtLeast(0.001f)
        val fittedW = bmp.width * fitScale
        val fittedH = bmp.height * fitScale
        val scaledW = fittedW * scale
        val scaledH = fittedH * scale
        val maxOffsetX = (scaledW - frameWidthPx).coerceAtLeast(0f) / 2f
        val maxOffsetY = (scaledH - frameHeightPx).coerceAtLeast(0f) / 2f
        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
    }

    if (showDeleteConfirm && currentEntity != null) {
        val dialogEntity = currentEntity
        VaultifyThemedConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message_one),
            confirmButtonText = stringResource(R.string.delete_confirm_button),
            dismissButtonText = stringResource(R.string.action_cancel),
            iconResId = R.drawable.newvaultdeleicon,
            iconContentDescription = stringResource(R.string.cd_unlock_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Positive,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                analytics.logButtonClick(
                    "image_viewer_delete",
                    VaultifyAnalytics.Screen.IMAGE_VIEWER
                )
                viewerViewModel.deleteSinglePermanently(
                    entity = dialogEntity,
                    onDeleted = {
                        context.showBrandedToast(
                            context.getString(R.string.toast_items_deleted_one, 1)
                        )
                        removeCurrent(dialogEntity)
                    },
                    onError = { msg -> context.showBrandedToast(msg, Toast.LENGTH_LONG) }
                )
            }
        )
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = {
                            analytics.logButtonClick(
                                "image_viewer_back",
                                VaultifyAnalytics.Screen.IMAGE_VIEWER
                            ); navController.popBackStack()
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.backarrowbluecolor),
                                contentDescription = cdBack,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                // Unlock / Delete moved from the top bar into a prominent bottom action bar so
                // thumb reach matches the rest of the app and the icons are easier to parse than
                // the old tiny TextButtons.
                PreviewActionBar(
                    onUnlock = {
                        val entity = currentEntity ?: return@PreviewActionBar
                        analytics.logButtonClick(
                            "image_viewer_unhide",
                            VaultifyAnalytics.Screen.IMAGE_VIEWER
                        )
                        viewerViewModel.unhideSingle(
                            entity = entity,
                            onRestored = { info ->
                                context.showBrandedToast(info, Toast.LENGTH_LONG)
                                removeCurrent(entity)
                            },
                            onError = { msg -> context.showBrandedToast(msg, Toast.LENGTH_LONG) }
                        )
                    },
                    onDelete = { if (currentEntity != null) showDeleteConfirm = true },
                    enabled = currentEntity != null
                )
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val pageWidth = maxWidth
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) {
                    items(paths.size) { page ->
                        Box(
                            modifier = Modifier
                                .width(pageWidth)
                                .fillMaxHeight()
                        ) {
                            val pagePath = paths.getOrNull(page)
                            if (pagePath != null) {
                                if (page == currentPage) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .onSizeChanged {
                                                frameWidthPx = it.width.toFloat()
                                                frameHeightPx = it.height.toFloat()
                                                clampOffsets()
                                            }
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event =
                                                            awaitPointerEvent(PointerEventPass.Initial)
                                                        if (event.type == PointerEventType.Release) {
                                                            val allReleased =
                                                                event.changes.none { it.pressed }
                                                            if (allReleased) {
                                                                scale = 1f
                                                                offsetX = 0f
                                                                offsetY = 0f
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .pointerInput(
                                                scale,
                                                frameWidthPx,
                                                frameHeightPx,
                                                bitmap
                                            ) {
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                                    offsetX += pan.x
                                                    offsetY += pan.y
                                                    val bmp =
                                                        bitmap ?: return@detectTransformGestures
                                                    if (frameWidthPx <= 0f || frameHeightPx <= 0f) return@detectTransformGestures
                                                    val fitScale = minOf(
                                                        frameWidthPx / bmp.width,
                                                        frameHeightPx / bmp.height
                                                    ).coerceAtLeast(0.001f)
                                                    val fittedW = bmp.width * fitScale
                                                    val fittedH = bmp.height * fitScale
                                                    val scaledW = fittedW * scale
                                                    val scaledH = fittedH * scale
                                                    val maxOffsetX =
                                                        (scaledW - frameWidthPx).coerceAtLeast(0f) / 2f
                                                    val maxOffsetY =
                                                        (scaledH - frameHeightPx).coerceAtLeast(0f) / 2f
                                                    offsetX =
                                                        offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                                    offsetY =
                                                        offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                                }
                                            }
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap!!.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer(
                                                        scaleX = scale,
                                                        scaleY = scale,
                                                        translationX = offsetX,
                                                        translationY = offsetY
                                                    ),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(File(pagePath))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatVideoDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayingScreen(
    navController: NavHostController,
    videoPath: String
) {
    val cdBack = stringResource(R.string.cd_back)
    val cdPlay = stringResource(R.string.cd_play)
    val cdPause = stringResource(R.string.cd_pause)
    val analytics = rememberVaultifyAnalytics()
    val context = LocalContext.current
    val viewerViewModel: VaultViewModel = hiltViewModel()
    // The list screen stashes the entity here right before navigating, so we can do in-place
    // unlock / delete without an extra DAO lookup.
    var currentEntity by remember { mutableStateOf(MediaPreviewSharedState.currentItem) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.VIDEO_PLAYING, "VideoPlayingScreen")
    }
    var showPlayButton by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    val videoViewHolder = remember { object { var videoView: VideoView? = null } }
    // `uriBound` guards the one-shot setVideoURI call. AndroidView's `update` block fires on
    // every recomposition (slider drag, pause toggle, etc.); re-binding the URI each time was
    // resetting the MediaPlayer underneath and contributed to the janky pause-then-back feel.
    var uriBound by remember(videoPath) { mutableStateOf(false) }
    // `isExiting` triggers a synchronous teardown path: stop the player, hide the SurfaceView
    // with an opaque Box so the pop animation doesn't reveal a flickering surface, then pop.
    // Top-bar back, system back, unlock, and delete all funnel through this to avoid the
    // "back press but audio keeps playing for 300 ms" glitch.
    var isExiting by remember { mutableStateOf(false) }

    fun teardownPlayer() {
        val v = videoViewHolder.videoView ?: return
        try { v.stopPlayback() } catch (_: Exception) {}
    }

    fun beginExit(andThen: () -> Unit = { navController.popBackStack() }) {
        if (isExiting) return
        isExiting = true
        teardownPlayer()
        andThen()
    }

    BackHandler { beginExit() }

    DisposableEffect(Unit) {
        onDispose {
            teardownPlayer()
            videoViewHolder.videoView = null
        }
    }

    if (showDeleteConfirm && currentEntity != null) {
        val dialogEntity = currentEntity!!
        VaultifyThemedConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message_one),
            confirmButtonText = stringResource(R.string.delete_confirm_button),
            dismissButtonText = stringResource(R.string.action_cancel),
            iconResId = R.drawable.newvaultdeleicon,
            iconContentDescription = stringResource(R.string.cd_unlock_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Positive,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                    analytics.logButtonClick(
                        "video_playing_delete",
                        VaultifyAnalytics.Screen.VIDEO_PLAYING
                    )
                viewerViewModel.deleteSinglePermanently(
                    entity = dialogEntity,
                    onDeleted = {
                        context.showBrandedToast(
                            context.getString(R.string.toast_items_deleted_one, 1)
                        )
                        MediaPreviewSharedState.currentItem = null
                        currentEntity = null
                        navController.popBackStack()
                    },
                    onError = { msg -> context.showBrandedToast(msg, Toast.LENGTH_LONG) }
                )
            }
        )

    }

    LaunchedEffect(showPlayButton, isExiting) {
        // Guard: once we begin the exit transition the VideoView's MediaPlayer may be mid-
        // teardown; calling currentPosition on a stopped player has raised crashes on some
        // OEM ROMs. Bailing here also frees the coroutine so the exit animation runs on a
        // quiet main thread.
        if (!showPlayButton && !isExiting) {
            while (true) {
                val v = videoViewHolder.videoView ?: break
                val pos = try { v.currentPosition } catch (_: Exception) { break }
                val totalMs = try { v.duration } catch (_: Exception) { 0 }
                if (totalMs > 0) durationMs = totalMs.toLong()
                currentPositionMs = pos.toLong()
                delay(250)
            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = {
                            analytics.logButtonClick(
                                "video_playing_back",
                                VaultifyAnalytics.Screen.VIDEO_PLAYING
                            )
                            beginExit()
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.backarrowbluecolor),
                                contentDescription = cdBack,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                PreviewActionBar(
                    onUnlock = {
                        val entity = currentEntity ?: return@PreviewActionBar
                        analytics.logButtonClick(
                            "video_playing_unhide",
                            VaultifyAnalytics.Screen.VIDEO_PLAYING
                        )
                        // Route through beginExit so the SurfaceView is covered before the
                        // unhide coroutine races the pop animation.
                        beginExit {
                            viewerViewModel.unhideSingle(
                                entity = entity,
                                onRestored = { info ->
                                    context.showBrandedToast(info, Toast.LENGTH_LONG)
                                    MediaPreviewSharedState.currentItem = null
                                    currentEntity = null
                                    navController.popBackStack()
                                },
                                onError = { msg ->
                                    context.showBrandedToast(
                                        msg,
                                        Toast.LENGTH_LONG
                                    )
                                }
                            )
                        }
                    },
                    onDelete = { if (currentEntity != null) showDeleteConfirm = true },
                    enabled = currentEntity != null
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            videoViewHolder.videoView = this
                            setOnCompletionListener { showPlayButton = true }
                            setOnPreparedListener { durationMs = it.duration.toLong() }
                        }
                    },
                    update = { videoView ->
                        // Bind the URI exactly once. setVideoURI resets the underlying MediaPlayer
                        // state, so calling it on every recomposition (which update() is invoked
                        // on) was the source of the "pause -> back feels sluggish" glitch.
                        if (!uriBound && videoPath.isNotBlank()) {
                            try {
                                if (videoPath.startsWith("content:") || videoPath.startsWith("file:")) {
                                    videoView.setVideoURI(Uri.parse(videoPath))
                                } else {
                                    videoView.setVideoPath(videoPath)
                                }
                                uriBound = true
                            } catch (_: Exception) {
                            }
                        }
                        // Once exit starts, drop the surface visibility so the leftover frame
                        // doesn't tear during the pop animation. Compose draws the blackout Box
                        // below at full alpha as a safety net on devices that ignore INVISIBLE.
                        if (isExiting) {
                            try {
                                videoView.visibility = android.view.View.INVISIBLE
                            } catch (_: Exception) {
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isExiting) {
                    // Opaque overlay that covers the SurfaceView for the ~300 ms of exit
                    // animation. SurfaceView's separate rendering pipeline doesn't play well with
                    // Compose fade/slide transitions, so we just paint over it.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
                if (showPlayButton) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                showPlayButton = false
                                videoViewHolder.videoView?.start()
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(40.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.vallutappplayicon),
                                contentDescription = cdPlay,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                if (!showPlayButton) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Slider(
                            value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * durationMs).toInt()
                                videoViewHolder.videoView?.seekTo(seekTo)
                                currentPositionMs = seekTo.toLong()
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = colorResource(id = R.color.splashhalftextColor),
                                activeTrackColor = colorResource(id = R.color.splashhalftextColor),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatVideoDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatVideoDuration(durationMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                videoViewHolder.videoView?.pause()
                                showPlayButton = true
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(80.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(40.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.pausevideoicon),
                                contentDescription = cdPause
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayingScreen(
    navController: NavHostController,
    audioPath: String
) {
    val cdBack = stringResource(R.string.cd_back)
    val cdPlay = stringResource(R.string.cd_play)
    val cdPause = stringResource(R.string.cd_pause)
    val cdAudio = stringResource(R.string.cd_audio)
    val analytics = rememberVaultifyAnalytics()
    val viewerViewModel: VaultViewModel = hiltViewModel()
    var currentEntity by remember { mutableStateOf(MediaPreviewSharedState.currentItem) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.AUDIO_PLAYING, "AudioPlayingScreen")
    }
    var showPlayButton by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    val context = LocalContext.current
    val mediaPlayer = remember(audioPath) { MediaPlayer() }

    if (showDeleteConfirm && currentEntity != null) {
        val dialogEntity = currentEntity!!
        VaultifyThemedConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message_one),
            confirmButtonText = stringResource(R.string.delete_confirm_button),
            dismissButtonText = stringResource(R.string.action_cancel),
            iconResId = R.drawable.newvaultdeleicon,
            iconContentDescription = stringResource(R.string.cd_unlock_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Positive,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                    analytics.logButtonClick(
                        "audio_playing_delete",
                        VaultifyAnalytics.Screen.AUDIO_PLAYING
                    )
                viewerViewModel.deleteSinglePermanently(
                    entity = dialogEntity,
                    onDeleted = {
                        context.showBrandedToast(
                            context.getString(R.string.toast_items_deleted_one, 1)
                        )
                        MediaPreviewSharedState.currentItem = null
                        currentEntity = null
                        navController.popBackStack()
                    },
                    onError = { msg -> context.showBrandedToast(msg, Toast.LENGTH_LONG) }
                )
            }
        )
    }

    DisposableEffect(audioPath) {
        if (audioPath.isNotBlank()) {
            try {
                if (audioPath.startsWith("content:") || audioPath.startsWith("file:")) {
                    mediaPlayer.setDataSource(context, Uri.parse(audioPath))
                } else {
                    mediaPlayer.setDataSource(audioPath)
                }
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    durationMs = it.duration.toLong()
                }
                mediaPlayer.setOnCompletionListener {
                    showPlayButton = true
                }
            } catch (_: Exception) { }
        }
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(showPlayButton) {
        if (!showPlayButton) {
            while (true) {
                currentPositionMs = mediaPlayer.currentPosition.toLong()
                if (mediaPlayer.duration > 0) durationMs = mediaPlayer.duration.toLong()
                delay(250)
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = {
                            analytics.logButtonClick(
                                "audio_playing_back",
                                VaultifyAnalytics.Screen.AUDIO_PLAYING
                            ); navController.popBackStack()
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.backarrowbluecolor),
                                contentDescription = cdBack,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                PreviewActionBar(
                    onUnlock = {
                        val entity = currentEntity ?: return@PreviewActionBar
                        analytics.logButtonClick(
                            "audio_playing_unhide",
                            VaultifyAnalytics.Screen.AUDIO_PLAYING
                        )
                        try {
                            if (mediaPlayer.isPlaying) mediaPlayer.pause()
                        } catch (_: Exception) {
                        }
                        viewerViewModel.unhideSingle(
                            entity = entity,
                            onRestored = { info ->
                                context.showBrandedToast(info, Toast.LENGTH_LONG)
                                MediaPreviewSharedState.currentItem = null
                                currentEntity = null
                                navController.popBackStack()
                            },
                            onError = { msg -> context.showBrandedToast(msg, Toast.LENGTH_LONG) }
                        )
                    },
                    onDelete = { if (currentEntity != null) showDeleteConfirm = true },
                    enabled = currentEntity != null
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.audioicon),
                        contentDescription = cdAudio,
                        modifier = Modifier.size(120.dp)
                    )
                }
                if (showPlayButton) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                showPlayButton = false
                                try {
                                    mediaPlayer.start()
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(40.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.vallutappplayicon),
                                contentDescription = cdPlay,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                if (!showPlayButton) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Slider(
                            value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * durationMs).toInt()
                                mediaPlayer.seekTo(seekTo)
                                currentPositionMs = seekTo.toLong()
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = colorResource(id = R.color.splashhalftextColor),
                                activeTrackColor = colorResource(id = R.color.splashhalftextColor),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatVideoDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatVideoDuration(durationMs),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                try {
                                    mediaPlayer.pause()
                                } catch (_: Exception) {
                                }
                                showPlayButton = true
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(80.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(40.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.pausevideoicon),
                                contentDescription = cdPause
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun VideoThumbnail(
    filePath: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(filePath) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(filePath)
                    retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.videoicon),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colorResource(id = R.color.splashhalftextColor)
            )
        }
    }
}

private fun fileTypeIconRes(displayName: String, mimeType: String?): Int {
    val name = displayName.lowercase()
    val mime = mimeType?.lowercase() ?: ""
    return when {
        name.endsWith(".pdf") || mime == "application/pdf" -> R.drawable.pdfimage
        name.endsWith(".doc") || name.endsWith(".docx") || mime.contains("msword") || mime.contains("wordprocessingml") -> R.drawable.wordimage
        else -> R.drawable.fileicon
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun VaultItemCard(
    item: VaultItemEntity,
    type: String,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onItemClick: ((VaultItemEntity) -> Unit)? = null,
    /**
     * When true, the card renders a softly pulsing accent border to draw attention to a
     * freshly-added item (e.g. the PDF the OCR flow just saved). Independent of [isSelected];
     * highlight never implies selection.
     */
    isHighlighted: Boolean = false
) {
    val defaultFileLabel = stringResource(R.string.default_file_item_name)
    val defaultAudioLabel = stringResource(R.string.default_audio_item_name)
    val cdVideoLabel = stringResource(R.string.cd_video)
    val cdPdfDoc = stringResource(R.string.cd_pdf_document)
    val cdWordDoc = stringResource(R.string.cd_word_document)
    val cdItemSelected = stringResource(R.string.cd_item_selected)
    val cdItemNotSelected = stringResource(R.string.cd_item_not_selected)
    val file = File(item.localFilePath)
    val accentColor = colorResource(id = R.color.splashhalftextColor)
    // Gentle 0.4 → 1.0 opacity pulse for the "just saved" highlight. Runs as an infinite
    // repeatable animation; the parent screen revokes the highlight after ~2.5s so the pulse
    // naturally stops without an explicit cancellation signal here.
    val highlightAlpha = if (isHighlighted) {
        val transition = rememberInfiniteTransition(label = "vaultItemHighlight")
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vaultItemHighlightAlpha"
        ).value
    } else 0f
    val borderColor = when {
        isHighlighted -> accentColor.copy(alpha = highlightAlpha)
        isSelected -> accentColor
        else -> Color.Transparent
    }
    val borderWidth = when {
        isHighlighted -> 3.dp
        isSelected -> 2.dp
        else -> 0.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                // Long-press always starts / extends multi-select. Single tap opens the item
                // when not in selection mode, or toggles selection when we're already in it.
                when {
                    isDeleteMode -> Modifier.combinedClickable(
                        onClick = onToggleSelection,
                        onLongClick = onLongPress
                    )

                    onItemClick != null -> Modifier.combinedClickable(
                        onClick = { onItemClick(item) },
                        onLongClick = onLongPress
                    )

                    else -> Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongPress
                    )
                }
            )
            .background(colorResource(id = R.color.securitycardcolor))
    ) {
        if (type == "IMAGE" && file.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (type == "VIDEO" && file.exists()) {
            Box(modifier = Modifier.fillMaxSize()) {
                VideoThumbnail(
                    filePath = item.localFilePath,
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(id = R.drawable.vallutappplayicon),
                    contentDescription = cdVideoLabel,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                )
            }
        } else if (type == "FILE") {
            val fileIconRes = fileTypeIconRes(item.displayName, item.mimeType)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
//                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (fileIconRes) {
                        R.drawable.pdfimage -> Image(
                            painter = painterResource(id = R.drawable.pdfimage),
                            contentDescription = cdPdfDoc,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                        R.drawable.wordimage -> Image(
                            painter = painterResource(id = R.drawable.docimage),
                            contentDescription = cdWordDoc,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                        else -> Icon(
                            painter = painterResource(id = R.drawable.fileicon),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.displayName.ifBlank { defaultFileLabel },
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else if (type == "AUDIO") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
//                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.audioicon),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = colorResource(id = R.color.splashhalftextColor)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.displayName.ifBlank { defaultAudioLabel },
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = when (type) {
                        "VIDEO" -> painterResource(id = R.drawable.videoicon)
                        else -> painterResource(id = R.drawable.fileicon)
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = colorResource(id = R.color.splashhalftextColor)
                )
            }
        }
        AnimatedVisibility(
            visible = isDeleteMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                IconButton(
                    onClick = onToggleSelection,
                    modifier = Modifier.size(32.dp)
                ) {
                    Image(
                        painter = if (isSelected) painterResource(id = R.drawable.markedcheckbox) else painterResource(id = R.drawable.emptycheckbox),
                        contentDescription = if (isSelected) cdItemSelected else cdItemNotSelected
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ImagesScreen(navController: NavHostController, viewModel: VaultViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleOpenDocumentResult(context, result, viewModel, "image/*")
    }
    VaultItemScreen(
        navController = navController,
        title = stringResource(R.string.vault_title_images),
        type = "IMAGE",
        emptyMessage = stringResource(R.string.vault_empty_images),
        buttonText = stringResource(R.string.vault_add_images),
        emptyImageResId = R.drawable.imageitem,
        viewModel = viewModel,
        onUploadClick = {
            markSkipLockOnResume(context)
            imageLauncher.launch(buildOpenVisualMediaIntent("image/*"))
        },
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun VideosScreen(navController: NavHostController, viewModel: VaultViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val videoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleOpenDocumentResult(context, result, viewModel, "video/*")
    }
    VaultItemScreen(
        navController = navController,
        title = stringResource(R.string.vault_title_videos),
        type = "VIDEO",
        emptyMessage = stringResource(R.string.vault_empty_videos),
        buttonText = stringResource(R.string.vault_add_videos),
        emptyImageResId = R.drawable.videoitemimage,
        viewModel = viewModel,
        onUploadClick = {
            markSkipLockOnResume(context)
            videoLauncher.launch(buildOpenVisualMediaIntent("video/*"))
        }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun FilesScreen(navController: NavHostController, viewModel: VaultViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uris.add(it) }
                }
            }
            if (uris.isEmpty()) {
                result.data?.data?.let { uris.add(it) }
            }
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } catch (_: Exception) { }
            }
            if (uris.isNotEmpty()) {
                viewModel.addFromUris(uris, null)
            }
        }
    }
    VaultItemScreen(
        navController = navController,
        title = stringResource(R.string.vault_title_files),
        type = "FILE",
        emptyMessage = stringResource(R.string.vault_empty_files),
        buttonText = stringResource(R.string.vault_add_files),
        emptyImageResId = R.drawable.fileitemimage,
        viewModel = viewModel,
        onUploadClick = {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("skip_lock_on_resume", true).apply()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            documentLauncher.launch(intent)
        }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(navController: NavHostController, viewModel: VaultViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var showScanBottomSheet by remember { mutableStateOf(false) }
    // Pick up a "just saved from OCR" id on first composition and hand it to VaultItemScreen,
    // which owns the scroll-into-view + pulse lifecycle. Reading the id once (and clearing the
    // source) keeps the highlight a one-shot — re-entering Docs later doesn't re-flag it.
    val pendingHighlightId = remember {
        ScanFlowState.lastSavedItemId.also { ScanFlowState.lastSavedItemId = null }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uris.add(it) }
                }
            }
            if (uris.isEmpty()) {
                result.data?.data?.let { uris.add(it) }
            }
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } catch (_: Exception) { }
            }
            if (uris.isNotEmpty()) {
                viewModel.addFromUris(uris, null)
            }
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        showScanBottomSheet = false
        uri?.let {
            ScanFlowState.setImage(it)
            navController.navigate(NavRoutes.EXTRACT_IMAGE_SCREEN)
        }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        showScanBottomSheet = false
        if (success && ScanFlowState.imageUri != null) {
            navController.navigate(NavRoutes.EXTRACT_IMAGE_SCREEN)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showScanBottomSheet = false
            val file = File(context.cacheDir, "scan_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            ScanFlowState.setImage(uri)
            // The system camera can keep the user away far longer than our 30s grace window
            // (composing a shot, refocusing, reviewing). Trust the return trip: the user is
            // capturing at our request, so do NOT re-prompt the PIN when they come back — that
            // would race the TakePicture callback and land them on PIN instead of ExtractImage.
            AuthSession.markSkipLock(context)
            takePictureLauncher.launch(uri)
        } else {
            showScanBottomSheet = false
            context.showBrandedToast(context.getString(R.string.toast_camera_permission_required))
        }
    }
    if (showScanBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScanBottomSheet = false },
            containerColor = colorResource(id = R.color.bottomsheetcontainercolor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_sheet_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                0.3.dp,
                                colorResource(id = R.color.orcboxbordercolor),
                                RoundedCornerShape(14.dp)
                            )
                            .background(Color.Transparent)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }

                            ) {
                                AuthSession.markSkipLock(context)
                                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.ocrgalleryimage),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.label_gallery), color = Color.White, fontSize = 15.sp, fontFamily = FontFamily(Font(R.font.roboto_regular)))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Transparent)
                            .border(
                                0.3.dp,
                                colorResource(id = R.color.orcboxbordercolor),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        showScanBottomSheet = false
                                        val file = File(
                                            context.cacheDir,
                                            "scan_capture_${System.currentTimeMillis()}.jpg"
                                        )
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        ScanFlowState.setImage(uri)
                                        // Trust the camera round-trip (see note in
                                        // cameraPermissionLauncher above). Without this, a long
                                        // capture exceeds the 30s grace window and the PIN
                                        // screen races the TakePicture callback, so the photo
                                        // never reaches ExtractImageScreen.
                                        AuthSession.markSkipLock(context)
                                        takePictureLauncher.launch(uri)
                                    }

                                    else -> {
                                        // The runtime permission dialog ALSO pauses the
                                        // activity; skip the re-lock so the dialog → camera
                                        // chain doesn't drop us onto PIN between the two.
                                        AuthSession.markSkipLock(context)
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            }
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.ocrcameraimage),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.label_camera), color = Color.White, fontSize = 14.sp, fontFamily = FontFamily(Font(R.font.roboto_medium)))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    VaultItemScreen(
        navController = navController,
        title = stringResource(R.string.vault_title_files),
        type = "FILE",
        emptyMessage = stringResource(R.string.vault_empty_files),
        buttonText = stringResource(R.string.vault_add_files),
        emptyImageResId = R.drawable.fileitemimage,
        viewModel = viewModel,
        onUploadClick = {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("skip_lock_on_resume", true).apply()


            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                val mimeTypes = arrayOf(
                    "application/pdf",                                     // PDF
                    "application/msword",                                  // .doc
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                    "application/vnd.ms-excel",                            // .xls
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",      // .xlsx
                    "application/vnd.ms-powerpoint",                       // .ppt
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
                    "text/plain",                                          // .txt
                    "application/rtf",                                     // .rtf
//                    "application/zip"                                      // .zip
                )
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            documentLauncher.launch(intent)
        },
        onScanClick = { showScanBottomSheet = true },
        highlightItemId = pendingHighlightId
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AudiosScreen(navController: NavHostController, viewModel: VaultViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i).uri?.let { uris.add(it) }
                }
            }
            if (uris.isEmpty()) {
                result.data?.data?.let { uris.add(it) }
            }
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } catch (_: Exception) { }
            }
            if (uris.isNotEmpty()) {
                viewModel.addFromUris(uris, "audio/*")
            }
        }
    }
    VaultItemScreen(
        navController = navController,
        title = stringResource(R.string.vault_title_audio),
        type = "AUDIO",
        emptyMessage = stringResource(R.string.vault_empty_audio),
        buttonText = stringResource(R.string.vault_add_audio),
        emptyImageResId = R.drawable.audioitemiage,
        viewModel = viewModel,
        onUploadClick = {
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("skip_lock_on_resume", true).apply()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "audio/*",
                    "audio/mpeg",
                    "audio/mp3",
                    "audio/mp4",
                    "audio/x-wav",
                    "audio/wav",
                    "audio/aac",
                    "audio/ogg",
                    "audio/flac",
                    "audio/webm"
                ))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            audioLauncher.launch(intent)
        }
    )
}

/**
 * Sets the flag that suppresses the vault's lock-on-resume behavior so returning from a system
 * picker or permission prompt doesn't send the user back to the PIN screen. Thin shim over
 * [AuthSession.markSkipLock] kept for readability at the call sites.
 */
private fun markSkipLockOnResume(context: Context) = AuthSession.markSkipLock(context)

/**
 * Builds the `ACTION_OPEN_DOCUMENT` intent we use for images/videos. SAF returns document URIs
 * that translate cleanly to MediaStore URIs, which is what the "delete from device" flow needs.
 */
private fun buildOpenVisualMediaIntent(mimeType: String): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
    }

/**
 * Collects selected URIs from an ACTION_OPEN_DOCUMENT result, persists read/write grants, and
 * hands them off to the ViewModel. Mirrors the existing logic used by Files/Audio.
 */
private fun handleOpenDocumentResult(
    context: Context,
    result: androidx.activity.result.ActivityResult,
    viewModel: VaultViewModel,
    mimeType: String
) {
    if (result.resultCode != Activity.RESULT_OK) return
    val uris = mutableListOf<Uri>()
    result.data?.clipData?.let { clipData ->
        for (i in 0 until clipData.itemCount) {
            clipData.getItemAt(i).uri?.let { uris.add(it) }
        }
    }
    if (uris.isEmpty()) {
        result.data?.data?.let { uris.add(it) }
    }
    uris.forEach { uri ->
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) { }
    }
    if (uris.isNotEmpty()) {
        viewModel.addFromUris(uris, mimeType)
    }
}

/**
 * Resolves any URI produced by an upload flow (Android Photo Picker, ACTION_OPEN_DOCUMENT, a raw
 * MediaStore URI, etc.) into the real MediaStore URI required by [MediaStore.createDeleteRequest].
 *
 * The Android Photo Picker (PickVisualMedia / PickMultipleVisualMedia) returns scoped URIs shaped
 * like `content://media/picker/0/.../media/<pickerId>`. Those URIs are deliberately read-only and
 * do NOT expose the underlying `_ID` from MediaStore — passing one to `createDeleteRequest` throws
 * `IllegalArgumentException`. We recover, in order:
 *
 *   1. If it's already a MediaStore URI, use it directly.
 *   2. If it's a document URI (SAF / ACTION_OPEN_DOCUMENT), parse the `<kind>:<id>` document id.
 *   3. On API 33+ ask the platform to convert via [MediaStore.getMediaUri].
 *   4. Query the source URI for display name + size and look the item up in MediaStore ourselves
 *      (works on API 30-32 where `getMediaUri` doesn't exist).
 *   5. Last-ditch: query the URI for `_ID` and assume it matches MediaStore's id-space.
 *
 * Returns `null` when the URI has no on-device MediaStore counterpart (e.g. cloud-hosted picker
 * results). Those items genuinely can't be removed via `createDeleteRequest` and the caller should
 * tell the user honestly.
 */
private fun toMediaStoreUriWithId(context: Context, uri: Uri, type: String): Uri? {
    // 1. Already a MediaStore URI.
    val uriString = uri.toString()
    if (uriString.startsWith("content://media/external")) return uri

    // 2. Document URIs encode the id as "image:123", "video:123", etc.
    val docId = try {
        if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.getDocumentId(uri) else null
    } catch (_: Exception) { null }
    if (docId != null) {
        val parts = docId.split(":")
        if (parts.size >= 2) {
            val id = parts[1].toLongOrNull()
            if (id != null) {
                val baseUri = when (parts[0].lowercase()) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> collectionForType(type)
                }
                return ContentUris.withAppendedId(baseUri, id)
            }
        }
    }

    // 3. API 33+: official picker → MediaStore conversion.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        try {
            val converted = MediaStore.getMediaUri(context, uri)
            if (converted != null && converted.toString().startsWith("content://media/external")) {
                return converted
            }
        } catch (_: Exception) { /* fall through to the metadata-lookup path */ }
    }

    // 4. Metadata lookup: read display name + size from the picker URI and find the matching
    //    MediaStore row ourselves. Works on API 30-32 and is also a safety net on newer APIs.
    findMediaStoreByMetadata(context, uri, type)?.let { return it }

    // 5. Last-ditch: some URIs really do expose MediaStore's _ID directly.
    return queryIdAndBuildUri(context, uri, type)
}

private fun collectionForType(type: String): Uri = when (type) {
    "IMAGE" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    "AUDIO" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    else -> MediaStore.Files.getContentUri("external")
}

/**
 * Looks up a MediaStore row that matches the picker URI by display name and (when available)
 * size. This is deliberately permissive on size so we still match when the picker doesn't expose
 * it. Requires READ_MEDIA_IMAGES / READ_MEDIA_VIDEO (declared in the manifest).
 */
private fun findMediaStoreByMetadata(context: Context, uri: Uri, type: String): Uri? {
    val resolver = context.contentResolver
    val (displayName, size) = try {
        resolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                val sz = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else -1L
                name to sz
            } else null to -1L
        } ?: (null to -1L)
    } catch (_: Exception) { null to -1L }
    if (displayName.isNullOrBlank()) return null

    val collection = collectionForType(type)
    val (selection, args) = if (size > 0) {
        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?" to
            arrayOf(displayName, size.toString())
    } else {
        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?" to arrayOf(displayName)
    }
    return try {
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection, args,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                if (idx >= 0) ContentUris.withAppendedId(collection, cursor.getLong(idx)) else null
            } else null
        }
    } catch (_: Exception) { null }
}

/**
 * Last-resort: query the URI directly for `_ID` and hope it corresponds to MediaStore's id-space.
 * This is correct for legacy `content://media/external/…` URIs but incorrect for picker URIs — we
 * only reach here after every more-reliable path has failed.
 */
private fun queryIdAndBuildUri(context: Context, uri: Uri, type: String): Uri? {
    val collection = collectionForType(type)
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns._ID),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                if (idx >= 0) {
                    ContentUris.withAppendedId(collection, cursor.getLong(idx))
                } else null
            } else null
        }
    } catch (_: Exception) {
        null
    }
}
