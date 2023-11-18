package dev.blazelight.aiwallpaper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
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

        val generationInputData = workDataOf(
            "prompt" to prompt,
            "scale" to preferences.getFloat("scale", 1f),
            "height" to preferences.getInt("height", 512),
            "width" to preferences.getInt("width", 512),
            "upscale" to preferences.getBoolean("upscale", false),
            "model" to preferences.getString("model", "stable_diffusion"),
            "steps" to preferences.getInt("steps", 30),
            "workMode" to preferences.getString("workMode", "Download only"),
            "keepImageCount" to preferences.getInt("keepImageCount", 0),
            "selectedImageDirectoryUri" to preferences.getString("selectedImageDirectoryUri", null),
        )
        Log.i("Generation input data", generationInputData.toString())

        val imageGenerationRequest = createImageGenerationRequest(generationInputData, constraints)

        saveWorkRequestId(preferences, imageGenerationRequest.id)
        val existingWorkPolicy =
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

        Log.i("EnquePrompt", prompt)
        Log.i("Enqueue send prefs", generationInputData.toString())

        // Enqueue workers in a chain
        workManager
            .beginUniqueWork(WORK_NAME, existingWorkPolicy, imageGenerationRequest)
            .enqueue()
    }


    private fun getNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private fun createImageGenerationRequest(
        inputData: Data,
        constraints: Constraints
    ): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<promptProcessWorker>()
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