package dev.blazelight.aiwallpaper.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),     // A light purple
    onPrimary = Color.Black,         // Black text on light purple
    secondary = Color(0xFF03DAC5),   // A teal color
    onSecondary = Color.Black,       // Black text on teal
    tertiary = Color(0xFF03DAC6),    // A slightly different teal
    onTertiary = Color.Black,        // Black text on tertiary color
    background = Color(0xFF121212),  // Dark background
    onBackground = Color.White,      // White text on dark background
    surface = Color(0xFF121212),     // Dark surface
    onSurface = Color.White,         // White text on dark surface
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),     // A deep purple
    onPrimary = Color.White,         // White text on deep purple
    secondary = Color(0xFF03DAC6),   // A teal color
    onSecondary = Color.Black,       // Black text on teal
    tertiary = Color(0xFF018786),    // A darker teal
    onTertiary = Color.White,        // White text on darker teal
    background = Color(0xFFFFFFFF),  // White background
    onBackground = Color.Black,      // Black text on white background
    surface = Color(0xFFFFFFFF),     // White surface
    onSurface = Color.Black,         // Black text on white surface
)

@Composable
fun StableWallpaperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    Log.i("colorscheme", colorScheme.toString())
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}