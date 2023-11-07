package dev.blazelight.aiwallpaper

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.blazelight.aiwallpaper.viewmodel.WallpaperViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WallpaperViewModel
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize WorkManager
        workManager = WorkManager.getInstance(this)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[WallpaperViewModel::class.java]

        // Set up UI interactions
        setupUI()

        // Observe WorkRequest status
        observeWorkRequestStatus()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.buttonOpenSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.setWallpaperButton).setOnClickListener {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            refreshWallpaper()
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

    private fun observeWorkRequestStatus() {
        val workRequestId = WorkManagerHelper.getWorkRequestId(this)

        if (workRequestId != null) {
            val workId = UUID.fromString(workRequestId)
            workManager.getWorkInfoByIdLiveData(workId).observe(this, Observer { updateUI(it) })
        }
    }

    private fun updateUI(workInfo: WorkInfo?) {
        val statusTextView: TextView = findViewById(R.id.textViewStatus)
        val timerTextView: TextView = findViewById(R.id.textViewTimer)

        when (workInfo?.state) {
            WorkInfo.State.RUNNING -> {
                statusTextView.text = "Status: In Progress..."
                timerTextView.text = "Next Refresh: Calculating..."
            }
            WorkInfo.State.SUCCEEDED -> {
                statusTextView.text = "Status: Completed Successfully"
                timerTextView.text = calculateNextRefreshTime()
            }
            WorkInfo.State.FAILED -> {
                statusTextView.text = "Status: Failed"
            }
            else -> {
                // Handle other cases if needed
            }
        }
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
            0 -> 30          // 30 minutes
            1 -> 60          // 1 hour
            2 -> 180         // 3 hours
            3 -> 360         // 6 hours
            4 -> 720         // 12 hours
            5 -> 1440        // Daily (24 hours)
            else -> 30       // Default to 30 minutes
        }
    }
}