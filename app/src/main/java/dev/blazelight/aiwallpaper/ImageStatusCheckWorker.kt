package dev.blazelight.aiwallpaper

import HordeApiService
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import androidx.work.workDataOf
import okhttp3.internal.wait
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ImageStatusCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Fetch the request ID from inputData
        val requestId = inputData.getString("requestId") ?: return Result.failure()

        // Create the API service interface using Retrofit
        val apiService = Retrofit.Builder()
            .baseUrl(HordeApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HordeApiService::class.java)

        val response = apiService.checkImageGenerationStatus(requestId)

        if (response.isSuccessful) {
            val responseBody = response.body()
            Log.i("StatusCheck", responseBody.toString())
            if (responseBody != null) {
                val isDone = responseBody.done
                if (isDone) {
                    // If image generation is done, pass the request ID to the next worker
                    //Toast.makeText(applicationContext, "Image generation complete", Toast.LENGTH_SHORT).show()
                    return Result.success(workDataOf("requestId" to requestId))
                } else {
                    // Retry this worker based on the wait_time (in seconds) or after 20 seconds
                    val waitTime = responseBody.wait_time * 1000L // Convert to milliseconds
                    Log.i("WaitTime", waitTime.toString())
                    //Toast.makeText(applicationContext, "Image generation in progress", Toast.LENGTH_SHORT).show()
                    delay(waitTime.coerceAtMost(20000).coerceAtLeast(2000))
                    return Result.retry()
                }
            }
        }
        return Result.retry()
    }
}