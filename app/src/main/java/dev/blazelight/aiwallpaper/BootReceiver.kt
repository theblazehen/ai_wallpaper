package dev.blazelight.aiwallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val utils = Utils()
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            utils.startBackgroundWorker(context, sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE))
        }
    }
}
