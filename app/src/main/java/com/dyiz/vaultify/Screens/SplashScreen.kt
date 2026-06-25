package com.dyiz.vaultify.Screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics.Screen
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: SplashViewModel = hiltViewModel()
){
    val logoAlpha = remember { Animatable(0f) }
    val logoRotationY = remember { Animatable(-90f) }
    val logoScale = remember { Animatable(0.6f) }//0.6f
    val speedXTitleAlpha = remember { Animatable(0f) }
    val speedXTitleScale = remember { Animatable(0.8f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(30f) }

    val progressAlpha = remember { Animatable(0f) }
    val progressBarScale = remember { Animatable(0.5f) }
    val progressBarOffsetY = remember { Animatable(30f) }
    val scope = rememberCoroutineScope()
    val analytics = rememberVaultifyAnalytics()
    val systemUiController = rememberSystemUiController()

    LaunchedEffect(Unit) {
        analytics.logScreenView(Screen.SPLASH, "SplashScreen")
    }
    val statusBarColor = colorResource(id= R.color.splashscreenColor1)
    val gradientBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2),

        )

    )
    LaunchedEffect(Unit) {
            /*showLoader.value = true*/
            scope.launch {
                delay(200)
                launch { logoAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
                launch { logoRotationY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
                launch { logoScale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)) }
            }
            scope.launch {
                delay(1000)
                launch { speedXTitleAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 500, easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f))) } // DecelerateInterpolator
                launch { speedXTitleScale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 500, easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f))) } // DecelerateInterpolator
            }
            scope.launch {
                delay(1000 + 200)
                launch { subtitleAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) }
                launch { subtitleOffsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) }
            }
            scope.launch {
                delay(1600)
                launch { progressAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) }
                launch { progressBarScale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) }
                launch { progressBarOffsetY.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)) }
            }
            delay(2100 + 500)
            val setupComplete = viewModel.isSetupComplete()
            navController.navigate(
                if (setupComplete) NavRoutes.PIN_ENTER_SCREEN else NavRoutes.PIN_CREATION_SCREEN
            ) {
                popUpTo(NavRoutes.SPLASH) { inclusive = true }
            }
    }


    LaunchedEffect (systemUiController,statusBarColor){
        systemUiController.setStatusBarColor(
            color = statusBarColor, darkIcons = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
            Image(
                painter = painterResource(id = R.drawable.vaultifysplashscreenbg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .graphicsLayer(
                            alpha = logoAlpha.value,
                            rotationY = logoRotationY.value,
                            scaleX = logoScale.value,
                            scaleY = logoScale.value,
                            cameraDistance = 8f
                        ),
                    painter = painterResource(id = R.drawable.newappicon),
                    contentDescription = stringResource(R.string.cd_app_logo)
                )
                Spacer(modifier = Modifier.height(100.dp))
                Text(
                    modifier = Modifier
                        .offset(y = (-80).dp)
                        .graphicsLayer(
                            alpha = speedXTitleAlpha.value,
                            scaleX = speedXTitleScale.value,
                            scaleY = speedXTitleScale.value
                        ),
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append("Vault")
                        }

                        withStyle(style = SpanStyle(color = colorResource(id = R.color.splashhalftextColor))) {
                            append("ify")
                        }
                    },
                    fontSize = 35.sp,
                    fontFamily = FontFamily(Font(R.font.lobster_regular))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily(Font(R.font.robotocondensed_italic)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth().graphicsLayer(alpha = subtitleAlpha.value)
                        .offset(y = (-80).dp).offset(y = subtitleOffsetY.value.dp),
                    color = colorResource(id = R.color.splashsubtextcolor)
                )
            }
        }
    }

}