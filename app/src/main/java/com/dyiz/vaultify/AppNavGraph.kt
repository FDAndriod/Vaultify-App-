package com.dyiz.vaultify

//import com.dyiz.vaultify.Screens.Fingerprint.FingerprintLoginScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dyiz.vaultify.Screens.AudioPlayingScreen
import com.dyiz.vaultify.Screens.AudiosScreen
import com.dyiz.vaultify.Screens.CropImageScreen
import com.dyiz.vaultify.Screens.DisguiseIconScreen
import com.dyiz.vaultify.Screens.DocsScreen
import com.dyiz.vaultify.Screens.ExtractImageScreen
import com.dyiz.vaultify.Screens.ExtractedTextScreen
import com.dyiz.vaultify.Screens.FilePreviewScreen
import com.dyiz.vaultify.Screens.FilesScreen
import com.dyiz.vaultify.Screens.Fingerprint.FingerprintSetupScreen
import com.dyiz.vaultify.Screens.HomeScreen
import com.dyiz.vaultify.Screens.ImageViewerScreen
import com.dyiz.vaultify.Screens.ImagesScreen
import com.dyiz.vaultify.Screens.NoteEditScreen
import com.dyiz.vaultify.Screens.NotesListScreen
import com.dyiz.vaultify.Screens.PinCreation.PinCreationScreen
import com.dyiz.vaultify.Screens.SecurityQuestion.SecurityQuestionRecoveryScreen
import com.dyiz.vaultify.Screens.SecurityQuestion.SecurityQuestionScreen
import com.dyiz.vaultify.Screens.SplashScreen
import com.dyiz.vaultify.Screens.VideoPlayingScreen
import com.dyiz.vaultify.Screens.VideosScreen

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    pendingLockOnResume: Boolean = false,
    onLockHandled: () -> Unit = {}
) {
    // Navigate to PIN_ENTER when lock-on-resume is pending. This runs after NavHost has set the graph,
    // avoiding "Navigation graph has not been set" crash when ON_RESUME fires before composition.
    LaunchedEffect(pendingLockOnResume) {
        if (pendingLockOnResume) {
            navController.navigate(NavRoutes.PIN_ENTER_SCREEN) {
                popUpTo(NavRoutes.PIN_ENTER_SCREEN) { inclusive = true }
                launchSingleTop = true
            }
            onLockHandled()
        }
    }

    NavHost(navController = navController, startDestination = NavRoutes.SPLASH){
        composable(NavRoutes.SPLASH) { SplashScreen(navController = navController) }
        composable(NavRoutes.PIN_CREATION_SCREEN) {
            PinCreationScreen(navController = navController, isEnterMode = false)
        }
        composable(NavRoutes.PIN_ENTER_SCREEN) {
            PinCreationScreen(navController = navController, isEnterMode = true)
        }
        composable(
            route="${NavRoutes.SECURITY_QUESTION_SCREEN}?isEdit={isEdit}",
            arguments = listOf(navArgument("isEdit") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) {backStackEntry->
            val isEdit = backStackEntry.arguments?.getBoolean("isEdit") ?: false
            SecurityQuestionScreen(navController = navController,isEdit=isEdit)
        }
        composable(NavRoutes.SECURITY_QUESTION_RECOVERY) {
            SecurityQuestionRecoveryScreen(navController = navController)
        }
        composable(NavRoutes.RESET_PIN_SCREEN) {
            PinCreationScreen(navController = navController, isEnterMode = false, isResetMode = true)
        }
        composable(NavRoutes.FINGERPRINT_SETUP_SCREEN) {
            FingerprintSetupScreen(navController = navController)
        }
       /* composable(NavRoutes.FINGERPRINT_LOGIN_SCREEN) {
            FingerprintLoginScreen(navController = navController)
        }*/
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(NavRoutes.IMAGES_SCREEN) {
            ImagesScreen(navController = navController)
        }
        composable(NavRoutes.VIDEOS_SCREEN) {
            VideosScreen(navController = navController)
        }
        composable(NavRoutes.FILES_SCREEN) {
            FilesScreen(navController = navController)
        }
        composable(NavRoutes.AUDIOS_SCREEN) {
            AudiosScreen(navController = navController)
        }
        composable(NavRoutes.DISGUISE_ICON_SCREEN) {
            DisguiseIconScreen(navController = navController)
        }
        composable(NavRoutes.DOCS_SCREEN) {
            DocsScreen(navController = navController)
        }
        composable(NavRoutes.EXTRACT_IMAGE_SCREEN) {
            ExtractImageScreen(navController = navController)
        }
        composable(NavRoutes.CROP_IMAGE_SCREEN) {
            CropImageScreen(navController = navController)
        }
        composable(NavRoutes.EXTRACTED_TEXT_SCREEN) {
            ExtractedTextScreen(navController = navController)
        }
        composable(
            route = "${NavRoutes.IMAGE_VIEWER_SCREEN}/{startIndex}",
            arguments = listOf(navArgument("startIndex") { type = NavType.StringType })
        ) { backStackEntry ->
            val startIndex = backStackEntry.arguments?.getString("startIndex")?.toIntOrNull() ?: 0
            ImageViewerScreen(navController = navController, startIndex = startIndex)
        }
        composable(
            route = "${NavRoutes.VIDEO_PLAYING_SCREEN}/{videoPath}",
            arguments = listOf(navArgument("videoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoPath = android.net.Uri.decode(backStackEntry.arguments?.getString("videoPath") ?: "")
            VideoPlayingScreen(navController = navController, videoPath = videoPath)
        }
        composable(
            route = "${NavRoutes.AUDIO_PLAYING_SCREEN}/{audioPath}",
            arguments = listOf(navArgument("audioPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val audioPath = android.net.Uri.decode(backStackEntry.arguments?.getString("audioPath") ?: "")
            AudioPlayingScreen(navController = navController, audioPath = audioPath)
        }
        composable(NavRoutes.FILE_PREVIEW_SCREEN) {
            FilePreviewScreen(navController = navController)
        }
        composable(NavRoutes.NOTES_LIST_SCREEN) {
            NotesListScreen(navController = navController)
        }
        composable(NavRoutes.NOTE_EDIT_SCREEN) {
            NoteEditScreen(navController = navController)
        }
    }
}