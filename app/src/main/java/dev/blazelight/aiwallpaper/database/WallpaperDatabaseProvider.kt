package dev.blazelight.aiwallpaper.database

import android.content.Context
import androidx.room.Room

class WallpaperDatabaseProvider(private val context: Context) {
    val database: WallpaperDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            WallpaperDatabase::class.java,
            "wallpaper_database"
        ).build()
    }
}