package com.dyiz.vaultify.Screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.utils.ScanFlowState
import com.dyiz.vaultify.utils.showBrandedToast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractImageScreen(navController: NavHostController) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val scope = rememberCoroutineScope()
    val uri = remember { ScanFlowState.imageUri }
    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.EXTRACT_IMAGE, "ExtractImageScreen")
    }
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var extracting by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        if (uri == null) {
            navController.popBackStack()
            return@LaunchedEffect
        }
        // Large captures from the system camera (often 4–12 MP) take a noticeable moment to
        // decode on mid-range devices. Let the user know the app is working on it — otherwise
        // the blank preview feels like the photo was lost.
        context.showBrandedToast(context.getString(R.string.toast_ocr_preparing_image))
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri!!)?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.ocr_preview_title),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { ScanFlowState.clear(); navController.popBackStack() }) {
                            Image(
                                painter = painterResource(id = R.drawable.backarrowbluecolor),
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (bitmap != null || ScanFlowState.croppedBitmap != null) {
                                    navController.navigate(NavRoutes.CROP_IMAGE_SCREEN)
                                }
                            }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.croppedimage),
                                contentDescription = stringResource(R.string.cd_crop)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBackground)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
//                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        val displayBitmap = ScanFlowState.croppedBitmap ?: bitmap
                        if (displayBitmap != null) {
                            Image(
                                bitmap = displayBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Proper loading state while the captured photo decodes. Reusing the
                            // extracting Lottie keeps the OCR flow visually consistent end-to-end
                            // (capture → extract → save) and replaces the earlier "Loading…"
                            // text which looked like the screen was stuck.
                            val loadingComposition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(R.raw.extractinglottie)
                            )
                            val loadingProgress by animateLottieCompositionAsState(
                                composition = loadingComposition,
                                iterations = LottieConstants.IterateForever
                            )
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                LottieAnimation(
                                    composition = loadingComposition,
                                    progress = { loadingProgress },
                                    modifier = Modifier.size(160.dp)
                                )
                                Text(
                                    stringResource(R.string.toast_ocr_preparing_image),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                                )
                            }
                        }
                        if (extracting) {
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.RawRes(R.raw.extractinglottie)
                            )
                            val progress by animateLottieCompositionAsState(
                                composition = composition,
                                iterations = LottieConstants.IterateForever
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
//                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                LottieAnimation(
                                    composition = composition,
                                    progress = { progress },
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val bmp = ScanFlowState.croppedBitmap ?: bitmap
                            if (bmp == null) return@Button
                            extracting = true
                            scope.launch {
                                val text = withContext(Dispatchers.IO) {
                                    try {
                                        val recognizer =
                                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                        val image = InputImage.fromBitmap(bmp, 0)
                                        val result = suspendCancellableCoroutine<Text> { cont ->
                                            recognizer.process(image)
                                                .addOnSuccessListener { cont.resume(it) }
                                                .addOnFailureListener { cont.resumeWithException(it) }
                                        }
                                        result.text
                                    } catch (_: Exception) {
                                        ""
                                    }
                                }
                                extracting = false
                                ScanFlowState.extractedText = text
                                navController.navigate(NavRoutes.EXTRACTED_TEXT_SCREEN)
                            }
                        },
                        enabled = if (extracting) false else true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (extracting) colorResource(
                                id = R.color.bottomsheetcontainercolor
                            ) else colorResource(id = R.color.splashhalftextColor),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.extract_text_title),
                            fontSize = 17.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium)),
                            color = if (extracting) colorResource(id = R.color.disbalebtntextcolor) else Color.Black
                        )
                    }
                }
            }
        }
    }
}

private const val MIN_CROP_SIZE = 0.08f

