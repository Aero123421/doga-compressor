package com.example.uiedvideocompacter.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uiedvideocompacter.data.store.UserPreferences
import com.example.uiedvideocompacter.ui.screens.library.LibraryScreen
import com.example.uiedvideocompacter.ui.screens.onboarding.OnboardingScreen
import com.example.uiedvideocompacter.ui.screens.preview.PreviewScreen
import com.example.uiedvideocompacter.ui.screens.progress.ProgressScreen
import com.example.uiedvideocompacter.ui.screens.queue.QueueScreen
import com.example.uiedvideocompacter.ui.screens.result.ResultScreen
import com.example.uiedvideocompacter.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val onboardingCompleted by userPreferences.onboardingCompleted.collectAsState(initial = null)

    if (onboardingCompleted == null) {
        // Wait for preferences to load
        return
    }

    val startDestination = if (onboardingCompleted == true) {
        Screen.Library.route
    } else {
        Screen.Onboarding.route
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToPreview = { encodedUri ->
                    navController.navigate("preview/$encodedUri")
                },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.Preview.route,
            arguments = listOf(
                androidx.navigation.navArgument("encodedUri") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("encodedUri")
            PreviewScreen(
                uriString = uri,
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Queue.route) {
            QueueScreen(
                onNavigateToProgress = { navController.navigate(Screen.Progress.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Progress.route) {
            ProgressScreen(
                onNavigateToResult = {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(Screen.Queue.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Result.route) {
            ResultScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Library.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}