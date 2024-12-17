package me.itissid.privyloci.util

import android.util.Log

const val TAG = "PrivyLociLogger"
object Logger {

    inline fun v(className: String, message: String) {
        Log.v(TAG, "[$className] $message")
    }

    inline fun d(className: String, message: String) {
            Log.d(TAG, "[$className] $message")
    }

    inline fun e(className: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$className] $message", throwable)
        } else {
            Log.e(TAG, "[$className] $message (throwable is null no stack trace :()")
        }
    }

    inline fun i(className: String, message: String) {
            Log.i(TAG, "[$className] $message")
    }

    inline fun w(className: String, message: String) {
            Log.w(TAG, "[$className] $message")
    }
}
