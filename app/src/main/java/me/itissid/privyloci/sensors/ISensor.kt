package me.itissid.privyloci.sensors

import android.annotation.SuppressLint

interface ISensor {
    @SuppressLint("MissingPermission")
    fun start()
    fun stop()
}