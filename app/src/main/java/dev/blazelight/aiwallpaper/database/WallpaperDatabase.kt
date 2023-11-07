package dev.blazelight.aiwallpaper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.blazelight.aiwallpaper.dao.WallpaperDao
import dev.blazelight.aiwallpaper.model.WallpaperEntity

@Database(entities = [WallpaperEntity::class], version = 1, exportSchema = false)
abstract class WallpaperDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
}
