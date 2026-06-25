package com.dyiz.vaultify.Screens.PinCreation

import android.content.Context
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.OnboardingConstants
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.auth.AuthSession
import com.dyiz.vaultify.utils.showBrandedToast
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun PinCreationScreen(
    navController: NavHostController,
    isEnterMode: Boolean = false,
    isResetMode: Boolean = false,
    viewModel: PinCreationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analytics = rememberVaultifyAnalytics()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val screenName = when {
            isResetMode -> VaultifyAnalytics.Screen.RESET_PIN
            isEnterMode -> VaultifyAnalytics.Screen.PIN_ENTER
            else -> VaultifyAnalytics.Screen.PIN_CREATION
        }
        analytics.logScreenView(screenName, "PinCreationScreen")
    }
    var fingerprintEnabledByUser by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        fingerprintEnabledByUser = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("fingerprint_enabled", false)
    }
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )

    val config = LocalConfiguration.current
    val keypadSpacing = (config.screenWidthDp * 0.04f).dp
    val forgetPinPadding = (config.screenWidthDp * 0.025f).dp.coerceIn(8.dp, 24.dp)
    val scrollState = rememberScrollState()

    val screenHeight = config.screenHeightDp
    val topSpacer = (screenHeight * 0.08f).dp // Dynamic top margin
    val midSpacer = (screenHeight * 0.05f).dp // Dynamic middle margin
    val buttonBottomMargin = OnboardingConstants.BUTTON_BOTTOM_MARGIN
    val buttonAreaHeight = 56.dp + buttonBottomMargin

    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = buttonAreaHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topSpacer))
                Text(
                    text = when {
                        isResetMode -> stringResource(id = R.string.reset_pin)
                        isEnterMode -> stringResource(id = R.string.enter_pin)
                        else -> stringResource(id = R.string.create_your_pin)
                    },
                    fontSize = 24.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    color = colorResource(id = R.color.white),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when{
                        isResetMode -> stringResource(id = R.string.reset_pinsubtitle)
                        isEnterMode->stringResource(id = R.string.pin_subtitle)
                        else->stringResource(id = R.string.createpin_subtitle)
                    },
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = colorResource(id = R.color.pinscreesubtextcolor),
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
//                Spacer(modifier = Modifier.height(40.dp))
                Spacer(modifier = Modifier.height(midSpacer))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val boxSize = 56.dp
                    val boxSpacing = 12.dp
                    val totalPinRowWidth = (boxSize * 4) + (boxSpacing * 6)
                    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
                        initialValue = 1f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(530),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "cursorAlpha"
                    )
                    AnimatedVisibility(
                        visible = uiState.errorMessage != null,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it }
                    ) {
                        uiState.errorMessage?.let { message ->
                            Text(
                                text = message,
                                fontSize = 13.sp,
                                color = colorResource(id = R.color.wronganswercolor),
                                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Pin box
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (0 until 4).forEach { index ->
                            val digit = uiState.pinDigits.getOrNull(index)
                            val isFocused = index == uiState.pinDigits.length
                            val hasWrongPinError = isEnterMode && uiState.errorMessage != null
                            val borderColor = when {
                                hasWrongPinError -> colorResource(id = R.color.wronganswercolor)
                                isFocused -> colorResource(id = R.color.white)
                                else -> colorResource(id = R.color.splashscreenColor2)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        colorResource(id = R.color.splashscreenColor2).copy(
                                            alpha = 0.8f
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (digit != null) {
                                    if (uiState.isPinVisible) {
                                        Text(
                                            text = digit.toString(),
                                            fontSize = 24.sp,
                                            color = colorResource(id = R.color.white),
                                            fontFamily = FontFamily(Font(R.font.roboto_bold)),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(Alignment.CenterVertically)
                                                .align(Alignment.Center)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.asterickimage),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                                if (isFocused) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(x = if (digit != null) 14.dp else 0.dp)
                                            .width(3.dp)
                                            .height(26.dp)
                                            .alpha(cursorAlpha)
                                            .background(colorResource(id = R.color.white))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    //------ Hide pin row

                    Row(
                        modifier = Modifier
                            .width(totalPinRowWidth)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (uiState.isPinVisible) stringResource(id = R.string.hide_pin) else stringResource(
                                id = R.string.view_pin
                            ),
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.canvapointercolor),
                            fontFamily = FontFamily(Font(R.font.roboto_regular)),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.togglePinVisibility() }
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(midSpacer))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(keypadSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf('1', '2', '3').forEach { key ->
                            KeypadButton(key = key.toString()) { viewModel.addDigit(key) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf('4', '5', '6').forEach { key ->
                            KeypadButton(key = key.toString()) { viewModel.addDigit(key) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf('7', '8', '9').forEach { key ->
                            KeypadButton(key = key.toString()) { viewModel.addDigit(key) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .then(
                                    if (isEnterMode && fingerprintEnabledByUser)
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { navController.navigate(NavRoutes.FINGERPRINT_SETUP_SCREEN) }
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEnterMode && fingerprintEnabledByUser) {
                                Image(
                                    painter = painterResource(id = R.drawable.fingerprint),
                                    contentDescription = stringResource(R.string.cd_fingerprint)
                                )
                            }
                        }
                        KeypadButton(key = "0") { viewModel.addDigit('0') }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.removeDigit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.crosspin),
                                contentDescription = stringResource(R.string.cd_delete_digit),
//                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                if (isEnterMode) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.forget_your_pin),
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.pinscreesubtextcolor),
                            fontFamily = FontFamily(Font(R.font.roboto_regular)),
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    navController.navigate(NavRoutes.SECURITY_QUESTION_RECOVERY)
                                }
                                .padding(forgetPinPadding)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = uiState.pinDigits.length == 4,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = buttonBottomMargin)
            ) {
                Button(
                    onClick = {
                            viewModel.continueClicked(
                                isEnterMode = isEnterMode,
                                onSuccess = {
                                    // Starting a fresh authenticated session — wipe any stale
                                    // lock arms so a pause that happened before unlocking can't
                                    // re-fire a redundant PIN prompt right after we navigate.
                                    AuthSession.prefs(context).edit()
                                        .putBoolean(AuthSession.KEY_SHOULD_LOCK_ON_RESUME, false)
                                        .putBoolean(AuthSession.KEY_SKIP_LOCK_ON_RESUME, false)
                                        .remove(AuthSession.KEY_PAUSED_AT_MS)
                                        .apply()
                                    when {
                                        isEnterMode -> {
                                            navController.navigate(NavRoutes.HOME) {
                                                popUpTo(NavRoutes.PIN_ENTER_SCREEN) {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                        isResetMode -> {
                                            navController.navigate(NavRoutes.HOME) {
                                                popUpTo(NavRoutes.RESET_PIN_SCREEN) {
                                                    inclusive = true
                                                }
                                            }
                                            context.showBrandedToast(context.getString(R.string.toast_pin_reset_success))
                                        }
                                        else -> {
                                            context.getSharedPreferences(
                                                "app_prefs",
                                                0
                                            )
                                                .edit()
                                                .putBoolean(
                                                    "has_reached_security_question",
                                                    true
                                                )
                                                .apply()
                                            navController.navigate("${NavRoutes.SECURITY_QUESTION_SCREEN}?isEdit=false") {
                                                popUpTo(NavRoutes.PIN_CREATION_SCREEN) {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                    }
                                },
                                onError = { message -> viewModel.setError(message) }
                            )
                        },
                        modifier = Modifier
                            .width(314.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.splashhalftextColor),
                            contentColor = colorResource(id = R.color.getstartedtextcolor)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isResetMode) stringResource(id = R.string.pin_update_button) else stringResource(id = R.string.continue_text),
                            fontSize = 17.sp,
                            color = colorResource(id = R.color.getstartedtextcolor),
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key,
            fontSize = 32.sp,
            color = colorResource(id = R.color.white),
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
    }
}
