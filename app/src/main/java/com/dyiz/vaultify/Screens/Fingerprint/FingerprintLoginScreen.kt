package com.dyiz.vaultify.Screens.Fingerprint

/*@Composable
fun FingerprintLoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val statusBarColor = colorResource(id = R.color.splashscreenColor1)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )

    var authState by remember { mutableStateOf<FingerprintAuthState>(FingerprintAuthState.Idle) }
    var failedAttempts by remember { mutableStateOf(0) }
    val maxAttempts = 5
    val lockoutMinutes = 15

    LaunchedEffect(systemUiController, statusBarColor) {
        systemUiController.setStatusBarColor(color = statusBarColor, darkIcons = true)
    }

    LaunchedEffect(authState) {
        if (authState is FingerprintAuthState.Success) {
            navController.navigate(NavRoutes.HOME) {
                popUpTo(NavRoutes.SPLASH) { inclusive = true }
            }
        }
    }

    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                authState = FingerprintAuthState.Success
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        authState = FingerprintAuthState.TooManyAttempts(lockoutMinutes)
                    }
                    BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        authState = FingerprintAuthState.Idle
                    }
                    else -> {
                        failedAttempts++
                        if (failedAttempts >= maxAttempts) {
                            authState = FingerprintAuthState.TooManyAttempts(lockoutMinutes)
                        } else {
                            authState = FingerprintAuthState.Error(errString.toString())
                        }
                    }
                }
            }
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                failedAttempts++
                if (failedAttempts >= maxAttempts) {
                    authState = FingerprintAuthState.TooManyAttempts(lockoutMinutes)
                } else {
                    authState = FingerprintAuthState.Error(context.getString(R.string.fingerprint_not_recognized))
                }
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.fingerprint_login))
            .setSubtitle(context.getString(R.string.authenticate_using_fingerprint))
            .setNegativeButtonText(context.getString(android.R.string.cancel))
            .build()
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt()
        } else {
            authState = FingerprintAuthState.Error(context.getString(R.string.no_fingerprint_sensor))
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = stringResource(R.string.enable_fingerprint_login),
                    fontSize = 22.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    color = colorResource(id = R.color.white),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.authenticate_that_its_you),
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = colorResource(id = R.color.pinscreesubtextcolor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))

                when (val state = authState) {
                    FingerprintAuthState.Success -> {
                        Image(
                            painter = painterResource(id = R.drawable.fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorResource(id = R.color.splashhalftextColor))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.recognized_and_proceeding),
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium)),
                            color = colorResource(id = R.color.splashhalftextColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is FingerprintAuthState.TooManyAttempts -> {
                        Image(
                            painter = painterResource(id = R.drawable.fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorResource(id = R.color.wronganswercolor))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.too_many_attempts),
                            fontSize = 15.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_regular)),
                            color = colorResource(id = R.color.wronganswercolor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text(stringResource(android.R.string.ok), color = colorResource(id = R.color.splashhalftextColor))
                        }
                    }
                    is FingerprintAuthState.Error -> {
                        Image(
                            painter = painterResource(id = R.drawable.fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorResource(id = R.color.wronganswercolor))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            fontSize = 15.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_regular)),
                            color = colorResource(id = R.color.wronganswercolor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { authState = FingerprintAuthState.Idle; showBiometricPrompt() }
                        ) {
                            Text("Fingerprint not Recognized! Try Again", color = colorResource(id = R.color.splashhalftextColor))
                        }
                    }
                    FingerprintAuthState.Idle -> {
                        Image(
                            painter = painterResource(id = R.drawable.fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.please_place_finger),
                            fontSize = 15.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_regular)),
                            color = colorResource(id = R.color.splashhalftextColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.Button(
                            onClick = { showBiometricPrompt() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.splashhalftextColor),
                                contentColor = colorResource(id = R.color.getstartedtextcolor)
                            )
                        ) {
                            Text(stringResource(R.string.please_place_finger))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.answer_security_question_instead),
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = colorResource(id = R.color.splashhalftextColor),
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            navController.navigate(NavRoutes.SECURITY_QUESTION_RECOVERY) {
                                popUpTo(NavRoutes.FINGERPRINT_LOGIN_SCREEN) { inclusive = true }
                            }
                        }
                )
            }
        }
    }
}*/

private sealed class FingerprintAuthState {
    data object Idle : FingerprintAuthState()
    data object Success : FingerprintAuthState()
    data class Error(val message: String) : FingerprintAuthState()
    data class TooManyAttempts(val minutes: Int) : FingerprintAuthState()
}
