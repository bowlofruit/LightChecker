package com.bowlof.lightchecker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bowlof.lightchecker.presentation.onboarding.OnboardingRoute
import com.bowlof.lightchecker.presentation.schedule.ScheduleRoute
import com.bowlof.lightchecker.presentation.settings.SettingsRoute

@Composable
fun LightCheckerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.BOOTSTRAP,
        modifier = modifier,
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
        composable(NavRoutes.SCHEDULE) {
            ScheduleRoute(
                onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}
