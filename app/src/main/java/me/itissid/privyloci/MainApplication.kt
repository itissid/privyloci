package me.itissid.privyloci

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import me.itissid.privyloci.service.PrivyForegroundService
import me.itissid.privyloci.util.Logger

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        Logger.d("MainApplication", "Creating Notification Channel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PrivyForegroundService.CHANNEL_ID,
                "PrivyLociForegroundServiceChannel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for PrivyLoci Foreground Service"
            }
            Logger.d("MainApplication", "Created Notification Channel")
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}