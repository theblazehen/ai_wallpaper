package dev.blazelight.aiwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import dev.blazelight.aiwallpaper.ui.SettingsScreen
import dev.blazelight.aiwallpaper.ui.theme.StableWallpaperTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        setContent {
            val utils = remember { Utils() }
            StableWallpaperTheme {
                SettingsScreen(applicationContext, prefs, utils)
            }

        }
    }
}
