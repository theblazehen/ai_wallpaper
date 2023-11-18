package dev.blazelight.aiwallpaper

import HordeApiService
import ImageGenerationRequest
import ModelGenerationInputStable
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Point
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.random.Random

// Function to round up a value to the next multiple of 64
fun roundUpToNextMultipleOf64(value: Int): Int {
    return ((value + 63) / 64) * 64
}

class ImageGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Fetch the prompt from inputData
        val prompt = inputData.getString("prompt") ?: return Result.failure()

        val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val height = size.y
        var width = size.x.toDouble()

        if (inputData.getBoolean("parallax", false)) {
            width *= 1.3
        }

        val scale = inputData.getFloat("scale", 0F)
        val model = inputData.getString("model") ?: "stable_diffusion"
        val steps = inputData.getInt("steps", 30) ?: 30

        // Calculate the scaled width and height (ensure they are divisible by 64)
        val scaledWidth = roundUpToNextMultipleOf64((width / scale).toInt())
        val scaledHeight = roundUpToNextMultipleOf64((height / scale).toInt())


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
                height = scaledHeight,
                width = scaledWidth,
                seed_variation = 1000,
                post_processing = emptyList(),
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
                    val outputData = workDataOf("requestId" to responseBody.id)
                    return Result.success(outputData)
                }
            }
        } catch (e: Exception) {
            Log.e("Except", e.toString())
            // Handle network request failure
            return Result.retry()
        }

        // If the request fails, return Result.retry() to retry later
        return Result.retry()
    }

}
