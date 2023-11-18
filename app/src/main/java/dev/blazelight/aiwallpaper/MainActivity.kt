package dev.blazelight.aiwallpaper

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.compose.material3.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.blazelight.aiwallpaper.viewmodel.WallpaperViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import dev.blazelight.aiwallpaper.Utils

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: WallpaperViewModel
    private lateinit var workManager: WorkManager
    private lateinit var imageLoader: WallpaperImageLoader
    private lateinit var wallpaperManager: WallpaperManager
    private lateinit var utils: Utils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        utils = Utils()
        setContent {
            MyApp()
        }

        // Initialize WorkManager, ViewModel, etc.
        workManager = WorkManager.getInstance(this)
        viewModel = ViewModelProvider(this)[WallpaperViewModel::class.java]
        imageLoader = WallpaperImageLoader(applicationContext)
        wallpaperManager = WallpaperManager.getInstance(applicationContext)
    }

    @Composable
    fun MyApp() {
        MaterialTheme {
            MainContent()
        }
    }

    @Composable
    fun MainContent() {
        val context = LocalContext.current
        var (showDialog, setShowDialog) = remember { mutableStateOf(false) }
        val prompts = getPromptsFromPreferences(context)

        Log.i("MainActivity", "Prompts: $prompts")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }) {
                Text("Configure Wallpaper")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (prompts.isEmpty()) {
                    Toast.makeText(context, "No prompts available", Toast.LENGTH_SHORT).show()
                } else {
                    setShowDialog(true)
                }
            }) {
                Text("Refresh Now with specific prompt")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                utils.startBackgroundWorker(context, context.getSharedPreferences("MyPrefs", MODE_PRIVATE))
            }) {
                Text("Refresh Now with random prompt")
            }
        }

        if (showDialog) {
            ShowPromptSelectionDialog(
                prompts = prompts,
                onDismiss = { showDialog = false },
                onSelect = { selectedPrompt ->
                    // Handle the selected prompt
                    setShowDialog(false)
                    WorkManagerHelper.enqueueImageGenerationWork(context, selectedPrompt, true)
                }
            )
        }
    }

    @Composable
    fun ShowPromptSelectionDialog(prompts: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
        val context = LocalContext.current
        if (prompts.isNotEmpty()) {
            MaterialAlertDialogBuilder(context)
                .setTitle("Choose a Prompt")
                .setItems(prompts.toTypedArray()) { _, which ->
                    val selectedPrompt = prompts[which]
                    onSelect(selectedPrompt)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss()
                }
                .show()
        }
    }

    private fun getPromptsFromPreferences(context: Context): List<String> {
        val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return prefs.getStringSet("prompts", emptySet())?.toList() ?: listOf()
    }
}