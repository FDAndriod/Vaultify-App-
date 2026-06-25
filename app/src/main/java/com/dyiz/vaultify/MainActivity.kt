package com.dyiz.vaultify

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dyiz.vaultify.ads.AdsConsent
import com.dyiz.vaultify.ads.AdsEntryPoint
import com.dyiz.vaultify.ads.InterstitialNavPolicy
import com.dyiz.vaultify.auth.AuthSession
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

//ComponentActivity()
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute = intent?.getStringExtra(EXTRA_TARGET_ROUTE)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            MainScreen(
                targetRoute = pendingRoute,
                onRouteConsumed = {pendingRoute = null}
            )
        }
        // UMP + Mobile Ads must not block the first frame (otherwise a long white blank window during consent/network).
        window.decorView.post {
            AdsConsent.prepareAndInitializeMobileAds(this)
        }
    }
    companion object {
        const val EXTRA_TARGET_ROUTE = "extra_target_route"
    }
}

@Composable
fun MainScreen(
    targetRoute: String? = null,
    onRouteConsumed:()->Unit={}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val navController = rememberNavController()//step 1
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showExitDialog by remember { mutableStateOf(false) }
    var hasReachedSecurityQuestion by remember { mutableStateOf(false) }
    var pendingLockOnResume by remember { mutableStateOf(false) }


    val prefs = (context as? Activity)?.getSharedPreferences("app_prefs", 0)

    val isImageViewer = currentRoute?.startsWith(NavRoutes.IMAGE_VIEWER_SCREEN) == true
    val isVideoViewer = currentRoute?.startsWith(NavRoutes.VIDEO_PLAYING_SCREEN) == true
    val isAudioPlayer = currentRoute?.startsWith(NavRoutes.AUDIO_PLAYING_SCREEN) == true
    val isSecurityQuestionScreen = currentRoute?.startsWith(NavRoutes.SECURITY_QUESTION_SCREEN) == true
    val isRecentPinScreen = currentRoute?.startsWith(NavRoutes.RESET_PIN_SCREEN) == true
    val isPinEnterScreen = currentRoute?.startsWith(NavRoutes.PIN_ENTER_SCREEN) == true
    val isPinCreationScreen = currentRoute?.startsWith(NavRoutes.PIN_CREATION_SCREEN) == true
    val isSecurityQuestionRecovery = currentRoute?.startsWith(NavRoutes.SECURITY_QUESTION_RECOVERY) == true
    val isFingerprintScreen = currentRoute?.startsWith(NavRoutes.FINGERPRINT_SETUP_SCREEN) == true



    val isSplashScreen = currentRoute == NavRoutes.SPLASH || isPinEnterScreen || isPinCreationScreen
            || isSecurityQuestionScreen || isSecurityQuestionRecovery
            || isRecentPinScreen || currentRoute == NavRoutes.HOME || currentRoute == NavRoutes.FINGERPRINT_LOGIN_SCREEN
            || isFingerprintScreen|| currentRoute == NavRoutes.IMAGES_SCREEN || currentRoute == NavRoutes.VIDEOS_SCREEN
            || currentRoute == NavRoutes.FILES_SCREEN || currentRoute == NavRoutes.AUDIOS_SCREEN
            || currentRoute == NavRoutes.DISGUISE_ICON_SCREEN || currentRoute == NavRoutes.DOCS_SCREEN || currentRoute == NavRoutes.EXTRACT_IMAGE_SCREEN
            || currentRoute == NavRoutes.CROP_IMAGE_SCREEN || currentRoute == NavRoutes.EXTRACTED_TEXT_SCREEN || isImageViewer
            || isVideoViewer || isAudioPlayer|| currentRoute == NavRoutes.FILE_PREVIEW_SCREEN
            || currentRoute == NavRoutes.NOTES_LIST_SCREEN || currentRoute == NavRoutes.NOTE_EDIT_SCREEN

    DisposableEffect(navController, context) {
        val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            val destRoute = destination.route ?: return@OnDestinationChangedListener
            val destBase = destRoute.substringBefore("?").substringBefore("/")
            if (destBase == NavRoutes.HOME) {
                val prevRoute = controller.previousBackStackEntry?.destination?.route
                if (InterstitialNavPolicy.shouldOfferInterstitial(prevRoute)) {
                    val act = context as? FragmentActivity ?: return@OnDestinationChangedListener
                    EntryPointAccessors.fromApplication(context.applicationContext, AdsEntryPoint::class.java)
                        .interstitialAdManager()
                        .showIfReady(act, "home_after_vault")
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    LaunchedEffect(currentRoute) {
        hasReachedSecurityQuestion = prefs?.getBoolean("has_reached_security_question", false) ?: false
        activity?.window?.let { window ->
            if (isSplashScreen) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            } else {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }
    LaunchedEffect(targetRoute) {
        if (!targetRoute.isNullOrBlank()) {
            navController.navigate(targetRoute) {
                launchSingleTop = true
            }
            onRouteConsumed()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        val route = currentRoute
        val isProtectedRoute = route in listOf(
            NavRoutes.HOME,
            NavRoutes.IMAGES_SCREEN,
            NavRoutes.VIDEOS_SCREEN,
            NavRoutes.FILES_SCREEN,
            NavRoutes.AUDIOS_SCREEN,
            NavRoutes.DOCS_SCREEN,
            NavRoutes.DISGUISE_ICON_SCREEN,
            NavRoutes.EXTRACT_IMAGE_SCREEN,
            NavRoutes.CROP_IMAGE_SCREEN,
            NavRoutes.EXTRACTED_TEXT_SCREEN
        ) || route?.startsWith(NavRoutes.IMAGE_VIEWER_SCREEN) == true
            || route?.startsWith(NavRoutes.VIDEO_PLAYING_SCREEN) == true
            || route?.startsWith(NavRoutes.AUDIO_PLAYING_SCREEN) == true
        if (isProtectedRoute) {
            prefs?.edit()
                ?.putBoolean(AuthSession.KEY_SHOULD_LOCK_ON_RESUME, true)
                ?.putLong(AuthSession.KEY_PAUSED_AT_MS, System.currentTimeMillis())
                ?.apply()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val p = prefs ?: return@LifecycleEventEffect
        if (p.getBoolean(AuthSession.KEY_SKIP_LOCK_ON_RESUME, false)) {
            p.edit()
                .putBoolean(AuthSession.KEY_SKIP_LOCK_ON_RESUME, false)
                .putBoolean(AuthSession.KEY_SHOULD_LOCK_ON_RESUME, false)
                .apply()
            return@LifecycleEventEffect
        }
        if (p.getBoolean(AuthSession.KEY_SHOULD_LOCK_ON_RESUME, false)) {
            val pausedAt = p.getLong(AuthSession.KEY_PAUSED_AT_MS, 0L)
            val awayMs = System.currentTimeMillis() - pausedAt
            val withinGrace = pausedAt > 0L && awayMs in 0..AuthSession.GRACE_PERIOD_MS
            p.edit().putBoolean(AuthSession.KEY_SHOULD_LOCK_ON_RESUME, false).apply()
            if (!withinGrace) {
                pendingLockOnResume = true
            }
        }
    }

    BackHandler(
        enabled = currentRoute == NavRoutes.HOME || (currentRoute != null && currentRoute != NavRoutes.SPLASH && hasReachedSecurityQuestion)
    ) {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onDismiss = { showExitDialog = false },
            onConfirmExit = {
                showExitDialog = false
                (context as? Activity)?.finish()
            }
        )
    }
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }
    Scaffold (
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = colorResource(id = R.color.white)
    ){
            paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        // On Android 16+ (API 36+), edge-to-edge is enforced and Scaffold includes status bar padding
        // We need to exclude status bar padding to match Android 9-15 behavior
        val adjustedPadding = if (Build.VERSION.SDK_INT >= 36) {
            // Android 16+: exclude status bar padding from top
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            androidx.compose.foundation.layout.PaddingValues(
                top = (paddingValues.calculateTopPadding() - statusBarPadding).coerceAtLeast(0.dp),
                bottom = paddingValues.calculateBottomPadding(),
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection)
            )
        }
        else {
            // Android 9-15: use padding as-is
            paddingValues
        }
        val isImageViewer = currentRoute?.startsWith(NavRoutes.IMAGE_VIEWER_SCREEN) == true
        val isVideoViewer = currentRoute?.startsWith(NavRoutes.VIDEO_PLAYING_SCREEN) == true
        val isAudioPlayer = currentRoute?.startsWith(NavRoutes.AUDIO_PLAYING_SCREEN) == true
        val isSecurityQuestionScreen = currentRoute?.startsWith(NavRoutes.SECURITY_QUESTION_SCREEN) == true
        val isSecurityQuestionRecovery = currentRoute?.startsWith(NavRoutes.SECURITY_QUESTION_RECOVERY) == true
        val isFingerprintScreen = currentRoute?.startsWith(NavRoutes.FINGERPRINT_SETUP_SCREEN) == true


        Box(modifier = Modifier
            .then(
                if(currentRoute  ==  NavRoutes.SPLASH || currentRoute == NavRoutes.PIN_ENTER_SCREEN
                    || currentRoute == NavRoutes.PIN_CREATION_SCREEN
                    || isSecurityQuestionScreen|| isSecurityQuestionRecovery
                    || currentRoute == NavRoutes.RESET_PIN_SCREEN || currentRoute == NavRoutes.HOME || currentRoute == NavRoutes.FINGERPRINT_LOGIN_SCREEN
                    || isFingerprintScreen || currentRoute == NavRoutes.IMAGES_SCREEN || currentRoute == NavRoutes.VIDEOS_SCREEN
                    || currentRoute == NavRoutes.FILES_SCREEN || currentRoute == NavRoutes.AUDIOS_SCREEN
                    || currentRoute == NavRoutes.DISGUISE_ICON_SCREEN || currentRoute == NavRoutes.DOCS_SCREEN || currentRoute == NavRoutes.EXTRACT_IMAGE_SCREEN
                    || currentRoute == NavRoutes.CROP_IMAGE_SCREEN || currentRoute == NavRoutes.EXTRACTED_TEXT_SCREEN || isImageViewer
                    || isVideoViewer  || isAudioPlayer || currentRoute == NavRoutes.FILE_PREVIEW_SCREEN
                    || currentRoute == NavRoutes.NOTES_LIST_SCREEN || currentRoute == NavRoutes.NOTE_EDIT_SCREEN)
                    Modifier
                else Modifier.padding(adjustedPadding)
            )
            .fillMaxSize()){
            AppNavGraph(
                navController = navController,
                pendingLockOnResume = pendingLockOnResume,
                onLockHandled = { pendingLockOnResume = false }
            )
        }
    }

}

@Composable
private fun ExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    val accent = colorResource(R.color.splashhalftextColor)
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            colorResource(R.color.splashscreenColor1),
            colorResource(R.color.splashscreenColor2)
        )
    )
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
                    .background(cardGradient, RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp)
            ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape)
                        .border(1.dp, accent.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.cd_exit_dialog_lock),
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.exit_dialog_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.exit_dialog_message),
                    color = colorResource(R.color.splashsubtextcolor),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, colorResource(R.color.nobuttonbordercolor)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.exit_dialog_no),
                                fontFamily = FontFamily(Font(R.font.roboto_medium)),
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Button(
                        onClick = onConfirmExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.yesbuttoncontainercolor),
                            contentColor = Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.exit_dialog_yes),
                                fontFamily = FontFamily(Font(R.font.roboto_medium)),
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

