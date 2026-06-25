package com.dyiz.vaultify.Screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dyiz.vaultify.NavRoutes
import com.dyiz.vaultify.R
import com.dyiz.vaultify.ads.VaultifyAdBanner
import com.dyiz.vaultify.ads.VaultifyBannerFormat
import com.dyiz.vaultify.analytics.VaultifyAnalytics
import com.dyiz.vaultify.analytics.rememberVaultifyAnalytics
import com.dyiz.vaultify.utils.getAppVersionName
import com.dyiz.vaultify.utils.showBrandedToast

/** Opens this app on Google Play (store app or browser). Returns false if nothing handled the intent. */
private fun openPlayStoreAppListing(context: Context): Boolean {
    val pkg = context.packageName
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        )
        true
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
private fun formatVaultItemCount(n: Int): String = when {
    n == 0 -> stringResource(R.string.home_count_empty)
    n == 1 -> stringResource(R.string.home_count_one)
    else -> stringResource(R.string.home_count_many, n)
}

@Composable
private fun formatNotesCount(n: Int): String = when {
    n == 0 -> stringResource(R.string.home_count_notes_empty)
    n == 1 -> stringResource(R.string.home_count_notes_one)
    else -> stringResource(R.string.home_count_notes_many, n)
}

@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val analytics = rememberVaultifyAnalytics()
    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsSidebarOpen by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val counts by viewModel.counts.collectAsState()

    LaunchedEffect(Unit) {
        analytics.logScreenView(VaultifyAnalytics.Screen.HOME, "HomeScreen")
        viewModel.loadCounts()
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colorResource(id = R.color.splashscreenColor1),
            colorResource(id = R.color.splashscreenColor2)
        )
    )

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                CustomBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        if (tab == 1) {
                            analytics.logButtonClick(
                                "bottom_nav_disguise",
                                VaultifyAnalytics.Screen.HOME
                            )
                            navController.navigate(NavRoutes.DISGUISE_ICON_SCREEN)
                        }
                    }
                )
            }
        ) { paddingValues ->
            CompositionLocalProvider(
                LocalDensity provides Density(
                    LocalDensity.current.density,
                    fontScale = 1f
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBackground)
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(15.dp))
                        // Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("Vault")
                                    }
                                    withStyle(style = SpanStyle(color = colorResource(id = R.color.splashhalftextColor))) {
                                        append("ify")
                                    }
                                },
                                fontSize = 22.sp,
                                fontFamily = FontFamily(Font(R.font.lobster_regular))
                            )
                            Row {
                                Box(
                                    modifier = Modifier.size(40.dp).background(
                                        color = Color(0xFF222F39),shape = RoundedCornerShape(10.dp)
                                    )
                                        .clickable(
                                            indication = ripple(),
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) {
                                            analytics.logSettingsOpened()
                                            settingsSidebarOpen = true
                                        },
                                    contentAlignment = Alignment.Center
                                ){
                                    Image(
                                        modifier = Modifier
                                            .size(20.dp),
                                        painter = painterResource(id = R.drawable.locksettingicon),
                                        contentDescription = stringResource(R.string.cd_settings),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.home_subtitle),
                            color = Color(0xFFAFAFAF),
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Card Grid
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                VaultCard(
                                    modifier = Modifier.weight(1f),
                                    icon = painterResource(id = R.drawable.imageicon),
                                    title = stringResource(R.string.vault_title_images),
                                    count = formatVaultItemCount(counts.imageCount),
                                    onClick = {
                                        analytics.logButtonClick(
                                            "vault_card_images",
                                            VaultifyAnalytics.Screen.HOME
                                        )
                                        navController.navigate(NavRoutes.IMAGES_SCREEN)
                                    }
                                )
                                VaultCard(
                                    modifier = Modifier.weight(1f),
                                    icon = painterResource(id = R.drawable.videoicon),
                                    title = stringResource(R.string.vault_title_videos),
                                    count = formatVaultItemCount(counts.videoCount),
                                    onClick = {
                                        analytics.logButtonClick(
                                            "vault_card_videos",
                                            VaultifyAnalytics.Screen.HOME
                                        )
                                        navController.navigate(NavRoutes.VIDEOS_SCREEN)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                VaultCard(
                                    modifier = Modifier.weight(1f),
                                    icon = painterResource(id = R.drawable.fileicon),
                                    title = stringResource(R.string.vault_title_files),
                                    count = formatVaultItemCount(counts.fileCount),
                                    onClick = {
                                        analytics.logButtonClick(
                                            "vault_card_files",
                                            VaultifyAnalytics.Screen.HOME
                                        )
                                        navController.navigate(NavRoutes.DOCS_SCREEN)
                                    }
                                )
                                VaultCard(
                                    modifier = Modifier.weight(1f),
                                    icon = painterResource(id = R.drawable.audioicon),
                                    title = stringResource(R.string.vault_title_audio),
                                    count = formatVaultItemCount(counts.audioCount),
                                    onClick = {
                                        analytics.logButtonClick(
                                            "vault_card_audios",
                                            VaultifyAnalytics.Screen.HOME
                                        )
                                        navController.navigate(NavRoutes.AUDIOS_SCREEN)
                                    }
                                )
                            }

                            NotesWideCard(
                                icon = painterResource(id = R.drawable.notediaryicon),
                                title = stringResource(R.string.vault_title_notes),
                                count = formatNotesCount(counts.noteCount),
                                onClick = {
                                    analytics.logButtonClick(
                                        "vault_card_notes",
                                        VaultifyAnalytics.Screen.HOME
                                    )
                                    navController.navigate(NavRoutes.NOTES_LIST_SCREEN)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        VaultifyAdBanner(
                            analytics = analytics,
                            modifier = Modifier.fillMaxWidth(),
                            format = VaultifyBannerFormat.MediumRectangle
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = settingsSidebarOpen,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize()
        )
        {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null, interactionSource = remember{ MutableInteractionSource()}
                        ) { settingsSidebarOpen = false }
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                SettingsSidebar(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .fillMaxHeight(),
                    onItemClick = { settingsSidebarOpen = false },
                    onShowAbout = { settingsSidebarOpen = false; showAboutDialog = true },
                    navController = navController,
                    analytics = analytics
                )
            }
        }
    }

    if (showAboutDialog) {
        val appVersion = getAppVersionName(context)
        val aboutScroll = rememberScrollState()
        val accent = colorResource(id = R.color.splashhalftextColor)
        val cardBg = colorResource(id = R.color.securitycardcolor)
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(aboutScroll),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.vaultifysplashicon),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.about_app_title),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontFamily = FontFamily(Font(R.font.roboto_medium)),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.about_version, appVersion),
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily(Font(R.font.roboto_regular))
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.about_app_tagline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            color = accent,
                            fontSize = 13.sp,
                            fontFamily = FontFamily(Font(R.font.roboto_medium)),
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = stringResource(R.string.about_app_description),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        fontFamily = FontFamily(Font(R.font.roboto_regular))
                    )
                    TextButton(
                        onClick = { showAboutDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = accent)
                    ) {
                        Text(
                            text = stringResource(R.string.about_close),
                            fontFamily = FontFamily(Font(R.font.roboto_medium)),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebar(
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {},
    onShowAbout: () -> Unit = {},
    navController: NavHostController,
    analytics: VaultifyAnalytics
) {
    val context = LocalContext.current
    val appVersion = getAppVersionName(context)
    var fingerprintEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("fingerprint_enabled", false)
        )
    }
    val settingsScroll = rememberScrollState()
    Column(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Top)
            )
            .fillMaxSize()
            .background(colorResource(id = R.color.securitycardcolor))
            .verticalScroll(settingsScroll)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.roboto_medium)),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionLabel(text = stringResource(R.string.settings_section_security))
        SettingsSidebarItem(
            iconResId = R.drawable.changepiniconsetting,
            label = stringResource(R.string.settings_change_pin),
            onClick = { analytics.logSettingsAction("change_pin"); onItemClick(); navController.navigate(NavRoutes.RESET_PIN_SCREEN) }
        )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = ripple(color = Color.White.copy(alpha = 0.12f)),
                            interactionSource = remember { MutableInteractionSource() }) {
                            fingerprintEnabled = !fingerprintEnabled
                            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("fingerprint_enabled", fingerprintEnabled)
                                .apply()

                            val message = if (fingerprintEnabled) {
                                context.getString(R.string.toast_fingerprint_unlock_enabled)
                            } else {
                                context.getString(R.string.toast_fingerprint_unlock_disabled)
                            }
                            context.showBrandedToast(message)
                        }
                        .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.fingerprinticonsetting),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.settings_fingerprint),
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.roboto_regular)),
                modifier = Modifier.weight(1f)
            )
          /*  SettingsSidebarItem(
                iconResId = R.drawable.fingerprinticonsetting,
                label = "Fingerprint",
                onClick = { *//*onItemClick(); navController.navigate(NavRoutes.RESET_PIN_SCREEN) *//*}
            )*/
            Switch(
                checked = fingerprintEnabled,
                onCheckedChange = { newValue ->
                    analytics.logSettingsAction("fingerprint_toggle")
                    fingerprintEnabled = newValue
                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("fingerprint_enabled", newValue)
                        .apply()
                    val message = if (fingerprintEnabled) {
                        context.getString(R.string.toast_fingerprint_unlock_enabled)
                    } else {
                        context.getString(R.string.toast_fingerprint_unlock_disabled)
                    }
                    context.showBrandedToast(message)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.splashhalftextColor),
                    checkedTrackColor = colorResource(id = R.color.splashhalftextColor).copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        SettingsSidebarItem(
            iconResId = R.drawable.changesecurityquesiconsetting,
            label = stringResource(R.string.settings_security_qa),
            onClick = { analytics.logSettingsAction("change_security_qa"); onItemClick(); navController.navigate("${NavRoutes.SECURITY_QUESTION_SCREEN}?isEdit=true") }
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color.White.copy(alpha = 0.12f)
        )
        SettingsSectionLabel(text = stringResource(R.string.settings_section_store))
        SettingsSidebarItem(
            iconResId = R.drawable.ic_settings_update,
            label = stringResource(R.string.settings_check_update),
            onClick = {
                analytics.logSettingsAction("check_update")
                onItemClick()
                if (openPlayStoreAppListing(context)) {
                    context.showBrandedToast(context.getString(R.string.toast_play_store_opening))
                } else {
                    context.showBrandedToast(context.getString(R.string.toast_play_store_unavailable))
                }
            }
        )
        SettingsSidebarItem(
            iconResId = R.drawable.feedbackiconsetting,
            label = stringResource(R.string.settings_rate_review),
            onClick = {
                analytics.logSettingsAction("feedback")
                onItemClick()
                if (openPlayStoreAppListing(context)) {
                    context.showBrandedToast(context.getString(R.string.toast_play_store_opening))
                } else {
                    context.showBrandedToast(context.getString(R.string.toast_play_store_unavailable))
                }
            }
        )
        SettingsSidebarItem(
            iconResId = R.drawable.shareiconsetting,
            label = stringResource(R.string.settings_share),
            onClick = {
                analytics.logSettingsAction("share")
                onItemClick()
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("skip_lock_on_resume", true).apply()
                val shareText = context.getString(R.string.share_app_text, context.packageName)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                try {
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser_title)))
                } catch (_: Exception) { }
            }
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Color.White.copy(alpha = 0.12f)
        )
        SettingsSectionLabel(text = stringResource(R.string.settings_section_legal))
        SettingsSidebarItem(
            iconResId = R.drawable.abouticonsetting,
            label = stringResource(R.string.settings_about),
            onClick = {
                analytics.logSettingsAction("about")
                onShowAbout()
            }
        )
        SettingsSidebarItem(
            iconResId = R.drawable.privacypoliciyiconsetting,
            label = stringResource(R.string.settings_privacy_policy),
            onClick = {
                analytics.logSettingsAction("privacy_policy")
                onItemClick()
                val url = context.getString(R.string.privacy_policy_url)
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) { }
            }
        )
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.about_version, appVersion),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontFamily = FontFamily(Font(R.font.roboto_regular))
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
        color = Color.White.copy(alpha = 0.42f),
        fontSize = 11.sp,
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        letterSpacing = 1.1.sp
    )
}

