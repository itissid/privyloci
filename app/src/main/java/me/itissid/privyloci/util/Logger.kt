package me.itissid.privyloci.util

import android.util.Log

object Logger {
    private const val TAG = "PrivyLociLogger"

    fun d(className: String, message: String) {
            Log.d(TAG, "[$className] $message")
    }

    fun e(className: String, message: String, throwable: Throwable? = null) {
            Log.e(TAG, "[$className] $message", throwable)
    }

    fun i(className: String, message: String) {
            Log.i(TAG, "[$className] $message")
    }

    fun w(className: String, message: String) {
            Log.w(TAG, "[$className] $message")
    }
}
