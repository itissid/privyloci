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
// The sco
// Should clients(any activity)  bind to this service to communicate with it ?
// I think the only reason to do binding is to start/stop the persistent notification on some kind of an event. I am not sure i want this.
// I want the FG service to  collect the location and the persistent notification to display to the user that we are doing so.
// Lets look at the UI and UX usecases, cause thats how to best understand persmissions flow for my app. This will also help me in
// migrating the current places/assets and subscriptions from the current Layout based structure to fragments.
// Objective 1: Minimally create a screen for the app with a top bar and a bottom bar and reus ethe the PLace/Asset Fragment code in it.
// Objective 2:
// 1. On the dashboard there is one icon that is displayed on the top bar if the location permissions are not granted.
//      (1) Create a LocationPermissionState class that has a needsPermission() fn that returns a boolean.
//      (2) Convert the current activity into a composable(like LocationUpdateScreen): Create a `var showRationaleDialog by remember { mutableStateOf(false) }` and a topbar icon state set to warn  if needsPermission is true.
//      Mutate showRationaleDialog when this icon is clicked( its possible we want to hoist this variable later and use it in the subscription cards as well). TEST: set needsPermission to always true and a rationale should
//      always display when warn icon is clicked. Here is the code snippet to follow:
/**
 *     var showRationaleDialog by remember { mutableStateOf(false) }
 *     if (showRationaleDialog) {
 *         PermissionRationaleDialog(
 *             onConfirm = {
 *                 showRationaleDialog = false
 *                 onButtonClick()
 *             },
 *             onDismiss = { showRationaleDialog = false }
 *         )
 *     }
 *
 *     fun onClick() {
 *         if (needsPermissionRationale) {
 *             showRationaleDialog = true
 *         } else {
 *             onButtonClick()
 *         }
 *     }
 */
//    (3). What happens when subscriptions are displayed for the first time? Lets say the app may or may not have permission ATM. Lets have a mutable state variable and a rationale thingy in each card too. Just like above.
//          3.a: Create a card composable from the data model and add the above logic. Test that all of the cards display the warn icon and the rationale. If the user dismisses the rationale nothing in the UI changes.
//          3.b: If the user confirms, trigger the Permission Flow. TEST: Is the permission is granted.
//    (4). TEST: If the permission is granted, the icon should disappear and the cards should display the subscription data.
//      _THINKING_: Do I need finer grain location permission like per app's subscription? Maybe. I'll think about it later.
//      the onclick of the icon shows a rational if needsPermission is true.

// So now lets hook in the permissions.
// 1. Implement  Location Preferences as a master switch with an intent from one of our activities:
/**
 *  if (intent?.action == ACTION_STOP_UPDATES) {
 *             stopLocationUpdates()
 *             lifecycleScope.launch {
 *                 locationPreferences.setLocationTurnedOn(false)
 *             }
 *         }
 */
// 2. Permissions: IMPLEMENT ME
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