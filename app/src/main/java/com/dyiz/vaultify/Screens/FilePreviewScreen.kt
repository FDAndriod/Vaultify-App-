package com.dyiz.vaultify.Screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dyiz.vaultify.Database.VaultItemEntity
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.auth.AuthSession
import com.dyiz.vaultify.ui.VaultifyConfirmPrimaryStyle
import com.dyiz.vaultify.ui.VaultifyThemedConfirmDialog
import com.dyiz.vaultify.utils.showBrandedToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.Color as AndroidColor

/**
 * In-app preview for vault files (Docs / Files slice). Chooses a renderer based on the file's
 * extension / mime type:
 *
 *  - **PDF**  → native [PdfRenderer] with on-demand page rendering so a 500-page PDF doesn't
 *               OOM the process. Pages are serialised through a [Mutex] because PdfRenderer is
 *               explicitly not thread-safe.
 *  - **Text** → UTF-8 decoded (first 512 KB only) into a scrollable monospace view. Good enough
 *               for common document / config / code-style files without pulling in a full text
 *               engine.
 *  - **Image**→ Coil [AsyncImage], same component used by the images vault.
 *  - **Other**→ Metadata card (name / size / mime) plus an "Open with another app" fallback
 *               that copies the file into [Context.cacheDir] and fires an `ACTION_VIEW` intent
 *               through the existing FileProvider. The vault's real on-disk path is never
 *               shared — we only ever hand out a cache-path URI.
 *
 * The Unlock / Delete action bar matches the video & audio viewers so the gesture is consistent
 * across every preview surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(navController: NavHostController) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val viewerViewModel: VaultViewModel = hiltViewModel()
    var currentEntity by remember { mutableStateOf(FilePreviewSharedState.currentItem) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.FILE_PREVIEW, "FilePreviewScreen")
    }

    // Guard against somehow landing on this screen with no entity (process death, stale state,
    // etc). Pop back instead of rendering an empty shell that the user can't do anything with.
    LaunchedEffect(currentEntity) {
        if (currentEntity == null) navController.popBackStack()
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
                        "file_preview_delete",
                        VaultifyAnalytics.Screen.FILE_PREVIEW
                    )
                viewerViewModel.deleteSinglePermanently(
                    entity = dialogEntity,
                    onDeleted = {
                        context.showBrandedToast(
                            context.getString(R.string.toast_items_deleted_one, 1)
                        )
                        FilePreviewSharedState.currentItem = null
                        currentEntity = null
                        navController.popBackStack()
                    },
                    onError = { msg -> context.showBrandedToast(msg, Toast.LENGTH_LONG) }
                )
            }
        )
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentEntity?.displayName
                                ?: stringResource(R.string.file_preview_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            analytics.logButtonClick(
                                "file_preview_back",
                                VaultifyAnalytics.Screen.FILE_PREVIEW
                            )
                            navController.popBackStack()
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.backarrowbluecolor),
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                PreviewActionBar(
                    onUnlock = {
                        val entity = currentEntity ?: return@PreviewActionBar
                        analytics.logButtonClick(
                            "file_preview_unhide",
                            VaultifyAnalytics.Screen.FILE_PREVIEW
                        )
                        viewerViewModel.unhideSingle(
                            entity = entity,
                            onRestored = { info ->
                                context.showBrandedToast(info, Toast.LENGTH_LONG)
                                FilePreviewSharedState.currentItem = null
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
                    .background(gradient)
                    .padding(paddingValues)
            ) {
                val entity = currentEntity ?: return@Box
                val file = File(entity.localFilePath)
                val kind =
                    remember(entity.id) { detectPreviewKind(entity.displayName, entity.mimeType) }
                when (kind) {
                    PreviewKind.PDF -> PdfPreview(file = file)
                    PreviewKind.TEXT -> TextPreview(file = file)
                    PreviewKind.IMAGE -> ImagePreview(file = file)
                    PreviewKind.UNSUPPORTED -> UnsupportedPreview(
                        entity = entity,
                        file = file,
                        analytics = analytics
                    )
                }
            }
        }
    }
}

// ---------- kind detection ----------------------------------------------------------------

private enum class PreviewKind { PDF, TEXT, IMAGE, UNSUPPORTED }

/** Extensions we try to decode as UTF-8 text. Kept inclusive but not absurd — binary files
 *  (e.g. `.docx`) decode to garbage and belong in [PreviewKind.UNSUPPORTED] regardless of how
 *  tempting it is to render them. */
private val TEXT_EXTENSIONS: Set<String> = setOf(
    "txt", "md", "markdown", "log", "json", "xml", "html", "htm", "csv", "tsv",
    "yml", "yaml", "ini", "properties", "conf", "cfg", "sh", "bash", "zsh",
    "kt", "kts", "java", "py", "rb", "rs", "go", "js", "jsx", "ts", "tsx",
    "css", "scss", "less", "c", "cpp", "cc", "h", "hpp", "m", "mm", "swift",
    "gradle", "dart", "sql", "env"
)

