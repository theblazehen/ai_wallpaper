package dev.blazelight.aiwallpaper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.*
import java.util.UUID

object WorkManagerHelper {

    private const val PREF_NAME = "MyPrefs"
    private const val WORK_REQUEST_ID_KEY = "work_request_id"
    private const val WORK_NAME = "wallpaperGenerationWork"


    fun enqueueImageGenerationWork(context: Context, prompt: String, replaceExisting: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.i("Prefs", preferences.toString())
        val constraints = getNetworkConstraints()

        val generationInputData = workDataOf("prompt" to prompt, "scale" to preferences.getFloat("scale", 1f), "parallax" to preferences.getBoolean("parallax", false), "model" to preferences.getString("model", "stable_diffusion"), "steps" to preferences.getInt("steps", 30))
        Log.i("Generation input data", generationInputData.toString())
        val downloadInputData = workDataOf("prompt" to prompt)

        val imageGenerationRequest = createImageGenerationRequest(generationInputData, constraints)
        val imageStatusCheckRequest = createStatusCheckRequest(constraints)
        val imageDownloadRequest = createDownloadRequest(downloadInputData, constraints)
        val wallpaperSetRequest = createWallpaperSetRequest(constraints)

        saveWorkRequestId(preferences, imageGenerationRequest.id)
        val existingWorkPolicy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        Log.i("EnquePrompt", prompt)
        Log.i("Enqueue send prefs", generationInputData.toString())

        // Enqueue workers in a chain
        workManager
            .beginUniqueWork(WORK_NAME, existingWorkPolicy, imageGenerationRequest)
            .then(imageStatusCheckRequest)
            .then(imageDownloadRequest)
            .then(wallpaperSetRequest)
            .enqueue()
    }


        private fun getNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private fun createImageGenerationRequest(inputData: Data, constraints: Constraints): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<ImageGenerationWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
    }

    private fun createStatusCheckRequest(constraints: Constraints): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<ImageStatusCheckWorker>()
            .setConstraints(constraints)
            .build()
    }

    private fun createWallpaperSetRequest(constraints: Constraints): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<WallpaperSetWorker>()
            .setConstraints(constraints)
            .build()
    }

    private fun createDownloadRequest(inputData: Data, constraints: Constraints): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<ImageDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
    }
    private fun saveWorkRequestId(preferences: SharedPreferences, id: UUID) {
        preferences.edit().putString(WORK_REQUEST_ID_KEY, id.toString()).apply()
    }

    // Function to retrieve the saved WorkRequest ID
    fun getWorkRequestId(context: Context): String? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(WORK_REQUEST_ID_KEY, null)
    }
}