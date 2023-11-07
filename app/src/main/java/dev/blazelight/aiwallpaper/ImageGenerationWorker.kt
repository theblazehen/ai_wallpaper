package dev.blazelight.aiwallpaper

import HordeApiService
import ImageGenerationRequest
import ModelGenerationInputStable
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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



        val scale = inputData.getFloat("scale", 0F)
        val width = inputData.getInt("width", 0)
        val height = inputData.getInt(("height"), 0)

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
                seed = "",
                height = scaledHeight,
                width = scaledWidth,
                seed_variation = 1000,
                post_processing = emptyList(),
                karras = true,
                tiling = false,
                hires_fix = false,
                clip_skip = 1,
                steps = 30,
                n = 1
            ),
            nsfw = false,
            trusted_workers = false,
            slow_workers = true,
            censor_nsfw = false,
            workers = emptyList(),
            worker_blacklist = false,
            models = listOf("stable_diffusion"),
            r2 = true,
            dry_run = false
        )

        // Make the POST request to initiate image generation
        try {
            val response = apiService.initiateImageGeneration(HordeApiService.getApiKey(applicationContext), request)
            Log.i("response", response.toString())
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
