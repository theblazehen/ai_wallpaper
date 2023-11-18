package dev.blazelight.aiwallpaper

import android.content.Context
import android.content.SharedPreferences
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class Utils() {

    /*fun calculateNextRefreshTime(): String {
        val interval = getIntervalFromPreferences()
        val nextRefreshTime = Calendar.getInstance().apply { add(Calendar.MINUTE, interval) }.time
        return "Next Refresh: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(nextRefreshTime)}"
    }*/

    fun startBackgroundWorker(context: Context, sharedPreferences: SharedPreferences) {
        val interval = sharedPreferences.getInt("refreshInterval", 15)
        val workManager = WorkManager.getInstance()
        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<ScheduledGenerationWorker>(interval.toLong(), TimeUnit.MINUTES)
                .build()

        workManager.enqueueUniquePeriodicWork(
            "ScheduledGenerationWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }
}