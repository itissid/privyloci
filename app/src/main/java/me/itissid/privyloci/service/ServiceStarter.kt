package me.itissid.privyloci.service

import android.content.Context
import android.content.Intent
import me.itissid.privyloci.util.Logger

fun startMyForegroundService(context: Context) {

    val serviceIntent = Intent(context, PrivyForegroundService::class.java)
    Logger.w("ServiceStarter", "Starting Privy Loc Foreground Service")
    context.startForegroundService(serviceIntent)
}

fun stopMyForegroundService(context: Context) {
    val serviceIntent = Intent(context, PrivyForegroundService::class.java)
    context.stopService(serviceIntent)
}