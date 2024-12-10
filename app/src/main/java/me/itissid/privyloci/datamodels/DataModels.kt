package me.itissid.privyloci.datamodels

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
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
@Entity(tableName = "place_tags")
data class PlaceTagEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String, // enum {PLACE, ASSET}
    val metadata: String, // Encrypted JSON string
    val createdAt: Long,
    val isActive: Boolean
) : Parcelable

fun PlaceTagEntity.toDomain(): PlaceTag {
    val domainType = when (type) {
        "PLACE" -> PlaceTagType.PLACE
        "ASSET_BLE_HEADPHONES" -> PlaceTagType.ASSET.BLEHeadphones
        "ASSET_BLE_CAR" -> PlaceTagType.ASSET.BLECar
        else -> throw IllegalArgumentException("Unknown type: $type")
    }

    return PlaceTag(
        id = id,
        name = name,
        type = domainType,
        metadata = metadata,
        createdAt = createdAt,
        isActive = isActive
    )
}

sealed class PlaceTagType {
    object PLACE : PlaceTagType()

    // Group all asset-related types under ASSET.
    sealed class ASSET : PlaceTagType() {
        object BLEHeadphones : ASSET()
        object BLECar : ASSET()
        // Future devices as we support them
    }
}

fun String.toPlaceTagType(): PlaceTagType {
    return when (this) {
        "PLACE" -> PlaceTagType.PLACE
        "ASSET_BLE_HEADPHONES" -> PlaceTagType.ASSET.BLEHeadphones
        "ASSET_BLE_CAR" -> PlaceTagType.ASSET.BLECar
        else -> throw IllegalArgumentException("Unknown type: $this")
    }
}

data class PlaceTag(
    val id: Int,
    val name: String,
    val type: PlaceTagType,
    val metadata: String,
    val createdAt: Long,
    val isActive: Boolean
) {
    private fun parseMetadata(): MutableMap<String, Any> {
        return try {
            Gson().fromJson<Map<String, Any>>(metadata, Map::class.java).toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    // Retrieve the selected BLE device address if this PlaceTag is BLEHeadphones.
    fun getSelectedDeviceAddress(): String? {
        return when (type) {
            PlaceTagType.PLACE -> null
            is PlaceTagType.ASSET.BLECar, PlaceTagType.ASSET.BLEHeadphones -> {
                parseMetadata()["selected_ble_device"] as? String
            }
        }
    }

    // Return a copy of this PlaceTag with the updated device address in metadata.
    // This is only meaningful if the type is BLEHeadphones; otherwise, we do nothing special.
    fun withSelectedDeviceAddress(address: String?): PlaceTag {
        val map = parseMetadata()
        if (address == null) {
            map.remove("selected_ble_device")
        } else {
            map["selected_ble_device"] = address
        }
        val newMetadata = Gson().toJson(map)
        return copy(metadata = newMetadata)
    }
}

fun PlaceTagType.tagString(): String {
    return when (this) {
        is PlaceTagType.PLACE -> "Place"
        is PlaceTagType.ASSET.BLEHeadphones -> "Headphones"
        is PlaceTagType.ASSET.BLECar -> "Car"
    }
}

fun PlaceTag.toEntity(): PlaceTagEntity {
    val dbType = when (type) {
        PlaceTagType.PLACE -> "PLACE"
        is PlaceTagType.ASSET.BLEHeadphones -> "ASSET_BLE_HEADPHONES"
        is PlaceTagType.ASSET.BLECar -> "ASSET_BLE_CAR"
    }

    return PlaceTagEntity(
        id = id,
        name = name,
        type = dbType,
        metadata = metadata,
        createdAt = createdAt,
        isActive = isActive
    )
}

@Serializable
@Parcelize
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val subscriptionId: Int,
    val type: SubscriptionType, // enum {APP, USER}
    val placeTagId: Int,
    var placeTagName: String,
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

enum class SensorType {
    LOCATION,
    BLE,
    WIFI
    // Add other sensor types
}

fun Subscription.requiredSensors(): List<SensorType> {
    return when (eventType) {
        EventType.GEOFENCE_ENTRY,
        EventType.GEOFENCE_EXIT -> listOf(SensorType.LOCATION)

        EventType.TRACK_BLE_ASSET_NEARBY -> listOf(SensorType.BLE, SensorType.LOCATION)
        EventType.QIBLA_DIRECTION_PRAYER_TIME -> listOf(SensorType.LOCATION)
        // Define required sensors for other event types
        else -> emptyList()
    }
}

enum class SubscriptionType {
    APP, USER
}

enum class EventType {
    GEOFENCE_ENTRY,
    GEOFENCE_EXIT,
    TRACK_BLE_ASSET_DISCONNECTED,
    TRACK_BLE_ASSET_NEARBY,
    QIBLA_DIRECTION_PRAYER_TIME,
    DISPLAY_PINS_MAP_TILE
}

val EventType.displayName: String
    get() = when (this) {
        EventType.GEOFENCE_ENTRY -> "Entry Alert"
        EventType.GEOFENCE_EXIT -> "Exit Alert"
        EventType.TRACK_BLE_ASSET_DISCONNECTED -> "Location Tracked after Disconnection"
        EventType.TRACK_BLE_ASSET_NEARBY -> "Tracking when in range, but not connected"
        EventType.QIBLA_DIRECTION_PRAYER_TIME -> "Direction to Qibla"
        EventType.DISPLAY_PINS_MAP_TILE -> "Displaying Pins on Map"
        else -> "Unknown Event"
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

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

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