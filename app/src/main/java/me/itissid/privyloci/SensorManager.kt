package me.itissid.privyloci

import me.itissid.privyloci.datamodels.SensorType
import me.itissid.privyloci.sensors.GoogleFusedLocationSensor
import me.itissid.privyloci.sensors.ISensor
import me.itissid.privyloci.service.PrivyForegroundService
import me.itissid.privyloci.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorManager @Inject constructor(
    private val googleLocationSensor: GoogleFusedLocationSensor
    // Add Other sensors as we implement them
) {
    //    private val locationMutableFlow = MutableSharedFlow<Location>(replay = 20)
//    val locationFlow: SharedFlow<Location> = locationMutableFlow.asSharedFlow()
    private val activeSensors = mutableSetOf<SensorType>()

//    private var isEmitting = false

//    fun startLocationUpdates() {
//        if (isEmitting) return
//        isEmitting = true
//        CoroutineScope(Dispatchers.Default).launch {
//            // TODO: Replace by a location provider.
////            val mockLocations = listOf(
////                createLocation(12.9715, 77.5945), // Outside geofence
////                createLocation(12.9716, 77.5946), // At geofence boundary
////                createLocation(12.9717, 77.5947)  // Inside geofence
////            )
////            for (location in mockLocations) {
////                locationMutableFlow.emit(location)
////                delay(5000) // Wait 5 seconds before next update
////            }
//        }
//    }

//    fun stopLocationUpdates() {
//        isEmitting = false
//    }

//    private fun createLocation(lat: Double, lon: Double): Location {
//        return Location("mock").apply {
//            latitude = lat
//            longitude = lon
//            time = System.currentTimeMillis()
//        }
//    }

    fun shutdown() {
        updateActiveSensors(emptySet())
    }

    fun initialize(privyForegroundService: PrivyForegroundService) {
        //TODO("Not yet implemented")
    }

    fun updateActiveSensors(requiredSensors: Set<SensorType>) {
        this::class.qualifiedName?.let {
            Logger.d(
                it,
                "Updating active sensors to: $requiredSensors"
            )
        }
        val sensorsToStart = requiredSensors - activeSensors
        val sensorsToStop = activeSensors - requiredSensors

        sensorsToStart.forEach { startSensors(it) }
        sensorsToStop.forEach { stopSensors(it) }

        activeSensors.clear()
        activeSensors.addAll(requiredSensors)
    }

    private fun startSensors(sensorType: SensorType) {
        this::class.qualifiedName?.let { Logger.d(it, "Starting sensor: $sensorType") }
        sensorFor(sensorType).start()
    }

    private fun stopSensors(sensorType: SensorType) {
        this::class.qualifiedName?.let { Logger.d(it, "Stopping sensor: $sensorType") }
        sensorFor(sensorType).stop()
    }

    fun sensorFor(sensor: SensorType): ISensor {
        return when (sensor) {
            SensorType.LOCATION -> googleLocationSensor
            SensorType.BLE -> TODO()
            SensorType.WIFI -> TODO()
        }
    }
}

