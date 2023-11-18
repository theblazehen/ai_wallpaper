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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
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

        findViewById<Button>(R.id.refreshButtonScheduled).setOnClickListener {
            refreshWallpaperScheduled()
        }

    }

    private fun refreshWallpaperScheduled() {
        val workManager = WorkManager.getInstance(applicationContext)
        val workRequest = OneTimeWorkRequestBuilder<promptProcessWorker>().build()

        workManager.beginUniqueWork("ScheduledGenerationWorker", ExistingWorkPolicy.REPLACE, workRequest).enqueue()

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


}