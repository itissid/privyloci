package me.itissid.privyloci.datamodels

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PlaceTag(
    val id: Int,
    val name: String,
    val type: PlaceTagType, // enum {PLACE, ASSET}
    val metadata: String, // Encrypted JSON string
    val createdAt: Long,
    val isActive: Boolean
)

enum class PlaceTagType {
    PLACE, ASSET
}

data class Subscription(
    val subscriptionId: Int,
    val type: SubscriptionType, // enum {APP, USER}
    val placeTagId: String,
    val appInfo: String, // JSON string with app details if type is APP
    val createdAt: Long,
    val isActive: Boolean,
    val expirationDt: Long?,
    val event: Event
) {
    // Custom property to format the timestamp into a date string
    val formattedDate: String
        get() {
            val date = Date(createdAt)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.timeZone = TimeZone.getDefault() // Use the user's current timezone
            return format.format(date)
        }
}

data class Event(
    val type: EventType,
    val metadata: String?        // Nullable since not all events use this
)

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

data class GeofenceSubscription(
    val subscriptionId: Int,
    val type: GeofenceEventType, // enum {GEOFENCE_ENTRY, GEOFENCE_EXIT}
    val geofenceCenter: String, // Lat/Long as a string
    val geofenceRadius: Int,
    val state: GeofenceState
)

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


data class AssetTrack(
    val subscriptionId: Int,
    val type: AssetTrackType, // enum {ASSET_TRACK_CONNECT_DISCONNECT, ASSET_NEARBY}
    val assetId: String,
    val assetName: String,
    val assetType: AssetType, // enum {BLE, WIFI, IR}
    val assetMetadata: String, // Encrypted JSON string
    val state: String // Encrypted state
)

enum class AssetTrackType {
    ASSET_TRACK_CONNECT_DISCONNECT, ASSET_NEARBY
}

enum class AssetType {
    BLE, WIFI, IR
}

/**
 * Intermediate Data classes for the views.
 */
data class AppContainer(
    val name: String,
    val uniquePlaces: Int,
    val uniqueSubscriptions: Int,
    val subscriptions: List<Subscription>,
    var isExpanded: Boolean = false
)