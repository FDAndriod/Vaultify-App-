package com.dyiz.vaultify.Screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dyiz.vaultify.Database.NoteEntity
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.ui.VaultifyConfirmPrimaryStyle
import com.dyiz.vaultify.ui.VaultifyThemedConfirmDialog
import com.dyiz.vaultify.utils.showBrandedToast
import java.util.concurrent.TimeUnit

/**
 * Shared palette for note cards. First entry is the "default" (use app card color); the rest are
 * vibrant-but-muted tones that read well on the app's dark background and still give each note a
 * recognizable identity at a glance. We deliberately keep the palette small so the grid stays
 * visually calm even when there are many notes.
 */
internal val NOTE_COLOR_PALETTE: List<Int?> = listOf(
    null,                 // default (uses securitycardcolor)
    0xFF4F4373.toInt(),   // deep violet
    0xFF2E5D69.toInt(),   // teal
    0xFF6B4A2B.toInt(),   // ochre
    0xFF5E3F4B.toInt(),   // rose
    0xFF355240.toInt(),   // forest
    0xFF4A3F2E.toInt()    // olive-brown
)

/** Shared handle between the list and the edit screen: which note is currently being edited. */
internal object NoteEditState {
    var currentId: Long? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val uiState by viewModel.uiState.collectAsState()
    var isSearchVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember{mutableStateOf(false)}

    // Note currently pending deletion from a card's quick-delete button. Held at the list level
    // (rather than inside NoteCard) so orientation/config changes can't strand the dialog on a
    // destroyed card composition.
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.NOTES_LIST, "NotesListScreen")
    }

    // The list screen owns the "just saved" highlight lifetime — once the user leaves the
    // screen the pulse is no longer relevant.
    DisposableEffect(Unit) {
        onDispose { viewModel.clearLastSavedId() }
    }

    noteToDelete?.let { target ->
        VaultifyThemedConfirmDialog(
            title = "Delete Note?",
            message = "This action will permanently delete your note",
            confirmButtonText = "Delete",
            dismissButtonText = stringResource(R.string.action_cancel),
            iconResId = R.drawable.newvaultdeleicon,
            iconContentDescription = stringResource(R.string.cd_delete_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Destructive,
            onDismiss = { noteToDelete = null },
            onConfirm = {
                analytics.logButtonClick(
                    "notes_card_quick_delete",
                    VaultifyAnalytics.Screen.NOTES_LIST
                )
                viewModel.delete(target.id) {
                    context.showBrandedToast(context.getString(R.string.toast_note_deleted))
                }
                noteToDelete = null
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
                            text = stringResource(R.string.notes_screen_title),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            analytics.logButtonClick(
                                "notes_list_back",
                                VaultifyAnalytics.Screen.NOTES_LIST
                            )
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
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },

                    actions = {
                        if(uiState.notes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(36.dp)
                                        .background(Color(0xFF222F39), RoundedCornerShape(8.dp))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            isSearchVisible = !isSearchVisible
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete Icon with Box
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(36.dp)
                                        .background(Color(0xFF222F39), RoundedCornerShape(8.dp))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            showDeleteDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.newvaultdeleicon),
                                        contentDescription = "Delete",
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        analytics.logButtonClick(
                            "notes_list_new_fab",
                            VaultifyAnalytics.Screen.NOTES_LIST
                        )
                        NoteEditState.currentId = null
                        navController.navigate(NavRoutes.NOTE_EDIT_SCREEN)
                    },
                    containerColor = colorResource(id = R.color.splashhalftextColor),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.notes_fab_new)
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .padding(paddingValues)
            )
            {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        NotesSearchField(
                            value = uiState.query,
                            onValueChange = viewModel::setQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (uiState.notes.isEmpty()) {
                        NotesEmptyState(
                            query = uiState.query,
                            onCreate = {
                                analytics.logButtonClick(
                                    "notes_empty_create",
                                    VaultifyAnalytics.Screen.NOTES_LIST
                                )
                                NoteEditState.currentId = null
                                navController.navigate(NavRoutes.NOTE_EDIT_SCREEN)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                        )
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 96.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = uiState.notes.size,
                                key = { idx -> uiState.notes[idx].id }
                            ) { index ->
                                val note = uiState.notes[index]
                                NoteCard(
                                    note = note,
                                    isHighlighted = note.id == uiState.lastSavedId,
                                    onClick = {
                                        analytics.logButtonClick(
                                            "notes_card_open",
                                            VaultifyAnalytics.Screen.NOTES_LIST
                                        )
                                        NoteEditState.currentId = note.id
                                        navController.navigate(NavRoutes.NOTE_EDIT_SCREEN)
                                    },
                                    onDeleteClick = { noteToDelete = note }
                                )
                            }
                        }
                    }
                }

            }
            if (showDeleteDialog) {

                VaultifyThemedConfirmDialog(
                    title ="Delete All?",
                    message ="This action will permanently delete all created notes",
                    confirmButtonText ="Delete All",
                    dismissButtonText = "Cancel",
                    iconResId = R.drawable.newvaultdeleicon,
                    iconContentDescription = stringResource(R.string.cd_delete_all),
                    primaryStyle = VaultifyConfirmPrimaryStyle.Destructive,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        analytics.logButtonClick("notes_delete_all", VaultifyAnalytics.Screen.NOTES_LIST)
                        viewModel.deleteAllNotes {
                            context.showBrandedToast(context.getString(R.string.toast_all_notes_deleted))
                        }
                        showDeleteDialog = false
                    }
                )
            }

        }
    }
}