private enum class CropHandle { NONE, MOVE, LEFT, TOP, RIGHT, BOTTOM, TL, TR, BR, BL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropImageScreen(navController: NavHostController) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.CROP_IMAGE, "CropImageScreen")
    }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropRect by remember { mutableStateOf(android.graphics.RectF(0.15f, 0.15f, 0.85f, 0.85f)) }
    var layoutW by remember { mutableStateOf(0f) }
    var layoutH by remember { mutableStateOf(0f) }
    var dragHandle by remember { mutableStateOf(CropHandle.NONE) }
    var dragStartRect by remember { mutableStateOf(android.graphics.RectF(0f, 0f, 0f, 0f)) }
    var lastNormX by remember { mutableStateOf(0.5f) }
    var lastNormY by remember { mutableStateOf(0.5f) }
    val handleSizePx = with(density) { 44.dp.toPx() }

    LaunchedEffect(Unit) {
        bitmap = withContext(Dispatchers.IO) {
            val b = ScanFlowState.croppedBitmap
            if (b != null) b
            else ScanFlowState.imageUri?.let { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) { null }
            }
        }
        if (bitmap == null) navController.popBackStack()
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            stringResource(R.string.crop_screen_title),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Image(painter = painterResource(id = R.drawable.backarrowbluecolor), contentDescription = stringResource(R.string.cd_back), modifier = Modifier.size(15.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(paddingValues)
        ) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                bitmap?.let { bmp ->
                    val scale =
                        minOf(layoutW / bmp.width, layoutH / bmp.height).coerceAtLeast(0.001f)
                    val imgW = bmp.width * scale
                    val imgH = bmp.height * scale
                    val offsetX = (layoutW - imgW) / 2f
                    val offsetY = (layoutH - imgH) / 2f

                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                layoutW = it.width.toFloat(); layoutH = it.height.toFloat()
                            }
                            .pointerInput(layoutW, layoutH, bmp.width, bmp.height) {
                                if (layoutW <= 0f || layoutH <= 0f) return@pointerInput
                                val scaleP =
                                    minOf(layoutW / bmp.width, layoutH / bmp.height).coerceAtLeast(
                                        0.001f
                                    )
                                val imgWP = bmp.width * scaleP
                                val imgHP = bmp.height * scaleP
                                val offsetXP = (layoutW - imgWP) / 2f
                                val offsetYP = (layoutH - imgHP) / 2f
                                fun toNorm(x: Float, y: Float): Pair<Float, Float> {
                                    val nx = ((x - offsetXP) / imgWP).coerceIn(0f, 1f)
                                    val ny = ((y - offsetYP) / imgHP).coerceIn(0f, 1f)
                                    return nx to ny
                                }

                                fun hitTest(x: Float, y: Float): CropHandle {
                                    val (nx, ny) = toNorm(x, y)
                                    val l = offsetXP + cropRect.left * imgWP
                                    val t = offsetYP + cropRect.top * imgHP
                                    val r = offsetXP + cropRect.right * imgWP
                                    val b = offsetYP + cropRect.bottom * imgHP
                                    val h = handleSizePx
                                    val inCrop =
                                        nx in cropRect.left..cropRect.right && ny in cropRect.top..cropRect.bottom
                                    if (x in l - h..l + h && y in t - h..t + h) return CropHandle.TL
                                    if (x in r - h..r + h && y in t - h..t + h) return CropHandle.TR
                                    if (x in r - h..r + h && y in b - h..b + h) return CropHandle.BR
                                    if (x in l - h..l + h && y in b - h..b + h) return CropHandle.BL
                                    if (x in l + h..r - h && y in t - h..t + h) return CropHandle.TOP
                                    if (x in l + h..r - h && y in b - h..b + h) return CropHandle.BOTTOM
                                    if (x in l - h..l + h && y in t + h..b - h) return CropHandle.LEFT
                                    if (x in r - h..r + h && y in t + h..b - h) return CropHandle.RIGHT
                                    if (inCrop) return CropHandle.MOVE
                                    return CropHandle.NONE
                                }
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        dragHandle = hitTest(offset.x, offset.y)
                                        dragStartRect = android.graphics.RectF(cropRect)
                                        lastNormX = (cropRect.left + cropRect.right) / 2f
                                        lastNormY = (cropRect.top + cropRect.bottom) / 2f
                                    },
                                    onDrag = { change, _ ->
                                        if (dragHandle == CropHandle.NONE) return@detectDragGestures
                                        change.consume()
                                        val (nx, ny) = toNorm(change.position.x, change.position.y)
                                        when (dragHandle) {
                                            CropHandle.MOVE -> {
                                                val dx = nx - lastNormX
                                                val dy = ny - lastNormY
                                                val w = cropRect.width()
                                                val h = cropRect.height()
                                                val newLeft =
                                                    (cropRect.left + dx).coerceIn(0f, 1f - w)
                                                val newTop =
                                                    (cropRect.top + dy).coerceIn(0f, 1f - h)
                                                cropRect = android.graphics.RectF(
                                                    newLeft,
                                                    newTop,
                                                    newLeft + w,
                                                    newTop + h
                                                )
                                                lastNormX = nx
                                                lastNormY = ny
                                            }

                                            CropHandle.LEFT -> cropRect = android.graphics.RectF(
                                                nx.coerceIn(0f, cropRect.right - MIN_CROP_SIZE),
                                                cropRect.top,
                                                cropRect.right,
                                                cropRect.bottom
                                            )

                                            CropHandle.RIGHT -> cropRect = android.graphics.RectF(
                                                cropRect.left,
                                                cropRect.top,
                                                nx.coerceIn(cropRect.left + MIN_CROP_SIZE, 1f),
                                                cropRect.bottom
                                            )

                                            CropHandle.TOP -> cropRect = android.graphics.RectF(
                                                cropRect.left,
                                                ny.coerceIn(0f, cropRect.bottom - MIN_CROP_SIZE),
                                                cropRect.right,
                                                cropRect.bottom
                                            )

                                            CropHandle.BOTTOM -> cropRect = android.graphics.RectF(
                                                cropRect.left,
                                                cropRect.top,
                                                cropRect.right,
                                                ny.coerceIn(cropRect.top + MIN_CROP_SIZE, 1f)
                                            )

                                            CropHandle.TL -> cropRect = android.graphics.RectF(
                                                nx.coerceIn(0f, cropRect.right - MIN_CROP_SIZE),
                                                ny.coerceIn(0f, cropRect.bottom - MIN_CROP_SIZE),
                                                cropRect.right, cropRect.bottom
                                            )

                                            CropHandle.TR -> cropRect = android.graphics.RectF(
                                                cropRect.left,
                                                ny.coerceIn(0f, cropRect.bottom - MIN_CROP_SIZE),
                                                nx.coerceIn(cropRect.left + MIN_CROP_SIZE, 1f),
                                                cropRect.bottom
                                            )

                                            CropHandle.BR -> cropRect = android.graphics.RectF(
                                                cropRect.left, cropRect.top,
                                                nx.coerceIn(cropRect.left + MIN_CROP_SIZE, 1f),
                                                ny.coerceIn(cropRect.top + MIN_CROP_SIZE, 1f)
                                            )

                                            CropHandle.BL -> cropRect = android.graphics.RectF(
                                                nx.coerceIn(0f, cropRect.right - MIN_CROP_SIZE),
                                                cropRect.top,
                                                cropRect.right,
                                                ny.coerceIn(cropRect.top + MIN_CROP_SIZE, 1f)
                                            )

                                            CropHandle.NONE -> {}
                                        }
                                    },
                                    onDragEnd = { dragHandle = CropHandle.NONE }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val l = offsetX + cropRect.left * imgW
                        val t = offsetY + cropRect.top * imgH
                        val r = offsetX + cropRect.right * imgW
                        val b = offsetY + cropRect.bottom * imgH
                        drawRect(
                            Color(0xFF1DB3DB),
                            topLeft = Offset(l, t),
                            size = Size(r - l, b - t),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(3f)
                        )
                        val handleSz = 12f
                        val handleColor = Color(0xFFD9D9D9)
                        drawRect(
                            handleColor,
                            topLeft = Offset(l, t),
                            size = Size(handleSz, handleSz)
                        )
                        drawRect(
                            handleColor,
                            topLeft = Offset(r - handleSz, t),
                            size = Size(handleSz, handleSz)
                        )
                        drawRect(
                            handleColor,
                            topLeft = Offset(r - handleSz, b - handleSz),
                            size = Size(handleSz, handleSz)
                        )
                        drawRect(
                            handleColor,
                            topLeft = Offset(l, b - handleSz),
                            size = Size(handleSz, handleSz)
                        )
                    }
                }
                bitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .navigationBarsPadding()
                    ) {
                        IconButton(
                            onClick = {
                                val w = bmp.width
                                val h = bmp.height
                                val left = (cropRect.left * w).toInt().coerceIn(0, w - 1)
                                val top = (cropRect.top * h).toInt().coerceIn(0, h - 1)
                                val right = (cropRect.right * w).toInt().coerceIn(left + 1, w)
                                val bottom = (cropRect.bottom * h).toInt().coerceIn(top + 1, h)
                                val cropped =
                                    Bitmap.createBitmap(bmp, left, top, right - left, bottom - top)
                                ScanFlowState.croppedBitmap = cropped
                                navController.popBackStack()
                            },
                            /*modifier = Modifier
                            .size(56.dp)
                            .background(colorResource(id = R.color.splashhalftextColor), androidx.compose.foundation.shape.CircleShape)*/
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ocrcroppedtickmarkimage),
                                contentDescription = stringResource(R.string.cd_done)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun minOf(a: Float, b: Float) = if (a < b) a else b

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractedTextScreen(
    navController: NavHostController,
    vaultViewModel: VaultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(ScanFlowState.extractedText) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    // Full-screen progress state while we generate the PDF and persist it into the Docs vault.
    // Blocks both the save dialog and the underlying screen so the user can't re-tap Save and
    // accidentally create duplicate vault rows.
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.EXTRACTED_TEXT, "ExtractedTextScreen")
        text = ScanFlowState.extractedText
        analytics.logOcrScanComplete(text?.isNotBlank() == true)
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )

    if (showSaveDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSaveDialog = false },
            containerColor = colorResource(id = R.color.bottomsheetcontainercolor),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    stringResource(R.string.dialog_export_pdf_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        placeholder = { Text(text=stringResource(R.string.placeholder_file_name), fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.roboto_regular)),color = colorResource(id = R.color.pdfsaveplaceholder).copy(alpha = 0.5f)) },
