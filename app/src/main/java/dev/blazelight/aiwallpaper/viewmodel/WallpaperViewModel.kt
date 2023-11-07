package dev.blazelight.aiwallpaper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.blazelight.aiwallpaper.database.WallpaperDatabaseProvider
import dev.blazelight.aiwallpaper.model.WallpaperEntity
import dev.blazelight.aiwallpaper.repository.WallpaperRepository
import kotlinx.coroutines.launch

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WallpaperRepository

    init {
        val wallpaperDao = WallpaperDatabaseProvider(application).database.wallpaperDao()
        repository = WallpaperRepository(wallpaperDao)
    }

    fun insertWallpaper(wallpaper: WallpaperEntity) = viewModelScope.launch {
        repository.insertWallpaper(wallpaper)
    }

    fun getRecentWallpapers() = viewModelScope.launch {
        repository.getRecentWallpapers()
    }

    // Additional methods for API calls will be added here later
}