@Composable
private fun NotesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = colorResource(id = R.color.securitycardcolor)
    val placeholderColor = colorResource(id = R.color.loginplaceholdertextcolor)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(containerColor, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = placeholderColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.padding(start = 10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.notes_search_placeholder),
                    color = placeholderColor,
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                ),
                cursorBrush = SolidColor(colorResource(id = R.color.splashhalftextColor)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_clear),
                    tint = placeholderColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val defaultCardColor = colorResource(id = R.color.securitycardcolor)
    val backgroundColor = note.colorArgb?.let { Color(it) } ?: defaultCardColor
    // Auto-contrast: light backgrounds get dark text, dark backgrounds get white. Based on
    // ITU-R BT.709 luminance so our palette stays readable if we tweak colors later.
    val onColor = if (backgroundColor.luminance() > 0.55f) Color(0xFF1B1B1B) else Color.White
    val subtleOnColor = onColor.copy(alpha = 0.65f)
    val borderColor = if (isHighlighted) colorResource(id = R.color.splashhalftextColor)
    else Color.White.copy(alpha = 0.06f)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Stack the card content and the corner delete button. The button lives above the
        // content Column so the touch target stays clickable even when the card body grows;
        // we also pad the right side of the title/body so long text never slides under it.
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (note.pinned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.notepin),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text(
                            text = stringResource(R.string.notes_pinned_label),
                            color = subtleOnColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                }
                val displayTitle = note.title.ifBlank {
                    // Fall back to the first non-empty line of the body so every card has a heading.
                    note.content.lineSequence().firstOrNull { it.isNotBlank() }?.take(60).orEmpty()
                }
                if (displayTitle.isNotEmpty()) {
                    Text(
                        text = displayTitle,
                        color = onColor,
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_medium)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        // Reserve room for the corner delete button so 2 lines never sit under it.
                        modifier = Modifier.padding(end = 28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        color = onColor.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = formatRelativeTime(note.updatedAtMs),
                    color = subtleOnColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                )
            }

            // Corner quick-delete. Tint matches the destructive red we use elsewhere so the
            // affordance reads the same on the default dark card, the coloured cards, and any
            // future palette additions.
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.note_edit_delete),
                    tint = colorResource(id = R.color.error_red).copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NotesEmptyState(
    query: String,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.notediaryicon),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .alpha(0.85f)
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (query.isBlank()) {
                Text(
                    text = stringResource(R.string.notes_empty_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily(Font(R.font.roboto_medium))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.notes_empty_message),
                    color = Color(0xFFAFA9A9),
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Text(
                    text = stringResource(R.string.notes_no_results, query),
                    color = colorResource(id = R.color.splashsubtextcolor),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                )
            }
        }
    }
}

