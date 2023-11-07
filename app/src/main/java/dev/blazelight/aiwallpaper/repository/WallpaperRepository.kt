package dev.blazelight.aiwallpaper.repository

import dev.blazelight.aiwallpaper.dao.WallpaperDao
import dev.blazelight.aiwallpaper.model.WallpaperEntity

class WallpaperRepository(private val wallpaperDao: WallpaperDao) {

    suspend fun insertWallpaper(wallpaper: WallpaperEntity) {
        wallpaperDao.insertWallpaper(wallpaper)
    }

    suspend fun getRecentWallpapers(): List<WallpaperEntity> {
        return wallpaperDao.getRecentWallpapers()
    }

    // Additional methods for API calls will be added here later
}
