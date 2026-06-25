package com.dyiz.vaultify.Screens.SecurityQuestion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.OnboardingConstants
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.utils.showBrandedToast
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private const val CUSTOM_QUESTION_OPTION = "Write your own question..."

private val TRENDY_SECURITY_QUESTIONS = listOf(
    "What was the name of your first pet?",
    "What city were you born in?",
    "What is your mother's maiden name?",
    "What was your favorite teacher's name?",
    "What is the name of your best childhood friend?",
    "What street did you grow up on?",
    "What was your first car model?",
    "What is your favorite movie of all time?",
    "What was the name of your first school?",
    "What is your favorite book?",
    "What is your dream job?",
    "What is your favorite food?",
    "What is your zodiac sign?",
    "What is your favorite vacation spot?",
    "What is the name of your favorite song?",
    CUSTOM_QUESTION_OPTION
)

@Composable
fun SecurityQuestionScreen(
    navController: NavHostController,
    isEdit:Boolean,
    viewModel: SecurityQuestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analytics = rememberVaultifyAnalytics()
    val context = LocalContext.current

    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedQuestion by remember { mutableStateOf<String?>(null) }
    val isCustomQuestion = selectedQuestion == CUSTOM_QUESTION_OPTION

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.SECURITY_QUESTION, "SecurityQuestionScreen")
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )
    LaunchedEffect(uiState.question) {
        if (uiState.question.isNotBlank()) {
            if (TRENDY_SECURITY_QUESTIONS.contains(uiState.question)) {
                selectedQuestion = uiState.question
            } else {
                selectedQuestion = CUSTOM_QUESTION_OPTION
            }
        }
    }

    LaunchedEffect(Unit) {
        context.getSharedPreferences("app_prefs", 0)
            .edit()
            .putBoolean("has_reached_security_question", true)
            .apply()
    }

    val config = LocalConfiguration.current
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val buttonBottomMargin = OnboardingConstants.BUTTON_BOTTOM_MARGIN
    val buttonAreaHeight = 56.dp + buttonBottomMargin
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    .padding(bottom = buttonAreaHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                Text(
                    text = if(isEdit) stringResource(id = R.string.security_question_reset_title) else stringResource(id = R.string.security_question_title),
                    fontSize = 23.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    color = colorResource(id = R.color.white),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.security_question_subtitle),
                    fontSize = 15.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = colorResource(id = R.color.pinscreesubtextcolor),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))
                // INPUT FIELDS
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SecurityQuestionDropdown(
                        label = stringResource(id = R.string.choose_question),
                        selectedQuestion = selectedQuestion,
                        onQuestionSelected = { option ->
                            selectedQuestion = option
                            if (option != CUSTOM_QUESTION_OPTION) {
                                viewModel.updateQuestion(option)
                            } else {
                                viewModel.updateQuestion("")
                            }
                        },
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    )

                    if (isCustomQuestion) {
                        SecurityInputField(
                            label = stringResource(id = R.string.enter_your_question),
                            placeholder = stringResource(id = R.string.enter_question_placeholder),
                            value = uiState.question,
                            onValueChange = { viewModel.updateQuestion(it) }
                        )
                    }

                    SecurityInputField(
                        label = stringResource(id = R.string.enter_answer),
                        placeholder = stringResource(id = R.string.enter_answer_placeholder),
                        value = uiState.answer,
                        onValueChange = { viewModel.updateAnswer(it) }
                    )

                    SecurityInputField(
                        label = stringResource(id = R.string.hint_field_label),
                        placeholder = stringResource(id = R.string.write_hint_placeholder),
                        value = uiState.hint,
                        onValueChange = { viewModel.updateHint(it) },
                        isOptional = true
                    )
                }
            }
            Button(
                onClick = {
                    val questionToSave = if (isCustomQuestion) uiState.question else selectedQuestion
                    if (questionToSave.isNullOrBlank() || uiState.answer.isBlank()) {
                        context.showBrandedToast(context.getString(R.string.please_enter_question_answer))
                        return@Button
                    }
                    viewModel.onSetClick {
                        context.showBrandedToast(context.getString(R.string.saved_navigate_home))
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.SECURITY_QUESTION_SCREEN) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = buttonBottomMargin)
                    .width(314.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.splashhalftextColor),
                    contentColor = colorResource(id = R.color.getstartedtextcolor)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isEdit) stringResource(id = R.string.action_update) else stringResource(id = R.string.set_button),
                    fontSize = 17.sp,
                    color = colorResource(id = R.color.getstartedtextcolor),
                    fontFamily = FontFamily(Font(R.font.roboto_medium))
                )
            }
        }
    }
}

@Composable
private fun SecurityQuestionDropdown(
    label: String,
    selectedQuestion: String?,
    onQuestionSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val offsetY = with(density) { 72.dp.roundToPx() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF222F39))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        color = colorResource(id = R.color.securitquestiontextcolor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedQuestion ?: stringResource(id = R.string.select_question_placeholder),
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular)),
                        color = if (selectedQuestion != null) Color.White else colorResource(id = R.color.securityquestionsubtexcolor).copy(alpha = 0.4f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (expanded) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, offsetY),
                onDismissRequest = { onExpandedChange(false) }
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = (config.screenWidthDp - 32).dp)
                        .heightIn(max = 280.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = colorResource(id = R.color.securitycardcolor)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(vertical = 8.dp)
                    ) {
                        TRENDY_SECURITY_QUESTIONS.forEach { question ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQuestionSelected(question)
                                        onExpandedChange(false)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = question,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isOptional: Boolean = false
) {
    val optionalSuffix = if (isOptional) stringResource(R.string.field_optional_suffix) else ""
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF222F39))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label + optionalSuffix,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                color = colorResource(id = R.color.securitquestiontextcolor)
            )

            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.securityquestionsubtexcolor).copy(alpha = 0.4f),
                        fontFamily = FontFamily(Font(R.font.ffnort_regular))
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular))
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    cursorBrush = SolidColor(colorResource(id = R.color.splashhalftextColor))
                )
            }
        }

        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.align(Alignment.CenterEnd).size(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fieldcrossicon),
                    contentDescription = stringResource(R.string.cd_clear),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