// ---------- Edit screen -----------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    navController: NavHostController,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val focusManager = LocalFocusManager.current

    // `initialEditingId` is the id we were launched with (null = new note). It feeds the one-shot
    // hydrate effect below so we never reload the note mid-session.
    // `persistedId` tracks the id currently stored in Room. It starts equal to `initialEditingId`
    // but flips from `null` to a real id as soon as the user hits the explicit save button, so
    // subsequent saves update the existing row instead of inserting duplicates.
    val initialEditingId = remember { NoteEditState.currentId }
    var persistedId by remember { mutableStateOf(initialEditingId) }
    var loaded by remember { mutableStateOf(initialEditingId == null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var colorArgb by remember { mutableStateOf<Int?>(null) }
    var pinned by remember { mutableStateOf(false) }
    var updatedAtMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var noteDeleted by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.NOTE_EDIT, "NoteEditScreen")
    }

    // Hydrate editor with the existing note (once). If the id is no longer valid (e.g. deleted
    // from another surface) we silently fall back to a blank new note.
    LaunchedEffect(initialEditingId) {
        if (initialEditingId != null) {
            val existing = viewModel.getById(initialEditingId)
            if (existing != null) {
                title = existing.title
                content = existing.content
                colorArgb = existing.colorArgb
                pinned = existing.pinned
                updatedAtMs = existing.updatedAtMs
            }
            loaded = true
        }
    }

    // Single exit path — called from back handler, top bar, and the discard dialog so we always
    // auto-save (or cleanly discard an empty new note) exactly once.
    fun finishAndExit() {
        if (noteDeleted) {
            navController.popBackStack()
            return
        }
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        val isEmpty = trimmedTitle.isEmpty() && trimmedContent.isEmpty()
        val currentId = persistedId
        when {
            isEmpty && currentId == null -> {
                // Brand-new, nothing typed — discard silently without making noise.
                navController.popBackStack()
            }
            isEmpty && currentId != null -> {
                // They cleared an existing note — treat as delete so the vault doesn't keep a
                // stub row lying around.
                viewModel.delete(currentId) {
                    context.showBrandedToast(context.getString(R.string.toast_note_deleted))
                }
                navController.popBackStack()
            }
            else -> {
                viewModel.save(
                    id = currentId,
                    title = trimmedTitle,
                    content = trimmedContent,
                    colorArgb = colorArgb,
                    pinned = pinned
                )
                navController.popBackStack()
            }
        }
    }

    // Explicit "save in place" — the toolbar save button. Commits the current content and shows
    // a confirmation toast without leaving the screen, so the user can keep editing a long diary
    // entry across multiple checkpoints. Re-saving a brand-new note reuses the id returned from
    // the first save so we never insert duplicates.
    fun saveInPlace() {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) {
            // Nothing worth persisting yet — match the "empty note" behaviour from
            // finishAndExit but stay on screen so the user isn't surprised.
            context.showBrandedToast(context.getString(R.string.toast_note_discarded))
            return
        }
        analytics.logButtonClick("note_edit_save", VaultifyAnalytics.Screen.NOTE_EDIT)
        focusManager.clearFocus(force = false)
        viewModel.save(
            id = persistedId,
            title = trimmedTitle,
            content = trimmedContent,
            colorArgb = colorArgb,
            pinned = pinned,
            onSaved = { savedId ->
                persistedId = savedId
                updatedAtMs = System.currentTimeMillis()
                context.showBrandedToast(context.getString(R.string.toast_note_saved))
            }
        )
    }

    BackHandler(enabled = loaded) {
        focusManager.clearFocus(force = true)
        finishAndExit()
    }

    if (showDeleteConfirm && persistedId != null) {
        val idToDelete = persistedId!!
        VaultifyThemedConfirmDialog(
            title = "Delete Note?",
            message = "This action will permanently delete your note",
            confirmButtonText ="Delete",
            dismissButtonText = "Cancel",
            iconResId = R.drawable.newvaultdeleicon,
            iconContentDescription = stringResource(R.string.cd_delete_all),
            primaryStyle = VaultifyConfirmPrimaryStyle.Destructive,
            onDismiss = { showDeleteConfirm = false},
            onConfirm = {
                showDeleteConfirm = false
                analytics.logButtonClick(
                    "note_edit_delete",
                    VaultifyAnalytics.Screen.NOTE_EDIT
                )
                viewModel.delete(idToDelete) {
                    context.showBrandedToast(context.getString(R.string.toast_note_deleted))
                }
                noteDeleted = true
                navController.popBackStack()
            }
        )
    }

    val defaultCardColor = colorResource(id = R.color.securitycardcolor)
    val backgroundColor = colorArgb?.let { Color(it) } ?: defaultCardColor
    val onColor = if (backgroundColor.luminance() > 0.55f) Color(0xFF1B1B1B) else Color.White
    val placeholderColor = onColor.copy(alpha = 0.5f)
    val dividerColor = onColor.copy(alpha = 0.12f)
    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                if (persistedId == null) R.string.note_edit_new_title
                                else R.string.note_edit_edit_title
                            ),
                            color = onColor,
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus(force = true)
                            analytics.logButtonClick(
                                "note_edit_back",
                                VaultifyAnalytics.Screen.NOTE_EDIT
                            )
                            finishAndExit()
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.backarrowbluecolor),
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
//                                .size(36.dp)
//                                .background(Color(0xFF222F39), RoundedCornerShape(8.dp))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    pinned = !pinned
                                    analytics.logButtonClick(
                                        "note_edit_toggle_pin",
                                        VaultifyAnalytics.Screen.NOTE_EDIT
                                    )
                                    context.showBrandedToast(
                                        context.getString(
                                            if (pinned) R.string.toast_note_pinned
                                            else R.string.toast_note_unpinned
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.notepin),
                                contentDescription = stringResource(R.string.note_edit_toggle_pin),
                                colorFilter = ColorFilter.tint(
                                    if (pinned) colorResource(id = R.color.splashhalftextColor)
                                    else onColor.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        val hasContent = title.isNotBlank() || content.isNotBlank()
                        IconButton(onClick = { saveInPlace() }) {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = stringResource(R.string.action_save),
                                tint = if (hasContent) colorResource(id = R.color.splashhalftextColor)
                                else onColor.copy(alpha = 0.4f)
                            )
                        }
                        if (persistedId != null) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.note_edit_delete),
                                    tint = onColor
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(dividerColor)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ColorPickerRow(
                        selected = colorArgb,
                        onSelected = { colorArgb = it },
                        onColor = onColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.note_edit_updated_at,
                                formatRelativeTime(updatedAtMs)
                            ),
                            color = onColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_regular))
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.note_edit_char_count, content.length),
                            color = onColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_regular))
                        )
                    }
                }
            }
        ) { paddingValues ->
            AnimatedVisibility(
                visible = loaded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = false,
                        textStyle = TextStyle(
                            color = onColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        ),
                        cursorBrush = SolidColor(colorResource(id = R.color.splashhalftextColor)),
                        decorationBox = { inner ->
                            if (title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_edit_title_placeholder),
                                    color = placeholderColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily(Font(R.font.roboto_medium))
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        textStyle = TextStyle(
                            color = onColor,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_regular))
                        ),
                        cursorBrush = SolidColor(colorResource(id = R.color.splashhalftextColor)),
                        decorationBox = { inner ->
                            if (content.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_edit_body_placeholder),
                                    color = placeholderColor,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

        }
    }
}

