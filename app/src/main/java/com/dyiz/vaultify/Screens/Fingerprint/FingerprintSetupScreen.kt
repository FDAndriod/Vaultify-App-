package com.dyiz.vaultify.Screens.Fingerprint

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay


@Composable
fun FingerprintSetupScreen(navController: NavHostController) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()


    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.FINGERPRINT_SETUP, "FingerprintSetupScreen")
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(colorResource(id = R.color.splashscreenColor1), colorResource(id = R.color.splashscreenColor2))
    )

    // States
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    val biometricManager = remember { BiometricManager.from(context) }
    val authenticators = BIOMETRIC_STRONG

    // Status Check
    val biometricStatus = remember {
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "READY"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "NOT_ENROLLED"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "NO_HARDWARE"
            else -> "UNAVAILABLE"
        }
    }

    // Settings mein bhejne ka function
    val openSettings = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, authenticators)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        context.startActivity(intent)
    }

    fun startScan() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("fingerprint_enabled", true).apply()
                showSuccess = true
                isVerifying = false
            }


            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                isVerifying = false
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    errorMessage = errString.toString()
                    showError = true
                }
            }

            override fun onAuthenticationFailed() {
                isVerifying = false
//                errorMessage = "Fingerprint not Recognized! Try Again"
                showError = true
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.fingerprint_login))
            .setSubtitle(context.getString(R.string.biometric_prompt_subtitle_short))
            .setNegativeButtonText(context.getString(android.R.string.cancel))
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        if (biometricStatus == "READY") {
            delay(500)
            isVerifying = true
            startScan()
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500)
            navController.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.PIN_CREATION_SCREEN) { inclusive = true } }
        }
    }

    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if(showSuccess){
                    Spacer(modifier = Modifier.height(0.dp))
                }else{
                    Spacer(modifier = Modifier.height(80.dp))
                }

                Text(
                    text = if (showSuccess) "" else stringResource(R.string.enable_fingerprint_login),
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))

                when {
                    showSuccess -> SuccessUI(showSuccess)

                    biometricStatus == "NO_HARDWARE" -> {
                        ErrorStateUI(icon = R.drawable.fingerprint, message = stringResource(R.string.biometric_error_no_hardware), buttonText = stringResource(R.string.action_use_pin), onButtonClick = { navController.popBackStack() })
                    }

                    biometricStatus == "NOT_ENROLLED" && !showError -> {
                        ErrorStateUI(icon = R.drawable.fingerprint, message = stringResource(R.string.biometric_setup_none_enrolled), buttonText = stringResource(R.string.action_open_settings), onButtonClick = { openSettings() })
                    }

                    showError -> {
                        val isNoneEnrolled = errorMessage.contains("enrolled", ignoreCase = true) || biometricStatus == "NOT_ENROLLED"

                        ErrorStateUI(
                            icon = R.drawable.fingerprint,
                            message = errorMessage,
                            buttonText = if (isNoneEnrolled) stringResource(R.string.action_open_settings) else stringResource(R.string.action_try_again),
                            onButtonClick = {
                                if (isNoneEnrolled) openSettings() else startScan()
                            }
                        )
                    }

                    else -> {
                        Image(painterResource(id = R.drawable.fingerprint), null, Modifier.size(120.dp).clickable { startScan() })
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = if (isVerifying) stringResource(R.string.hold_verifying) else stringResource(R.string.please_place_finger), fontStyle = FontStyle.Italic,
                            color = Color.White.copy(0.7f), fontFamily = FontFamily(Font(R.font.roboto_regular)), fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
@Composable
private fun ErrorStateUI(
    icon: Int,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            colorFilter = ColorFilter.tint(colorResource(id = R.color.fingerprintcolor))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.White.copy(0.7f), fontFamily = FontFamily(Font(R.font.roboto_regular)),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.splashhalftextColor)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(50.dp).fillMaxWidth(0.7f)
        ) {
            Text(
                text = buttonText, fontSize = 17.sp, fontFamily = FontFamily(Font(R.font.roboto_regular)),
                color = Color.Black
            )
        }
    }
}

@Composable
private fun SuccessUI(showSuccess:Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (showSuccess) stringResource(R.string.verified_and_enabled) else "",
            fontFamily = FontFamily(Font(R.font.roboto_medium)),
            fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(
//            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(id = R.drawable.tickmark), contentDescription = null, modifier = Modifier.size(85.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.fingerprint_vault_access),
            color = Color.White, fontStyle = FontStyle.Italic, fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}
