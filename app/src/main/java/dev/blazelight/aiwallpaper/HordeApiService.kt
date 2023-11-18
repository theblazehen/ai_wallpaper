import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import android.content.Context

data class ImageGenerationRequest(
    val prompt: String,
    val params: ModelGenerationInputStable,
    val nsfw: Boolean,
    val trusted_workers: Boolean,
    val slow_workers: Boolean,
    val censor_nsfw: Boolean,
    val workers: List<String>,
    val worker_blacklist: Boolean,
    val models: List<String>,
    val r2: Boolean,
    val dry_run: Boolean,
    val shared: Boolean,
)

data class ModelGenerationInputStable(
    val sampler_name: String,
    val cfg_scale: Double,
    val denoising_strength: Double,
    val seed: String,
    val height: Int,
    val width: Int,
    val seed_variation: Int,
    val post_processing: List<String>,
    val karras: Boolean,
    val tiling: Boolean,
    val hires_fix: Boolean,
    val clip_skip: Int,
    val steps: Int,
    val n: Int
)

data class ImageGenerationResponse(
    val id: String,
    val kudos: Int
)

data class ImageGenerationStatus(
    val finished: Int,
    val processing: Int,
    val restarted: Int,
    val waiting: Int,
    val done: Boolean,
    val faulted: Boolean,
    val wait_time: Int,
    val queue_position: Int,
    val kudos: Int,
    val is_possible: Boolean
)

data class ImageDownloadStatus(
    val generations: List<ImageGeneration>,
    val shared: Boolean,
    val finished: Int,
    val processing: Int,
    val restarted: Int,
    val waiting: Int,
    val done: Boolean,
    val faulted: Boolean,
    val wait_time: Int,
    val queue_position: Int,
    val kudos: Int,
    val is_possible: Boolean
)

data class ImageGeneration(
    val img: String,
    val seed: String,
    val id: String,
    val censored: Boolean,
    val gen_metadata: List<Any>,
    val worker_id: String,
    val worker_name: String,
    val model: String,
    val state: String
)


interface HordeApiService {
    companion object {
        const val BASE_URL = "https://stablehorde.net" // Your base URL
        fun getApiKey(context: Context): String {
            val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return prefs.getString("apiKey", "0000000000") ?: "0000000000"
        }
    }

    @POST("/api/v2/generate/async")
    suspend fun initiateImageGeneration(
        @Header("apikey") apiKey: String,
        @Body request: ImageGenerationRequest
    ): Response<ImageGenerationResponse>

    @GET("/api/v2/generate/check/{requestId}")
    suspend fun checkImageGenerationStatus(@Path("requestId") requestId: String): Response<ImageGenerationStatus>

    @GET("api/v2/generate/status/{requestId}")
    suspend fun getImageDownloadStatus(@Path("requestId") requestId: String): Response<ImageDownloadStatus>
}