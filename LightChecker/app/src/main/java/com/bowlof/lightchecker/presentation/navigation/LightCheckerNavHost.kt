package com.bowlof.lightchecker.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bowlof.lightchecker.presentation.about.AboutRoute
import com.bowlof.lightchecker.presentation.history.SyncHistoryRoute
import com.bowlof.lightchecker.presentation.onboarding.OnboardingRoute
import com.bowlof.lightchecker.presentation.schedule.ScheduleRoute
import com.bowlof.lightchecker.presentation.schedule.ScheduleViewModel
import com.bowlof.lightchecker.presentation.settings.SettingsRoute

@Composable
fun LightCheckerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    deepLinkRegionId: String? = null,
    deepLinkQueueId: String? = null,
) {
    val animDuration = 300
    NavHost(
        navController = navController,
        startDestination = NavRoutes.BOOTSTRAP,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(animDuration)) + fadeIn(tween(animDuration))
        },
        exitTransition = { fadeOut(tween(animDuration)) },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(animDuration)) + fadeIn(tween(animDuration))
        },
        popExitTransition = { fadeOut(tween(animDuration)) },
    ) {
        composable(NavRoutes.BOOTSTRAP) {
            BootstrapRoute(navController = navController)
        }
        composable(NavRoutes.ONBOARDING) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(NavRoutes.SCHEDULE) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(NavRoutes.ONBOARDING_ADD) {
            OnboardingRoute(
                onFinished = {
                    navController.popBackStack()
                },
            )
        }
        composable(NavRoutes.SCHEDULE) {
            val viewModel: ScheduleViewModel = hiltViewModel()

            if (deepLinkRegionId != null && deepLinkQueueId != null) {
                LaunchedEffect(deepLinkRegionId, deepLinkQueueId) {
                    viewModel.selectPlaceByRegionQueue(deepLinkRegionId, deepLinkQueueId)
                }
            }

            ScheduleRoute(
                onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                viewModel = viewModel,
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onAddPlace = { navController.navigate(NavRoutes.ONBOARDING_ADD) },
                onHistory = { navController.navigate(NavRoutes.SYNC_HISTORY) },
                onAbout = { navController.navigate(NavRoutes.ABOUT) },
            )
        }
        composable(NavRoutes.SYNC_HISTORY) {
            SyncHistoryRoute(onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.ABOUT) {
            AboutRoute(onBack = { navController.popBackStack() })
        }
    }
}
