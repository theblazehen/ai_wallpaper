package dev.blazelight.aiwallpaper

import HordeApiService
import android.app.WallpaperManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
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
        //Toast.makeText(applicationContext, "Setting wallpaper", Toast.LENGTH_SHORT).show()


        val wallpaper = imageLoader.getLatestWallpaper()
        wallpaperManager.setBitmap(wallpaper)

        return Result.success()
    }
}
