package me.itissid.privyloci.datamodels

import android.os.Parcelable
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class PlaceTag(
    val id: Int,
    val name: String,
    val type: PlaceTagType, // enum {PLACE, ASSET}
    val metadata: String, // Encrypted JSON string
    val createdAt: Long,
    val isActive: Boolean
) : Parcelable

enum class PlaceTagType {
    PLACE, ASSET
}

@Serializable
@Parcelize
data class Subscription(
    val subscriptionId: Int,
    val type: SubscriptionType, // enum {APP, USER}
    val placeTagId: String,
    val appInfo: String, // JSON string with app details if type is APP
    val createdAt: Long,
    val isActive: Boolean,
    val expirationDt: Long?,
    val eventType: EventType

) : Parcelable {
    // Custom property to format the timestamp into a date string
    val formattedDate: String
        get() {
            val date = Date(createdAt)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.timeZone = TimeZone.getDefault() // Use the user's current timezone
            return format.format(date)
        }
    // Function to check if the subscription is valid
    fun isValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return isActive && (expirationDt == null || expirationDt > currentTime)
    }

    fun isTypeLocation(): Boolean {
        return eventType == EventType.GEOFENCE_ENTRY || eventType  == EventType.GEOFENCE_EXIT
    }
    fun getAppName(): String {
        return if (type == SubscriptionType.APP) {
            val gson = Gson()
            val appInfoMap = gson.fromJson<Map<String, String>>(appInfo, Map::class.java)
            appInfoMap["app_name"] ?: "Unknown App"
        } else {
            "User Subscription"
        }
    }
}

enum class SubscriptionType {
    APP, USER
}

enum class EventType {
    GEOFENCE_ENTRY, // TODO(Sid):
    GEOFENCE_EXIT,
    TRACK_BLE_ASSET_DISCONNECTED,
    TRACK_BLE_ASSET_NEARBY,
    QIBLA_DIRECTION_PRAYER_TIME,
    DISPLAY_PINS_MAP_TILE
}

@Serializable
sealed class EventState {
    abstract val subscriptionId: Int
}

data class GeofenceEventState(
    override val subscriptionId: Int,
    val type: GeofenceEventType, // enum {GEOFENCE_ENTRY, GEOFENCE_EXIT}
    val geofenceCenter: String, // Lat/Long as a string, (TODO(Sid): decide if this is encrypted?)
    val geofenceRadius: Int,
    val state: GeofenceState
): EventState()

data class GeofenceState(
    val mostRecentState: GeofenceStateType, // enum {IN, OUT}
    val mostRecentStateChangeTs: Long // Time since epoch in milliseconds.
)

enum class GeofenceStateType {
    IN, OUT
}

enum class GeofenceEventType {
    GEOFENCE_ENTRY, GEOFENCE_EXIT
}


data class AssetTrackEventState(
   override val subscriptionId: Int,
    val type: AssetTrackType, // enum {ASSET_TRACK_CONNECT_DISCONNECT, ASSET_NEARBY}
    val assetId: String,
    val assetName: String,
    val assetType: AssetType, // enum {BLE, WIFI, IR}
    val assetMetadata: String, // Encrypted JSON string
    val state: String // Encrypted state
): EventState()

enum class AssetTrackType {
    ASSET_TRACK_CONNECT_DISCONNECT, ASSET_NEARBY
}

enum class AssetType {
    BLE, WIFI, IR
}

/**
 * Intermediate Data classes for the views.
 */
@Parcelize
@Serializable
data class AppContainer(
    val name: String,
    val uniquePlaces: Int,
    val uniqueSubscriptions: Int,
    val subscriptions:  @RawValue List<Subscription>,
    var isExpanded: Boolean = false
) : Parcelable