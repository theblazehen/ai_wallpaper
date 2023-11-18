package dev.blazelight.aiwallpaper

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import dev.blazelight.aiwallpaper.database.WallpaperDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import android.net.Uri

class WallpaperImageLoader(private val context: Context) {
    private val wallpaperDatabase: WallpaperDatabase

    init {
        // Initialize the Room database
        wallpaperDatabase = Room.databaseBuilder(
            context,
            WallpaperDatabase::class.java,
            "wallpaper_database"
        ).build()
    }

    suspend fun getLatestWallpaper(): Bitmap? = withContext(Dispatchers.IO) {
        // Perform the database query to get the latest wallpaper
        val wallpaperEntity = wallpaperDatabase.wallpaperDao().getLatestWallpaper()

        // Convert the image path (String) to a Uri
        val imageUri = wallpaperEntity.imagePath?.let { Uri.parse(it) }

        // Convert the image Uri to a Bitmap
        return@withContext convertImageUriToBitmap(context, imageUri)
    }

    private fun convertImageUriToBitmap(context: Context, imageUri: Uri?): Bitmap? {
        if (imageUri == null) {
            return null
        }

        try {
            // Get an InputStream from the Uri
            context.contentResolver.openInputStream(imageUri).use { inputStream ->
                if (inputStream == null) return null

                // Decode the image file into a Bitmap
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888 // Adjust the config as needed
                return BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}
