package me.itissid.privyloci.util

import android.util.Log

object Logger {
    private const val TAG = "PrivyLociLogger"

    fun v(className: String, message: String) {
        Log.v(TAG, "[$className] $message")
    }
    fun d(className: String, message: String) {
            Log.d(TAG, "[$className] $message")
    }

    fun e(className: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$className] $message", throwable)
        } else {
            Log.e(TAG, "[$className] $message (throwable is null no stack trace :()")
        }
    }

    fun i(className: String, message: String) {
            Log.i(TAG, "[$className] $message")
    }

    fun w(className: String, message: String) {
            Log.w(TAG, "[$className] $message")
    }
}
