package me.itissid.privyloci.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import me.itissid.privyloci.TAG
import me.itissid.privyloci.util.Logger

const val TAG = "ServiceStarter"

fun stopPrivyForegroundService(context: Context, extraActions: List<Intent> = emptyList()) {
    val serviceIntent = Intent(context, PrivyForegroundService::class.java).apply {
        extraActions.forEach {
            putExtra(it.action, it)
        }
    }
    context.stopService(serviceIntent)
}

fun startPrivyForegroundService(context: Context, action: String? = null) {
    Logger.d(TAG, "Trying to Start foreground service")
    val serviceIntent = Intent(context, PrivyForegroundService::class.java).apply {
        if (action != null) {
            this.action = action
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            /* The common exception is:
            android.app.ForegroundServiceStartNotAllowedException: startForegroundService() not allowed due to mAllowStartForeground false: service me.itissid.privyloci/.service.PrivyForegroundService
            which is documented here: https://stackoverflow.com/q/70044393. The issue is that the foreground cannot be started from the background.
           N2S: might want to track/report this. Because it should ideally never happen.
             */
            Logger.e(TAG, "Error starting foreground service", e)
        }
    } else {
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}