package dev.blazelight.aiwallpaper

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.blazelight.aiwallpaper.viewmodel.WallpaperViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WallpaperViewModel
    private lateinit var workManager: WorkManager
    private lateinit var imageLoader: WallpaperImageLoader
    private lateinit var wallpaperManager: WallpaperManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize WorkManager
        workManager = WorkManager.getInstance(this)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[WallpaperViewModel::class.java]

        imageLoader = WallpaperImageLoader(applicationContext)
        wallpaperManager = WallpaperManager.getInstance(applicationContext)

        // Set up UI interactions
        setupUI()

    }

    private fun setupUI() {
        findViewById<Button>(R.id.buttonOpenSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            refreshWallpaper()
        }

        findViewById<Button>(R.id.setWallpaperNowButton).setOnClickListener {
            setWallpaperNow()
        }
    }

    private fun setWallpaperNow() {
        Log.i("mainactivity", "called into setWallpaperNow")
        Log.i("mainactivity", wallpaperManager.toString())
        try {
            Log.i("mainactivity", "start try load image as bitmap")
            GlobalScope.launch {
                val wallpaper = imageLoader.getLatestWallpaper()
                Log.i("mainactivity", "loaded wallpaper")
                wallpaperManager.setBitmap(wallpaper)
                Log.i("mainactivity", "set wallpaper")
            }
        } catch (e: Exception) {
            Log.e("mainactivity", e.toString())
        }

    }

    private fun refreshWallpaper() {
        val prompts = getPromptsFromPreferences()

        if (prompts.isEmpty()) {
            Toast.makeText(this, "No prompts available", Toast.LENGTH_SHORT).show()
            return
        }

        showPromptSelectionDialog(prompts)
    }

    private fun getPromptsFromPreferences(): List<String> {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return prefs.getStringSet("prompts", emptySet())?.toList() ?: listOf()
    }

    private fun showPromptSelectionDialog(prompts: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Choose a Prompt")
            .setItems(prompts.toTypedArray()) { _, which ->
                val selectedPrompt = prompts[which]
                WorkManagerHelper.enqueueImageGenerationWork(this, selectedPrompt, true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateNextRefreshTime(): String {
        val interval = getIntervalFromPreferences()
        val nextRefreshTime = Calendar.getInstance().apply { add(Calendar.MINUTE, interval) }.time
        return "Next Refresh: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(nextRefreshTime)}"
    }

    private fun getIntervalFromPreferences(): Int {
        val prefs = this.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val intervalPosition = prefs.getInt("refreshIntervalPosition", 0)
        return when (intervalPosition) {
            0 -> 5
            1 -> 30*60          // 30 minutes
            2 -> 60*60          // 1 hour
            3 -> 180*60         // 3 hours
            4 -> 360*60         // 6 hours
            5 -> 720*60         // 12 hours
            6 -> 1440*60        // Daily (24 hours)
            else -> 30*60       // Default to 30 minutes
        }
    }
}