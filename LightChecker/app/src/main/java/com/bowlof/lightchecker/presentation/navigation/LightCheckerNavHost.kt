package com.bowlof.lightchecker.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun LightCheckerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier,
    ) {
        composable(NavRoutes.HOME) {
            Text(
                text = "Light Checker",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
