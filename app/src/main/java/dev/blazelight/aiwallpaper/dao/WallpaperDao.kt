package dev.blazelight.aiwallpaper.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.blazelight.aiwallpaper.model.WallpaperEntity

@Dao
interface WallpaperDao {
    @Insert
    suspend fun insertWallpaper(wallpaper: WallpaperEntity)

    @Query("SELECT * FROM wallpaper_table ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentWallpapers(): List<WallpaperEntity>

    @Query("SELECT * FROM wallpaper_table ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWallpaper(): WallpaperEntity

    // Additional queries as required
}