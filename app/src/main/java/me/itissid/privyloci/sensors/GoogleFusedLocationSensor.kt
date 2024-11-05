package me.itissid.privyloci.sensors

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** All the sensors **/
@Singleton
class GoogleFusedLocationSensor @Inject constructor(
    private val fusionLocationProviderClient: FusedLocationProviderClient
) :
    ISensor {
    private val locationMutableFlow: MutableSharedFlow<Location> = MutableSharedFlow(replay = 1)
    val locationFlow: SharedFlow<Location> = locationMutableFlow.asSharedFlow()

    private var isEmitting = false

    @SuppressLint("MissingPermission")
    override fun start() {
        if (isEmitting) return
        isEmitting = true
        CoroutineScope(Dispatchers.Default).launch {
            fusionLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun stop() {
        fusionLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private val locationRequest = LocationRequest.Builder(
        10000L // 10 seconds
    ).setMinUpdateIntervalMillis(5000L).setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
        .build() // 5 seconds

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                locationMutableFlow.tryEmit(location)
            }
        }
    }
}