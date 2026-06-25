package com.dyiz.vaultify.Screens

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dyiz.vaultify.DisguiseIconHelper
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics

data class DisguiseOption(val alias: String, @StringRes val labelResId: Int, val iconResId: Int)

/**
 * The grid of disguise choices shown to the user. The order here is the visual order on
 * screen — [DisguiseOption.alias] strings must match both the manifest `<activity-alias>`
 * names and [DisguiseIconHelper.DISGUISE_ALIASES], otherwise applying the disguise will
 * quietly fail to toggle any component.
 */
private val DISGUISE_OPTIONS = listOf(
    DisguiseOption("Vaultify", R.string.launcher_label_vaultify, R.drawable.valtifyicon),
    DisguiseOption("Weather", R.string.launcher_label_weather, R.drawable.weathericon),
    DisguiseOption("Converter", R.string.launcher_label_converter, R.drawable.convertericon),
    DisguiseOption("Music", R.string.launcher_label_music, R.drawable.musicicon),
    DisguiseOption("Calendar", R.string.launcher_label_calendar, R.drawable.calendericon),
    DisguiseOption("Mealio", R.string.launcher_label_mealio, R.drawable.mailoicon),
    DisguiseOption("Calculator", R.string.launcher_label_calculator, R.drawable.calculatoricon),
    DisguiseOption("Notes", R.string.launcher_label_notes, R.drawable.notesicon),
    DisguiseOption("Clock", R.string.launcher_label_clock, R.drawable.clockicon),
    DisguiseOption("Flashlight", R.string.launcher_label_flashlight, R.drawable.flashlighticon),
    DisguiseOption("Wallet", R.string.launcher_label_wallet, R.drawable.walleticon),
    DisguiseOption("Fitness", R.string.launcher_label_fitness, R.drawable.fitnessicon)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseIconScreen(navController: NavHostController) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    val savedAlias = remember { DisguiseIconHelper.getSelectedAlias(context) }
    var selectedAlias by remember { mutableStateOf(savedAlias) }
    var showConfirmSheet by remember { mutableStateOf(false) }
    var pendingAlias by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.DISGUISE_ICON, "DisguiseIconScreen")
        selectedAlias = DisguiseIconHelper.getSelectedAlias(context)
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )
    if (showConfirmSheet && pendingAlias != null) {
        ModalBottomSheet(
            onDismissRequest = { showConfirmSheet = false; pendingAlias = null },
            containerColor = colorResource(id = R.color.bottomsheetcontainercolor),
            contentColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 16.dp) // Tight Spacing
            ) {
                Text(
                    text = stringResource(R.string.disguise_change_title), // "Change Icon"
                    fontSize = 19.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_medium)),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp)) // Kam spacing

                Text(
                    text = stringResource(R.string.disguise_confirm_message),
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular)),
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End, // Buttons on the right
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "No" Button (Transparent/Subtle)
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(100.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable(
                                indication = null, interactionSource = remember{MutableInteractionSource()}
                            ) {
                                showConfirmSheet = false
                                pendingAlias = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel), // "No"
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // "Yes" Button (Blue/Cyan)
                    Button(
                        onClick = {
                            pendingAlias?.let { alias ->
                                DisguiseIconHelper.applyDisguise(context, alias)
                                analytics.logDisguiseIconChanged(alias)
                                selectedAlias = alias
                            }
                            showConfirmSheet = false
                            pendingAlias = null
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .width(100.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.splashhalftextColor), // Cyan/Blue color
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_apply), // "Yes"
                            fontSize = 15.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                    }
                }
            }
        }
    }


    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.disguise_screen_title),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = FontFamily(Font(R.font.roboto_medium))
                            )

                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.securitycardcolor))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DisguiseBottomNavItem(
                                isSelected = false,
                                icon = R.drawable.lockhomeblue,
                                label = stringResource(R.string.nav_home),
                                onClick = {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.HOME) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                            DisguiseBottomNavItem(
                                isSelected = true,
                                icon = R.drawable.disguiseblueicon,
                                label = stringResource(R.string.nav_app_icon),
                                onClick = { }
                            )
                        }
                    }
                }
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
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.disguise_choose_subtitle),
                        color = Color(0xFFAFAFAF),
                        modifier = Modifier.padding(top = 6.dp),
                        fontSize = 12.sp,
                        maxLines = 1,
                        letterSpacing = 0.5.sp,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily(Font(R.font.roboto_regular))
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp)
                    ) {
                        items(DISGUISE_OPTIONS) { option ->
                            val isSelected = selectedAlias == option.alias
                            Card(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) colorResource(id = R.color.splashhalftextColor) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        selectedAlias = option.alias
                                        pendingAlias = option.alias
                                        showConfirmSheet = true
                                    },
                                colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.securitycardcolor)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = option.iconResId),
                                            contentDescription = stringResource(option.labelResId),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = stringResource(option.labelResId),
                                            color = colorResource(id = R.color.white),
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily(Font(R.font.roboto_regular))
                                        )
                                    }
                                    if (isSelected) {
                                        Image(
                                            painter = painterResource(id = R.drawable.mdi_tick_circle),
                                            contentDescription = stringResource(R.string.disguise_selected),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(
                                                    top = 12.dp,
                                                    end = 12.dp,
                                                    start = 12.dp,
                                                    bottom = 12.dp
                                                )
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisguiseBottomNavItem(
    isSelected: Boolean,
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .height(48.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            colorFilter = if(isSelected) ColorFilter.tint(colorResource(id = R.color.splashhalftextColor)) else ColorFilter.tint(colorResource(id = R.color.bottomiconcolorgray)),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (isSelected) colorResource(id = R.color.splashhalftextColor) else colorResource(id = R.color.bottomiconcolorgray),
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
    }
}
