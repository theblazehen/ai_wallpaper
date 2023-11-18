package dev.blazelight.aiwallpaper

import HordeApiService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
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
        //Toast.makeText(applicationContext, "Downloading image", Toast.LENGTH_SHORT).show()

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



                // Download and save the image to the database
                val imageByteArray = downloadImage(imageUrl)

                if (imageByteArray != null) {
                    val imagePath = saveImageToFile(imageByteArray, applicationContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE))
                    Log.i("ImagePath", imagePath.toString())
                    if (imagePath != null) {
                        saveImageToDatabase(imagePath, prompt)
                    }
                }
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
    private suspend fun saveImageToFile(imageData: ByteArray, prefs: SharedPreferences): String? {
        val context = applicationContext
        return withContext(Dispatchers.IO) {
            try {
                // Retrieve the saved directory URI from SharedPreferences
                val savedUriString = prefs.getString("selectedImageDirectoryUri", null)
                val directoryUri = savedUriString?.let { Uri.parse(it) }

                if (directoryUri == null) {
                    Log.e("SaveImage", "Directory URI is null")
                    return@withContext null
                }

                val directory = DocumentFile.fromTreeUri(context, directoryUri)
                val fileName = "wallpaper_${System.currentTimeMillis()}.jpg" // File name with timestamp

                // Create a new file in the selected directory
                val file = directory?.createFile("image/jpeg", fileName)

                file?.let {
                    // Open an output stream to the new file's URI
                    context.contentResolver.openOutputStream(it.uri)?.use { outputStream ->
                        // Write the image data to the file
                        outputStream.write(imageData)
                        outputStream.flush()
                    }
                    return@withContext it.uri.toString() // Return the URI as a string
                }

                null // Return null if file creation failed
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
