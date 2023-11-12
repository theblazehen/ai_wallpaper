package dev.blazelight.aiwallpaper

import HordeApiService
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
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

class ImageDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Fetch the request ID from inputData
        val requestId = inputData.getString("requestId") ?: return Result.failure()
        val prompt = inputData.getString("prompt") ?: return Result.failure()

        Log.i("InDownloadRequestId", requestId.toString())

        // Create the API service interface using Retrofit
        val apiService = Retrofit.Builder()
            .baseUrl(HordeApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HordeApiService::class.java)

        // Make a GET request to retrieve the image info
        val response = apiService.getImageDownloadStatus(requestId)
        Log.i("InDownloadResponse", response.toString())

        if (response.isSuccessful) {
            val responseBody = response.body()
            Log.i("Responsebody", responseBody.toString())
            if (responseBody != null && responseBody.generations.isNotEmpty()) {
                val imageInfo = responseBody.generations[0]
                val imageUrl = imageInfo.img
                Log.i("imageUrl", imageUrl)

                // First, fetch the preference
                val prefs = applicationContext.getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val saveOrSet = prefs.getBoolean("saveOrSet", true)

                if (saveOrSet) {
                    // Download and save the image to the database
                    val imageByteArray = downloadImage(imageUrl)
                    ...
                }
                /*if (imageByteArray != null) {
                    val imagePath = saveImageToFile(imageByteArray)
                    Log.i("ImagePath", imagePath.toString())
                    if (imagePath != null) {
                        saveImageToDatabase(imagePath, prompt)
                    }
                }*/
                // Return success
                return Result.success()
            }
        }

        // If the response is not successful or no image found, retry this worker
        return Result.retry()
    }

    private suspend fun downloadImage(imageUrl: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(imageUrl)
                    .build()

                val response = OkHttpClient.Builder().build().newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    private suspend fun saveImageToFile(imageData: ByteArray): String? {
        return withContext(Dispatchers.IO) {
            try {
                val customDirectoryName = "wallpapers"
                val fileDir = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), customDirectoryName)

                if (!fileDir.exists()) {
                    fileDir.mkdirs() // Create the custom directory if it doesn't exist
                }

                val fileName = "wallpaper_${System.currentTimeMillis()}.jpg" // You can choose a different file format
                val filePath = File(fileDir, fileName)

                FileOutputStream(filePath).use { outputStream ->
                    outputStream.write(imageData)
                    outputStream.flush()
                }

                filePath.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    private suspend fun saveImageToDatabase(imagePath: String, prompt: String) {
        val wallpaperDao = WallpaperDatabaseProvider(applicationContext).database.wallpaperDao()
        val wallpaper = WallpaperEntity(prompt = prompt, imagePath = imagePath)
        wallpaperDao.insertWallpaper(wallpaper)
    }
}