//                        modifier = Modifier.wrapContentWidth(),
                        modifier = Modifier
                            .weight(1f) // Takes remaining width
                            .height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = colorResource(id = R.color.savepdfboxbgcolor),
                            unfocusedContainerColor = colorResource(id = R.color.savepdfboxbgcolor),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White
                        )
                    )
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 60.dp)
                            .padding(4.dp)
//                            .width(80.dp)
                            .background(
                                (colorResource(id = R.color.savepdfboxbgcolor)),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text=stringResource(R.string.file_extension_pdf), color = Color.White, fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.roboto_regular)))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showSaveDialog = false },
                        modifier = Modifier
                            .widthIn(min = 100.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.savepdfboxbgcolor),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            fontSize = 17.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                            enabled = !saving,
                            onClick = {
                                analytics.logButtonClick(
                                    "extracted_text_save_pdf",
                                    VaultifyAnalytics.Screen.EXTRACTED_TEXT
                                )
                                val trimmed = fileName.trim()
                                if (trimmed.isBlank()) {
                                    context.showBrandedToast(context.getString(R.string.toast_enter_file_name))
                                    return@Button
                                }
                                // Drop the bottom sheet first so the UI transitions cleanly into
                                // the "saving" overlay without two modal layers competing.
                                showSaveDialog = false
                                saving = true
                                val pdfDisplayName = "$trimmed.pdf"
                                val content = text
                                scope.launch {
                                    context.showBrandedToast(context.getString(R.string.toast_ocr_saving_pdf))
                                    val bytes = withContext(Dispatchers.IO) {
                                        renderTextAsPdfBytes(content)
                                    }
                                    if (bytes == null) {
                                        saving = false
                                        context.showBrandedToast(context.getString(R.string.toast_ocr_pdf_save_failed))
                                        return@launch
                                    }
                                    // Persist into the Docs vault. We deliberately use a dedicated
                                    // save entrypoint (not addFromUri) so no "delete original from
                                    // device" consent dialog is triggered — we generated the
                                    // bytes ourselves, there's nothing on the device to unhide.
                                    vaultViewModel.saveInternalBytes(
                                        type = "FILE",
                                        displayName = pdfDisplayName,
                                        mimeType = "application/pdf",
                                        bytes = bytes
                                    ) { success, newId ->
                                        saving = false
                                        if (success) {
                                            // Hand the id to DocsScreen so it can scroll to /
                                            // briefly highlight the newly saved item — the user
                                            // originally complained that after save "it's not
                                            // selecting that file automatically".
                                            ScanFlowState.lastSavedItemId = newId
                                            context.showBrandedToast(
                                                context.getString(R.string.toast_ocr_pdf_saved, pdfDisplayName)
                                            )
                                            // Clean up the transient OCR state but preserve the
                                            // highlight id (ScanFlowState.clear() intentionally
                                            // leaves lastSavedItemId untouched).
                                            ScanFlowState.clear()
                                            navController.popBackStack(NavRoutes.DOCS_SCREEN, false)
                                        } else {
                                            context.showBrandedToast(
                                                context.getString(R.string.toast_ocr_pdf_save_failed)
                                            )
                                        }
                                    }
                                }
                            },
                        modifier = Modifier
                            .widthIn(min = 100.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.splashhalftextColor),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.action_save),
                            fontSize = 17.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            stringResource(R.string.extracted_text_screen_title),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { ScanFlowState.clear(); navController.popBackStack(NavRoutes.DOCS_SCREEN, false) }) {
                        Image(painter = painterResource(id = R.drawable.backarrowbluecolor), contentDescription = stringResource(R.string.cd_back), modifier = Modifier.size(15.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(paddingValues)
        ) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(
                                color = colorResource(id = R.color.securitycardcolor),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 16.sp, fontFamily = FontFamily(Font(R.font.roboto_regular))
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Transparent,
                            unfocusedTextColor = Color.Transparent,
                            cursorColor = colorResource(id = R.color.splashhalftextColor),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showSaveDialog = true }) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.savegalleryimage),
                                    contentDescription = stringResource(R.string.action_save),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    stringResource(R.string.action_save),
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                                    color = colorResource(id = R.color.white)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label_extracted), text)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                                    clip
                                )
                                context.showBrandedToast(context.getString(R.string.toast_text_copied))
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.copytextimage),
                                    contentDescription = stringResource(R.string.action_copy),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    stringResource(R.string.action_copy),
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                                    color = colorResource(id = R.color.white)
                                )
                            }
                        }
                    }
                }
            }
            // Full-screen saving overlay. Blocks interaction on both the Save bottom-sheet
            // (which is already dismissed by this point) and the text editor underneath so the
            // user can't accidentally trigger a second save while the first is in flight.
            if (saving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    val savingComposition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.extractinglottie)
                    )
                    val savingProgress by animateLottieCompositionAsState(
                        composition = savingComposition,
                        iterations = LottieConstants.IterateForever
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LottieAnimation(
                            composition = savingComposition,
                            progress = { savingProgress },
                            modifier = Modifier.size(180.dp)
                        )
                        Text(
                            stringResource(R.string.toast_ocr_saving_pdf),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders [content] into a single-page letter-size PDF (612×842 pt). Kept intentionally simple
 * — we only need a reliable export of the OCR text, and running under the system PrintManager
 * (which the old implementation used) pushed the user through an unrelated "printer" dialog
 * just to get a PDF saved. Returns the raw PDF bytes or `null` if the encoder threw.
 *
 * Overflow lines past the page are dropped to keep the save path deterministic; if long-
 * document export ever becomes a requirement, switch this to paginate across multiple pages.
 */
private fun renderTextAsPdfBytes(content: String): ByteArray? {
    return try {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val lineHeight = paint.textSize * 1.5f
        val margin = 40f
        var y = margin + paint.textSize
        for (line in content.split("\n")) {
            if (y > pageInfo.pageHeight - margin) break
            canvas.drawText(line, margin, y, paint)
            y += lineHeight
        }
        pdfDoc.finishPage(page)
        val out = java.io.ByteArrayOutputStream()
        pdfDoc.writeTo(out)
        pdfDoc.close()
        out.toByteArray()
    } catch (_: Exception) {
        null
    }
}