private val IMAGE_EXTENSIONS: Set<String> = setOf(
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif"
)

private fun detectPreviewKind(displayName: String, mimeType: String?): PreviewKind {
    val ext = displayName.substringAfterLast('.', "").lowercase()
    val mime = mimeType?.lowercase().orEmpty()
    return when {
        ext == "pdf" || mime == "application/pdf" -> PreviewKind.PDF
        ext in IMAGE_EXTENSIONS || mime.startsWith("image/") -> PreviewKind.IMAGE
        ext in TEXT_EXTENSIONS || mime.startsWith("text/") -> PreviewKind.TEXT
        else -> PreviewKind.UNSUPPORTED
    }
}

// ---------- PDF preview -------------------------------------------------------------------

/**
 * Structural holder for an open PDF. Bundles the native [PdfRenderer], its [ParcelFileDescriptor],
 * and the [Mutex] that serialises page open/render cycles ([PdfRenderer.openPage] requires that
 * at most one page is open at a time — two composables entering at the same scroll offset
 * would otherwise race).
 */
private class PdfPreviewHandle(
    val renderer: PdfRenderer,
    val pfd: ParcelFileDescriptor,
    val mutex: Mutex
)

/**
 * On-demand PDF renderer. Opens the file once, exposes the page count to a [LazyColumn], and
 * renders each page into a [Bitmap] only when the row enters composition — scrolling past
 * already-rendered pages costs nothing extra.
 */
