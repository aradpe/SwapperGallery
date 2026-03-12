package com.swappergallery.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.swappergallery.ui.editor.EditorScreen
import com.swappergallery.ui.gallery.GalleryScreen
import com.swappergallery.ui.viewer.ViewerScreen

object Routes {
    const val GALLERY = "gallery"
    const val VIEWER = "viewer/{imageUri}"
    const val EDITOR = "editor/{imageUri}"

    fun viewer(uri: String) = "viewer/${Uri.encode(uri)}"
    fun editor(uri: String) = "editor/${Uri.encode(uri)}"
}

@Composable
fun SwapperNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.GALLERY,
    externalImageUri: String? = null,
    onExternalImageHandled: (() -> Unit)? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.GALLERY) {
            GalleryScreen(
                onImageClick = { uri ->
                    navController.navigate(Routes.viewer(uri.toString()))
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val imageUri = Uri.decode(encodedUri)
            ViewerScreen(
                imageUri = imageUri,
                onBack = { navController.popBackStack() },
                onEdit = { uri ->
                    navController.navigate(Routes.editor(uri))
                }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val imageUri = Uri.decode(encodedUri)
            EditorScreen(
                imageUri = imageUri,
                onBack = { navController.popBackStack() }
            )
        }
    }

    // Handle external image URI (one-shot: navigate then clear)
    LaunchedEffect(externalImageUri) {
        if (externalImageUri != null) {
            navController.navigate(Routes.editor(externalImageUri)) {
                launchSingleTop = true
            }
            onExternalImageHandled?.invoke()
        }
    }
}
