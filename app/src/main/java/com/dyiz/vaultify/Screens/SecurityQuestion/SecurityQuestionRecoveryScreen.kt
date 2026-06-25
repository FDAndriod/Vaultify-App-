package com.dyiz.vaultify.Screens.SecurityQuestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SecurityQuestionRecoveryScreen(
    navController: NavHostController,
    viewModel: SecurityQuestionRecoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analytics = rememberVaultifyAnalytics()
    val context = LocalContext.current


    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.SECURITY_QUESTION_RECOVERY, "SecurityQuestionRecoveryScreen")
    }
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )


    val config = LocalConfiguration.current
    val horizontalPadding = (config.screenWidthDp * 0.06f).dp
    val scrollState = rememberScrollState()
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            Text(
                text = stringResource(id = R.string.security_question_title),
                fontSize = 26.sp,
                fontFamily = FontFamily(Font(R.font.roboto_bold)),
                color = colorResource(id = R.color.white),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.security_question_subtitle),
                fontSize = 15.sp,
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                color = colorResource(id = R.color.splashsubtextcolor),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colorResource(id = R.color.securitycardcolor)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(id = R.string.your_question),
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        color = colorResource(id = R.color.securitquestiontextcolor)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.question.ifEmpty { "—" },
                        fontSize = 15.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        color = colorResource(id = R.color.white)
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(id = R.string.enter_answer),
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        color = colorResource(id = R.color.securitquestiontextcolor).copy(alpha = 0.6f)
                    )
                    // UNDERLINE TEXT FIELD
                    TextField(
                        value = uiState.answer,
                        onValueChange = { viewModel.updateAnswer(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
//                    placeholder = {
//                        Text(
//                            text = "Enter your answer",
//                            color = Color.Gray.copy(alpha = 0.5f),
//                            fontSize = 15.sp
//                        )
//                    },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            cursorColor = colorResource(id = R.color.splashhalftextColor),
                            focusedIndicatorColor = colorResource(id = R.color.answerdivider),
                            unfocusedIndicatorColor = colorResource(id = R.color.answerdivider)
                        ),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontFamily = FontFamily(Font(R.font.ffnort_regular))
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.hint.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.hint_prefix, uiState.hint),
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = colorResource(id = R.color.pinscreesubtextcolor),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = colorResource(id = R.color.wronganswercolor),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

//            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    viewModel.onSubmit {
                        navController.navigate(NavRoutes.RESET_PIN_SCREEN) {
                            popUpTo(NavRoutes.PIN_ENTER_SCREEN) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .width(314.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.splashhalftextColor),
                    contentColor = colorResource(id = R.color.splashscreenColor1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.submit),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
