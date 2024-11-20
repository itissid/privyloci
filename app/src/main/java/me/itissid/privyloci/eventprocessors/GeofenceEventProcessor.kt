package me.itissid.privyloci.eventprocessors

import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.itissid.privyloci.R
import me.itissid.privyloci.SensorManager
import me.itissid.privyloci.datamodels.GeofenceStateType
import me.itissid.privyloci.datamodels.LatLng
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.requiredSensors
import me.itissid.privyloci.sensors.GoogleFusedLocationSensor
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class GeofenceEventProcessor(
    private val subscription: Subscription,
    private val context: Context,
    private val sensorManager: SensorManager
) : EventProcessor {
    companion object {
        const val CHANNEL_ID = "PrivyLogiGeoChannelNofitication"
    }

    private var job: Job? = null

    // TODO: Replace these from Subscription data after testing.
    private var geofenceState = GeofenceStateType.OUT
    private val geofenceCenter = LatLng(12.9716, 77.5946)
    private val geofenceRadius = 50 // in meters
    private val debounceTime = 10000L // 10 seconds
    private var lastStateChangeTime = System.currentTimeMillis()

    override fun startProcessing() {
        Log.d(
            "GeofenceEventProcessor",
            "Starting geofence event processor for Subscription ID: ${subscription.subscriptionId}"
        )
        assert(
            subscription.requiredSensors().size == 1
        ) { "Geofence event processor requires exactly one sensor" }

        val sensor = sensorManager.sensorFor(
            subscription.requiredSensors().first()
        ) as GoogleFusedLocationSensor
        job = sensor.locationFlow.onEach { location ->
                processLocation(location)
            }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    override fun stopProcessing() {
        Log.d(
            "GeofenceEventProcessor",
            "Stopping geofence event processor for Subscription ID: ${subscription.subscriptionId}"
        )
        job?.cancel()
    }

    private fun processLocation(location: Location) {
        // TODO: Reverse the location check with debounce time and ENTER/DWELL/EXIT logic.
        Log.d(
            "GeofenceEventProcessor",
            "Processing location ${
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(location.time),
                    ZoneId.of("UTC")
                )
            }"
        )

        val distance = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            geofenceCenter.latitude, geofenceCenter.longitude,
            distance
        )
        val isInside = distance[0] <= geofenceRadius

        val currentTime = System.currentTimeMillis()
        if (geofenceState == GeofenceStateType.IN && !isInside) {
            if (currentTime - lastStateChangeTime > debounceTime) {
                geofenceState = GeofenceStateType.OUT
                lastStateChangeTime = currentTime
                sendNotification("Exited geofence: ${subscription.placeTagName}")
            }
        } else if (geofenceState == GeofenceStateType.OUT && isInside) {
            if (currentTime - lastStateChangeTime > debounceTime) {
                geofenceState = GeofenceStateType.IN
                lastStateChangeTime = currentTime
                sendNotification("Entered geofence: ${subscription.placeTagName}")
            }
        }
    }

    private fun sendNotification(message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random.nextInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Geofence Event")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        notificationManager.notify(notificationId, notification)
    }
}
