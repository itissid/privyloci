package me.itissid.privyloci.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.itissid.privyloci.MainActivity
import me.itissid.privyloci.R
import me.itissid.privyloci.SensorManager
import me.itissid.privyloci.SubscriptionManager
import me.itissid.privyloci.util.Logger
import javax.inject.Inject
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy

@AndroidEntryPoint
class PrivyForegroundService : Service() {
    @Inject
    lateinit var sensorManager: SensorManager

    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    companion object {
        const val CHANNEL_ID = "PrivyLociForegroundServiceChannel"
        const val ACTION_SERVICE_STARTED = "me.itissid.privyloci.ACTION_SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "me.itissid.privyloci.ACTION_SERVICE_STOPPED"
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

        val intent = Intent(ACTION_SERVICE_STARTED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d(this::class.java.simpleName, "Privy Loci  onStartCommand called")
        // Handle any intents or actions here if needed

        // Service is already running, so return START_STICKY to keep it alive
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(this::class.java.simpleName, "Privy Loci Foreground Service destroyed")

        // Clean up resources
        try {
            sensorManager.shutdown()
            subscriptionManager.shutdown()
        } catch (e: Exception) {
            Logger.e(
                this::class.java.simpleName,
                "Error shutting down subscription or sensor manager",
                e
            )
        } finally {
            val intent = Intent(ACTION_SERVICE_STOPPED)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // When user removes a task from the preview
        Logger.d(this::class.java.simpleName, "FG Task removed")

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
                // TODO: How does one set up instrumentation for tracking these errors in production?
                // N2S: This happens typically due to lack of permissions but also due to a bug in the app where we try start a FG service from background. e.g. using LaunchedEffects etc.

                Log.e(
                    "PrivyForegroundService",
                    "Foreground service start not allowed for Android SDK version `${Build.VERSION.SDK_INT}` >= ${Build.VERSION_CODES.S}"
                )
            } else {
                Log.e(
                    "PrivyForegroundService",
                    "Error starting foreground service",
                    e
                )
            }
        }
    }

    private fun createNotification(): Notification {
        // Create an intent that will open the MainActivity when the notification is tapped
        val deleteIntent = Intent(this, NotificationDismissedReceiver::class.java)
        val pendingDeleteIntent =
            PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_IMMUTABLE)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // small icon for the notification
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle("Privy Loci is running")
            .setContentText("Monitoring your subscriptions")
            .setContentIntent(pendingIntent)
            .setDeleteIntent(pendingDeleteIntent)
            .setOngoing(true)
            .build()
    }
}

class NotificationDismissedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Have a viewmodel here set so that
        Logger.d(
            "NotificationDismissedReceiver",
            "Persistent Notification dismissed, stopping FG Service"
        )
        context?.let {
            // N2S: I decided to stop the foreground services en-masse but we could be more sparing.
            // We can send an intent to stop services that have private data only and let others run.
            stopPrivyForegroundService(it)
        }
        // TODO: Also update the user preference that the notification was dismissed.
        if (context != null) {
            val workRequest = OneTimeWorkRequestBuilder<ServiceStoppedWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