@Composable
private fun SettingsSidebarItem(
    iconResId: Int,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.roboto_regular))
        )
    }
}

@Composable
private fun NotesWideCard(
    icon: Painter,
    title: String,
    count: String,
    onClick: () -> Unit
) {
    // Same visual language as VaultCard (same card color + typography) but laid out horizontally
    // across the full width so the Notes entry point reads as a "diary row" rather than another
    // file bucket in the grid above.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.securitycardcolor)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colorResource(id = R.color.homecardstitlecolor),
                    fontSize = 17.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = count,
                    color = colorResource(id = R.color.homecardsubtitlecolor),
                    fontSize = 13.sp,
                    fontFamily = FontFamily(Font(R.font.roboto_regular))
                )
            }
        }
    }
}

@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    count: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .aspectRatio(1.3f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.securitycardcolor)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = icon,
                contentDescription = title,
//                tint = colorResource(id = R.color.splashhalftextColor),
                modifier = Modifier.size(35.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                color = colorResource(id = R.color.homecardstitlecolor),
                fontSize = 17.sp,
                fontFamily = FontFamily(Font(R.font.roboto_regular))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                color = colorResource(id = R.color.homecardsubtitlecolor),
                fontSize = 13.sp,
                fontFamily = FontFamily(Font(R.font.roboto_regular))
            )
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                LocalDensity.current.density,
                fontScale = 1f
            )
        ) {
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.securitycardcolor)
                )

            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        isSelected = selectedTab == 0,
                        icon = painterResource(id = R.drawable.lockhomeblue),
                        label = stringResource(R.string.nav_home),
                        onClick = { onTabSelected(0) }
                    )
                    BottomNavItem(
                        isSelected = selectedTab == 1,
                        icon = painterResource(id = R.drawable.disguiseblueicon),
                        label = stringResource(R.string.nav_app_icon),
                        onClick = { onTabSelected(1) },
                        idleOuterPadding = PaddingValues(start = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    isSelected: Boolean,
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    idleOuterPadding: PaddingValues = PaddingValues(0.dp)
) {
    val accent = colorResource(id = R.color.splashhalftextColor)
    val idleColor = colorResource(id = R.color.bottomiconcolorgray)
    Column(
        modifier = Modifier
            .then(
                if (!isSelected) Modifier.padding(idleOuterPadding) else Modifier
            )
            .padding(8.dp)
            .height(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Slightly larger slot + inset drawable so 1.5dp strokes are not clipped at the edges
        // (the disguise vector is busier than the home icon and was easy to read as “cut off”).
        IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
            Image(
                painter = icon,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                colorFilter = ColorFilter.tint(if (isSelected) accent else idleColor)
            )
        }
        Text(
            text = label,
            color = if (isSelected) accent else idleColor,
            fontSize = 13.sp, fontFamily = FontFamily(Font(R.font.roboto_regular)),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
