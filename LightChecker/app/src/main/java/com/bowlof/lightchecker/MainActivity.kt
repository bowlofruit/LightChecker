package com.bowlof.lightchecker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.bowlof.lightchecker.presentation.navigation.LightCheckerNavHost
import com.bowlof.lightchecker.ui.theme.LightCheckerTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import android.os.SystemClock

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LightCheckerTheme {
                NotificationPermissionRequestEffect()
                ReportFullyDrawnAfterFirstFrameEffect()
                LightCheckerNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun NotificationPermissionRequestEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional: track result */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun ReportFullyDrawnAfterFirstFrameEffect() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (BuildConfig.LOG_STARTUP_TIMING) {
            val start = StartupMetrics.processStartElapsedRealtimeMs()
            if (start > 0L) {
                val ms = SystemClock.elapsedRealtime() - start
                Timber.tag("NFR").d("first_frame_after_process_ms=%d", ms)
            }
        }
        (context as? ComponentActivity)?.reportFullyDrawn()
    }
}
