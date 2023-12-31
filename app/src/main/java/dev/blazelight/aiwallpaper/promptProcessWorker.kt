package dev.blazelight.aiwallpaper

import HordeApiService
import ImageGenerationRequest
import ModelGenerationInputStable
import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.blazelight.aiwallpaper.database.WallpaperDatabaseProvider
import dev.blazelight.aiwallpaper.model.WallpaperEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random

fun roundUpToNextMultipleOf64(value: Int): Int {
    return ((value + 63) / 64) * 64
}

class promptProcessWorker(
    appContext: Context,
    workerParams: WorkerParameters
): CoroutineWorker(appContext, workerParams) {

    private lateinit var imageLoader: WallpaperImageLoader
    private lateinit var wallpaperManager: WallpaperManager
    override suspend fun doWork(): Result {

        Log.i("promptProcessWorker", inputData.toString())
        // Fetch the prompt from inputData
        val prompt = inputData.getString("prompt") ?: return Result.failure()

        val width = inputData.getInt("width", 0)
        val height = inputData.getInt("height", 0)

        val model = inputData.getString("model") ?: "stable_diffusion"
        val steps = inputData.getInt("steps", 30) ?: 30

        val upscale = inputData.getBoolean("upscale", false)

        var post_processing = if (upscale) {
            listOf("RealESRGAN_x4plus")
        } else {
            emptyList<String>()
        }

        // Create the API service interface using Retrofit
        val apiService = Retrofit.Builder()
            .baseUrl(HordeApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HordeApiService::class.java)

        // Build the request body
        val request = ImageGenerationRequest(
            prompt = prompt,
            params = ModelGenerationInputStable(
                sampler_name = "k_lms",
                cfg_scale = 7.5,
                denoising_strength = 0.75,
                seed = Random.nextInt().toString(),
                height = height,
                width = width,
                seed_variation = 1000,
                post_processing = post_processing,
                karras = true,
                tiling = false,
                hires_fix = false,
                clip_skip = 1,
                steps = steps,
                n = 1
            ),
            nsfw = false,
            trusted_workers = false,
            slow_workers = true,
            censor_nsfw = false,
            workers = emptyList(),
            worker_blacklist = false,
            models = listOf(model),
            r2 = true,
            shared = true,
            dry_run = false
        )

        var requestId = ""

        // Make the POST request to initiate image generation
        try {
            val response = apiService.initiateImageGeneration(HordeApiService.getApiKey(applicationContext), request)
            Log.i("response", response.toString())
            //Toast.makeText(applicationContext, "Generating image", Toast.LENGTH_SHORT).show()
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.i("ResponseBody", responseBody.toString())
                if (responseBody != null) {
                    // Output the request ID for the next worker
                    requestId = responseBody.id
                } else {
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("Except", e.toString())
            // Handle network request failure
            return Result.failure()
        }

        // Give it a moment to assign it to a worker etc
        delay(5000)

        // Now we wait until the image is generated
        var imageReady = false

        while (!imageReady) {
            val response = apiService.checkImageGenerationStatus(requestId)

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.i("Status check", responseBody.toString())
                if (responseBody != null) {
                    val isDone = responseBody.done
                    if (isDone) {
                        imageReady = true
                    } else {
                        val waitTime = responseBody.wait_time * 1000L + 3000L// Convert to milliseconds, add 3 seconds
                        Log.i("Wait time", waitTime.toString())
                        delay(waitTime.coerceAtMost(30000).coerceAtLeast(5000))
                    }
                }
            }
        }

        // Let's download the image
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
            }
        }

        // Now action based on workMode
        val workMode = inputData.getString("workMode") ?: "Download only"
        Log.i("workmode", workMode.toString())
        if (workMode == "Download only") {
            Log.i("prompt process", "Download only, we're done")
        } else if (workMode == "Set wallpaper") {
            Log.i("Prompt process", "setting wallpaper")
            imageLoader = WallpaperImageLoader(applicationContext)
            wallpaperManager = WallpaperManager.getInstance(applicationContext)

            Log.i("wallpapersetworker", "Try setting wallpaper")
            //Toast.makeText(applicationContext, "Setting wallpaper", Toast.LENGTH_SHORT).show()

            val wallpaper = imageLoader.getLatestWallpaper()
            wallpaperManager.setBitmap(wallpaper)
        }

        // Now delete old images if relevant
        val keepImageCount = inputData.getInt("keepImageCount", 0)
        val selectedImageDirectoryUri = inputData.getString("selectedImageDirectoryUri")
        if (keepImageCount > 0) {
            withContext(Dispatchers.IO) {
                if (selectedImageDirectoryUri != null) {
                    val directoryUri = Uri.parse(selectedImageDirectoryUri)
                    val directory = DocumentFile.fromTreeUri(applicationContext, directoryUri)
                    val files = directory?.listFiles()
                    if (files != null) {
                        val sortedFiles = files.sortedBy { it.lastModified() }
                        if (sortedFiles.size > keepImageCount){
                            val filesToDelete = sortedFiles.take(sortedFiles.size - keepImageCount)
                            filesToDelete.forEach { it.delete() }
                        }
                    }
                }

            }
        }

        return Result.success()

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