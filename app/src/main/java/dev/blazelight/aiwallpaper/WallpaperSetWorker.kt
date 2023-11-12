package dev.blazelight.aiwallpaper

import HordeApiService
import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import androidx.work.workDataOf
import okhttp3.internal.wait
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WallpaperSetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private lateinit var imageLoader: WallpaperImageLoader
    private lateinit var wallpaperManager: WallpaperManager

    override suspend fun doWork(): Result {
        imageLoader = WallpaperImageLoader(applicationContext)
        wallpaperManager = WallpaperManager.getInstance(applicationContext)

        Log.i("wallpapersetworker", "Try setting wallpaper")
        // First, fetch the preference
        val prefs = applicationContext.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val saveOrSet = prefs.getBoolean("saveOrSet", true)

        if (!saveOrSet) {
            try {
                val wallpaper = imageLoader.getLatestWallpaper()
                wallpaperManager.setBitmap(wallpaper)
            } catch (e: Exception) {
                return Result.failure()
            }
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }
}
