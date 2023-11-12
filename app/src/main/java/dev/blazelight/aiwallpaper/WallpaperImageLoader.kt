package dev.blazelight.aiwallpaper

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import dev.blazelight.aiwallpaper.database.WallpaperDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory

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

        // Convert the image path to a Bitmap (you'll need to implement this)
        return@withContext convertImagePathToBitmap(wallpaperEntity?.imagePath)
    }

    fun convertImagePathToBitmap(imagePath: String?): Bitmap? {
        if (imagePath == null) {
            return null
        }

        try {
            // Decode the image file into a Bitmap
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888 // Adjust the config as needed
            return BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}
