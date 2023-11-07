package dev.blazelight.aiwallpaper.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpaper_table")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val imagePath: String,
    val timestamp: Long = System.currentTimeMillis()
)
