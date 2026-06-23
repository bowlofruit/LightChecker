package com.bowlof.lightchecker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bowlof.lightchecker.presentation.navigation.LightCheckerNavHost
import com.bowlof.lightchecker.ui.theme.LightCheckerTheme
import com.bowlof.lightchecker.work.SyncScheduleWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var deepLink by mutableStateOf<DeepLinkArgs?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink = intent?.toDeepLinkOrNull()

        setContent {
            LightCheckerTheme {
                NotificationPermissionRequestEffect()
                ReportFullyDrawnAfterFirstFrameEffect()
                val link = deepLink
                LightCheckerNavHost(
                    modifier = Modifier.fillMaxSize(),
                    deepLinkRegionId = link?.regionId,
                    deepLinkQueueId = link?.queueId,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.toDeepLinkOrNull()
    }
}

private data class DeepLinkArgs(val regionId: String, val queueId: String)

private fun Intent.toDeepLinkOrNull(): DeepLinkArgs? {
    val region = getStringExtra(EXTRA_NOTIFICATION_REGION) ?: return null
    val queue = getStringExtra(EXTRA_NOTIFICATION_QUEUE) ?: return null
    return DeepLinkArgs(region, queue)
}

@Composable
private fun NotificationPermissionRequestEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Timber.d("POST_NOTIFICATIONS permission result: granted=%b", granted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.POST_NOTIFICATIONS,
            ) ?: false
            if (shouldShowRationale) {
                Timber.d("Notification permission rationale should be shown")
            }
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

private const val EXTRA_NOTIFICATION_REGION = SyncScheduleWorker.EXTRA_REGION
private const val EXTRA_NOTIFICATION_QUEUE = SyncScheduleWorker.EXTRA_QUEUE
