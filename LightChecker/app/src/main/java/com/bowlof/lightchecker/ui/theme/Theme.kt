package com.bowlof.lightchecker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Amber500,
    onPrimary = Color.White,
    primaryContainer = Amber50,
    onPrimaryContainer = DarkBlue,
    secondary = DarkBlue,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = DarkBlue,
    tertiary = Teal600,
    onTertiary = Color.White,
    error = Red600,
    onError = Color.White,
    surface = Color.White,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    background = Color.White,
    onBackground = OnSurfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Amber400,
    onPrimary = Stone900,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = Amber50,
    secondary = Blue300,
    onSecondary = Stone900,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = SecondaryContainerLight,
    tertiary = TealDark,
    onTertiary = Stone900,
    error = Red500,
    onError = Stone900,
    surface = Stone900,
    onSurface = OnSurfaceDark,
    surfaceVariant = OnSurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    background = Stone950,
    onBackground = OnSurfaceDark,
)

/** App-wide Material 3 theme with Amber/Dark Blue branded palette and Inter font family. */
@Composable
fun LightCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
