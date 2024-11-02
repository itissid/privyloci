package me.itissid.privyloci

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.SensorType
import me.itissid.privyloci.service.PrivyForegroundService

object SensorManager {
    private val locationMutableFlow = MutableSharedFlow<Location>(replay = 1)
    val locationFlow: SharedFlow<Location> = locationMutableFlow.asSharedFlow()
    private val activeSensors = mutableSetOf<SensorType>()

    private var isEmitting = false

    fun startLocationUpdates() {
        if (isEmitting) return
        isEmitting = true
        CoroutineScope(Dispatchers.Default).launch {
            // TODO: Replace by a location provider.
            val mockLocations = listOf(
                createLocation(12.9715, 77.5945), // Outside geofence
                createLocation(12.9716, 77.5946), // At geofence boundary
                createLocation(12.9717, 77.5947)  // Inside geofence
            )
            for (location in mockLocations) {
                locationMutableFlow.emit(location)
                delay(5000) // Wait 5 seconds before next update
            }
        }
    }

    fun stopLocationUpdates() {
        isEmitting = false
    }

    private fun createLocation(lat: Double, lon: Double): Location {
        return Location("mock").apply {
            latitude = lat
            longitude = lon
            time = System.currentTimeMillis()
        }
    }

    fun shutdown() {
        // TODO("Not yet implemented")
    }

    fun initialize(privyForegroundService: PrivyForegroundService) {
        //
        //TODO("Not yet implemented")
    }


    fun updateActiveSensors(requiredSensors: Set<SensorType>) {
        val sensorsToStart = requiredSensors - activeSensors
        val sensorsToStop = activeSensors - requiredSensors

        sensorsToStart.forEach { startSensor(it) }
        sensorsToStop.forEach { stopSensor(it) }

        activeSensors.clear()
        activeSensors.addAll(requiredSensors)
    }

    private fun startSensor(sensorType: SensorType) {
        when (sensorType) {
            SensorType.LOCATION -> startLocationUpdates()
            // Start other sensors
            SensorType.BLE -> TODO()
            SensorType.WIFI -> TODO()
        }
    }

    private fun stopSensor(sensorType: SensorType) {
        when (sensorType) {
            SensorType.LOCATION -> stopLocationUpdates()
            // Stop other sensors
            SensorType.BLE -> TODO()
            SensorType.WIFI -> TODO()
        }
    }
}
