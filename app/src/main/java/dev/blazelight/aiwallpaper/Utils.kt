package dev.blazelight.aiwallpaper

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class Utils (){

    /*fun calculateNextRefreshTime(): String {
        val interval = getIntervalFromPreferences()
        val nextRefreshTime = Calendar.getInstance().apply { add(Calendar.MINUTE, interval) }.time
        return "Next Refresh: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(nextRefreshTime)}"
    }*/

    fun getIntervalFromPreferences(sharedPreferences: SharedPreferences): Int {
        val intervalPosition = sharedPreferences.getInt("refreshIntervalPosition", 0)
        return when (intervalPosition) {
            0 -> 5
            1 -> 30*60          // 30 minutes
            2 -> 60*60          // 1 hour
            3 -> 180*60         // 3 hours
            4 -> 360*60         // 6 hours
            5 -> 720*60         // 12 hours
            6 -> 1440*60        // Daily (24 hours)
            else -> 15*60       // Default to 30 minutes
        }
    }

    fun startBackgroundWorker(context: Context, sharedPreferences: SharedPreferences) {
        val interval = getIntervalFromPreferences(sharedPreferences) / 60
        val workManager = WorkManager.getInstance()
        val periodicWorkRequest = PeriodicWorkRequestBuilder<ScheduledGenerationWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork("ScheduledGenerationWorker", ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest)
    }
}