@Composable
private fun ColorPickerRow(
    selected: Int?,
    onSelected: (Int?) -> Unit,
    onColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.note_edit_color_label),
            color = onColor.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(R.font.roboto_medium))
        )
        Spacer(modifier = Modifier.widthIn(min = 6.dp))
        NOTE_COLOR_PALETTE.forEach { argb ->
            val swatchColor = argb?.let { Color(it) }
                ?: Color(0xFF222F39)
            val isSelected = selected == argb
            Box(
                modifier = Modifier
                    .size(if (isSelected) 28.dp else 24.dp)
                    .background(swatchColor, CircleShape)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) onColor else onColor.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable { onSelected(argb) }
            )
        }
    }
}

// ---------- helpers ---------------------------------------------------------------------

/**
 * Human-friendly "updated X ago" string in English (the app's sole supported language as of
 * writing). Falls back to absolute day count past a week so the cards don't show ballooning
 * "720 h ago" values.
 */
@Composable
private fun formatRelativeTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = (now - timestampMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> stringResource(R.string.notes_updated_just_now)
        minutes < 60 -> stringResource(R.string.notes_updated_minutes, minutes.toInt())
        hours < 24 -> stringResource(R.string.notes_updated_hours, hours.toInt())
        else -> stringResource(R.string.notes_updated_days, days.toInt())
    }
}

@Composable
fun PlanActionDialog(
    title: String,
    subText: String,
    iconRes: Int,
    confirmButtonText: String,
    cancelButtonText:String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss,properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()//
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF384C5A)),
            border = BorderStroke(1.2.dp, Color(0xFF1DB3DB))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFF644D5B),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.2.dp, Color(0xFFFF5E69), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtext
                Text(
                    text = subText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.2.dp,Color(0xFF375464))
                    ) {
                        Text(
                            text = cancelButtonText,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                    // Confirm (Repeat/Delete) Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(
                                color = Color(0xFFFF535F),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmButtonText,
                            color = Color.White,
                            fontFamily = FontFamily(Font(R.font.roboto_medium)),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
