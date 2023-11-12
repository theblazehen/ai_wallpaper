package dev.blazelight.aiwallpaper

import HordeApiService
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.blazelight.aiwallpaper.database.WallpaperDatabaseProvider
import dev.blazelight.aiwallpaper.model.WallpaperEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

class ScheduledGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {

            Log.i("ScheduledGenerationWorker", "Generating wallpaper from scheduled work")

            val prefs = applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val prompts = prefs.getStringSet("prompts", emptySet()) ?: emptySet()
            val chosenPrompt = prompts.random()

           WorkManagerHelper.enqueueImageGenerationWork(applicationContext, chosenPrompt, false)

        } catch (e: Exception) {
            Log.e("ScheduledGenerationWorker", "Error generating wallpaper", e)
            return Result.failure()
        }

        return Result.success()

    }

}
