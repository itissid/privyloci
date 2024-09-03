package me.itissid.privyloci.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.itissid.privyloci.R
import me.itissid.privyloci.util.Logger


class PrivyForegroundService : Service() {
    private var isPlaying = true // Track the playback state
    private val notificationId = 1

    companion object {
        const val CHANNEL_ID = "PrivyLociForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d(this::class.java.simpleName, "Privy Loci FG Service created")
        createNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Inflate the custom notification layout
        Logger.d(this::class.java.simpleName, "onStartCommand called")
        if (intent?.action == "ACTION_TOGGLE_PLAY_PAUSE") {
            togglePlayPause()
        } else {
            Logger.d(this::class.java.simpleName, "calling startForeground")
            startForeground(notificationId, createNotification())
        }
        return START_STICKY


    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources here
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val notificationLayout = RemoteViews(packageName, R.layout.persistent_fg_notification)
        // Set the text for the notification
        notificationLayout.setTextViewText(R.id.first_line, "2*Apps are listening for events")
        notificationLayout.setTextViewText(R.id.second_line, "4*Events served in last 24 hours")

        // Handle the play/pause button action
        val playPauseIntent = Intent(this, PrivyForegroundService::class.java).apply {
            action = "ACTION_TOGGLE_PLAY_PAUSE"
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
             PendingIntent.FLAG_MUTABLE
        )
        notificationLayout.setOnClickPendingIntent(R.id.action_button, playPausePendingIntent)

        // Create the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // small icon for the notification
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Set priority
            .build()
    }

    private fun togglePlayPause() {
        // Toggle the playback state
        isPlaying = !isPlaying

        // Update the notification
        val notificationLayout = RemoteViews(packageName, R.layout.persistent_fg_notification)
        notificationLayout.setImageViewResource(
            R.id.action_button,
            if (isPlaying) R.drawable.ic_no_location_icon else R.drawable.ic_location
        )
        // Check for permission before updating the notification
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Update the notification in the notification manager
            NotificationManagerCompat.from(this).notify(notificationId, createNotification())
        } else {
            // Handle the case where the permission is not granted
            // You can log an error or request the permission from the user
            Logger.w(this::class.java.simpleName, "Permission not granted to update notification")
        }
    }

    // Do your background work here
}