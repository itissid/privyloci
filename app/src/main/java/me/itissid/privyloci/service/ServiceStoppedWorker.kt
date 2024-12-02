package me.itissid.privyloci.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import me.itissid.privyloci.MainActivity
import me.itissid.privyloci.R
import me.itissid.privyloci.service.PrivyForegroundService.Companion.CHANNEL_ID

const val FG_NOTIFICATION_DISMISSED = "FOREGROUND_NOTIFICATION_DISMISSED"
const val FG_NOTIFICATION_DISMISSED_NOTIFICATION_ID = 2
class ServiceStoppedWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        sendServiceStoppedNotification()
        return Result.success()
    }

    private fun sendServiceStoppedNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(FG_NOTIFICATION_DISMISSED, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Privy Loci Service Stopped")
            .setContentText("Tap to understand why its important to resume it.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(FG_NOTIFICATION_DISMISSED_NOTIFICATION_ID, notification)
    }
}

@Composable
fun FGDismissedStoppedDialog(
    onDismiss: () -> Unit,
    onRestartService: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Service Stopped") },
        text = { Text("The location service has stopped. Would you like to restart it?") },
        confirmButton = {
            TextButton(onClick = onRestartService) {
                Text("Restart Service")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}