@Composable
private fun PdfPreview(file: File) {
    var handle by remember(file.absolutePath) { mutableStateOf<PdfPreviewHandle?>(null) }
    var pageCount by remember(file.absolutePath) { mutableStateOf(0) }
    var loadError by remember(file.absolutePath) { mutableStateOf(false) }



    LaunchedEffect(file.absolutePath) {
        loadError = false
        handle = null
        val opened = withContext(Dispatchers.IO) {
            try {
                if (!file.exists() || !file.canRead()) return@withContext null

                // Explicitly opening with READ_ONLY
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                PdfPreviewHandle(renderer, pfd, Mutex())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        if (opened == null) {
            loadError = true
        } else {
            pageCount = opened.renderer.pageCount
            handle = opened
        }
    }

    if (file.length() == 0L) {
        CenteredMessage("File is empty or corrupted")
        return
    }

    if (loadError) {
        CenteredMessage(stringResource(R.string.file_preview_pdf_error))
        return
    }

    val current = handle
    if (current == null || pageCount == 0) {
        LoadingPlaceholder()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count = pageCount) { index ->
            PdfPage(handle = current, pageIndex = index, pageCount = pageCount)
        }
    }
}
@Composable
private fun PdfPage(
    handle: PdfPreviewHandle,
    pageIndex: Int,
    pageCount: Int
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var aspect by remember(pageIndex) { mutableStateOf(0.707f) }

    LaunchedEffect(pageIndex) {
        val resultBmp = withContext(Dispatchers.IO) {
            handle.mutex.withLock {
                try {
                    val page = handle.renderer.openPage(pageIndex)
                    val pw = page.width
                    val ph = page.height

                    // Aspect ratio calculation
                    aspect = if (ph > 0) pw.toFloat() / ph.toFloat() else 0.707f

                    val targetWidth = 1000
                    val scale = targetWidth.toFloat() / pw.toFloat()
                    val outW = (pw * scale).toInt().coerceAtLeast(1)
                    val outH = (ph * scale).toInt().coerceAtLeast(1)

                    val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)

                    // Render page onto bitmap
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }


        withContext(Dispatchers.Main) {
            bitmap = resultBmp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(aspect)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Loader tab tak jab tak bitmap null hai
                CircularProgressIndicator(
                    color = colorResource(id = R.color.splashhalftextColor),
                    modifier = Modifier.size(30.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.file_preview_page_label, pageIndex + 1, pageCount),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
    }
}
// ---------- Text preview ------------------------------------------------------------------

private const val TEXT_PREVIEW_MAX_BYTES: Int = 512 * 1024

/** Decoded UTF-8 snapshot of a file plus a flag for whether we truncated past the byte cap. */
private data class TextPreviewResult(val text: String, val truncated: Boolean)

@Composable
private fun TextPreview(file: File) {
    var content by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var truncated by remember(file.absolutePath) { mutableStateOf(false) }
    var error by remember(file.absolutePath) { mutableStateOf(false) }

    LaunchedEffect(file.absolutePath) {
        val decoded = withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) return@withContext null
                file.inputStream().use { stream ->
                    val limit = TEXT_PREVIEW_MAX_BYTES
                    val buf = ByteArray(limit)
                    var read = 0
                    while (read < limit) {
                        val n = stream.read(buf, read, limit - read)
                        if (n <= 0) break
                        read += n
                    }
                    // Probe for one extra byte to know whether we truncated. Cheap — a single
                    // read() that the caller may not care about, but it lets us show an honest
                    // "showing first N KB" hint instead of silently cutting mid-page.
                    val more = stream.read() != -1
                    TextPreviewResult(
                        text = String(buf, 0, read, Charsets.UTF_8),
                        truncated = more
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
        if (decoded == null) {
            error = true
        } else {
            content = decoded.text
            truncated = decoded.truncated
        }
    }

    when {
        error -> CenteredMessage(stringResource(R.string.file_preview_text_error))
        content == null -> LoadingPlaceholder()
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (truncated) {
                    Text(
                        text = stringResource(
                            R.string.file_preview_text_truncated,
                            TEXT_PREVIEW_MAX_BYTES / 1024
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular))
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = colorResource(id = R.color.securitycardcolor),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = content!!,
                        color = Color.White,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

// ---------- Image preview -----------------------------------------------------------------

@Composable
private fun ImagePreview(file: File) {
    val context = LocalContext.current
    if (!file.exists()) {
        CenteredMessage(stringResource(R.string.file_preview_text_error))
        return
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

// ---------- Unsupported fallback ----------------------------------------------------------

@Composable
private fun UnsupportedPreview(
    entity: VaultItemEntity,
    file: File,
    analytics: VaultifyAnalytics
) {
    val context = LocalContext.current
    val fileIcon = when {
        entity.displayName.lowercase().endsWith(".doc") ||
            entity.displayName.lowercase().endsWith(".docx") -> R.drawable.docimage
        entity.displayName.lowercase().endsWith(".pdf") -> R.drawable.pdfimage
        else -> R.drawable.fileicon
    }
    val sizeLabel = remember(file.absolutePath) {
        if (file.exists()) Formatter.formatShortFileSize(context, file.length()) else "—"
    }
    val mimeLabel = entity.mimeType
        ?: MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(entity.displayName.substringAfterLast('.', "").lowercase())
        ?: "application/octet-stream"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = fileIcon),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = entity.displayName,
            color = Color.White,
            fontSize = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily(Font(R.font.roboto_medium))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.file_preview_file_info_size, sizeLabel),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
        Text(
            text = stringResource(R.string.file_preview_file_info_type, mimeLabel),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.file_preview_unsupported_title),
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = FontFamily(Font(R.font.roboto_medium))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.file_preview_unsupported_message),
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                analytics.logButtonClick(
                    "file_preview_open_external",
                    VaultifyAnalytics.Screen.FILE_PREVIEW
                )
                openExternally(context, entity, file, mimeLabel)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.splashhalftextColor),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = stringResource(R.string.file_preview_open_external),
                fontSize = 15.sp,
                fontFamily = FontFamily(Font(R.font.roboto_medium))
            )
        }
    }
}

/**
 * Hands the vault file off to whichever external app the user picks. We copy to [Context.cacheDir]
 * first (mapped by the existing `file_paths.xml` FileProvider) so the actual vault directory is
 * never exposed — the URI the receiver sees lives under `cache/` and is removed next time the
 * OS reclaims cache storage. Call site is already gated by [AuthSession.markSkipLock] so the
 * round-trip doesn't re-prompt the PIN when the external app returns focus.
 */
private fun openExternally(
    context: android.content.Context,
    entity: VaultItemEntity,
    source: File,
    mimeType: String
) {
    try {
        if (!source.exists()) {
            context.showBrandedToast(context.getString(R.string.file_preview_text_error))
            return
        }
        val exportDir = File(context.cacheDir, "preview_exports").apply { mkdirs() }
        // Unique filename keeps stale previews from colliding with the fresh one; keeping the
        // original extension is what lets the receiving app pick the right handler.
        val safeName = entity.displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val exported = File(exportDir, "${System.currentTimeMillis()}_$safeName")
        source.inputStream().use { input ->
            exported.outputStream().use { output -> input.copyTo(output) }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exported
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(
            viewIntent,
            context.getString(R.string.file_preview_open_external)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        // External apps pause our Activity long past the auth grace window; skip the re-lock
        // so the user comes back to the preview (not to the PIN screen).
        AuthSession.markSkipLock(context)
        context.showBrandedToast(context.getString(R.string.file_preview_opening_external))
        context.startActivity(chooser)
    } catch (_: android.content.ActivityNotFoundException) {
        context.showBrandedToast(context.getString(R.string.file_preview_no_external_app))
    } catch (_: Exception) {
        context.showBrandedToast(context.getString(R.string.file_preview_text_error))
    }
}

// ---------- tiny UI helpers ---------------------------------------------------------------

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colorResource(id = R.color.splashhalftextColor),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.file_preview_loading),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.roboto_regular))
            )
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
    }
}
