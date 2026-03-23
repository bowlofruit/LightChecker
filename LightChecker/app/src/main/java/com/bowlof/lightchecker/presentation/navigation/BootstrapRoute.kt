package com.bowlof.lightchecker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@Composable
fun BootstrapRoute(
    navController: NavHostController,
    viewModel: BootstrapViewModel = hiltViewModel(),
) {
    val target by viewModel.targetRoute.collectAsStateWithLifecycle()
    LaunchedEffect(target) {
        navController.navigate(target) {
            popUpTo(NavRoutes.BOOTSTRAP) { inclusive = true }
            launchSingleTop = true
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
