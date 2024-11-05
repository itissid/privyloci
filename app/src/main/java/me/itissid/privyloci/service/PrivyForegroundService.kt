package me.itissid.privyloci.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import me.itissid.privyloci.MainActivity
import me.itissid.privyloci.R
import me.itissid.privyloci.SensorManager
import me.itissid.privyloci.SubscriptionManager
import me.itissid.privyloci.util.Logger
import javax.inject.Inject

// TODO: Add methods to add/remove subscription to pass to the SubscriptionManager.
@AndroidEntryPoint
class PrivyForegroundService : Service() {
    @Inject
    lateinit var sensorManager: SensorManager

    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    companion object {
        const val CHANNEL_ID = "PrivyLociForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d(this::class.java.simpleName, "Privy Loci Foreground Service created")

        // Initialize SensorManager
        sensorManager.initialize(this)
        // Initialize SubscriptionManager lazily
        CoroutineScope(Dispatchers.IO).launch {
            subscriptionManager.initialize(this@PrivyForegroundService)
        }

        // Start the foreground service with notification
        startForegroundServiceWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(this::class.java.simpleName, "onStartCommand called")
        // Handle any intents or actions here if needed

        // Service is already running, so return START_STICKY to keep it alive
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(this::class.java.simpleName, "Privy Loci Foreground Service destroyed")

        // Clean up resources
        sensorManager.shutdown()
        subscriptionManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Only consider bindings if one wants to communicate with the service.
        return null
    }

    private fun startForegroundServiceWithNotification() {
        // Create the persistent notification
        try {
            val notification = createNotification()
            // Start the service in the foreground with the notification
            startForeground(1, notification)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                Log.e(
                    "PrivyForegroundService",
                    "Foreground service start not allowed for ${Build.VERSION.SDK_INT} >= ${Build.VERSION_CODES.S}"
                )
            }
        }
    }

    private fun createNotification(): Notification {
        // Create an intent that will open the MainActivity when the notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // small icon for the notification
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW) // Set priority
            .setContentTitle("Privy Loci is running")
            .setContentText("Monitoring your subscriptions")
            .setContentIntent(pendingIntent)
            .build()
    }
}
