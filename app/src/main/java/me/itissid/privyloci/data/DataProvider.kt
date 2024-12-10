package me.itissid.privyloci.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.itissid.privyloci.datamodels.*
import java.lang.reflect.Type

// A temporary data provider for testing the application.
object DataProvider {

    private val gson = Gson()

    // Your provided JSON snippet as a raw string
    private val jsonString = """
        {
            "places": [
                {
                    "id": 1,
                    "name": "Home",
                    "type": "PLACE",
                    "metadata": "{ \"lat\": \"12.9716\", \"long\": \"77.5946\" }",
                    "createdAt": 1693048323,
                    "isActive": true
                },
                {
                    "id": 2,
                    "name": "Grocery Store",
                    "type": "PLACE",
                    "metadata": "{ \"lat\": \"12.9656\", \"long\": \"77.5889\" }",
                    "createdAt": 1693048323,
                    "isActive": true
                }
            ],
            "assets": [
                {
                    "id": 3,
                    "name": "Headphones",
                    "type": "ASSET_BLE_HEADPHONES",
                    "metadata": "{ \"ble_id\": \"ABC123\" }",
                    "createdAt": 1693048323,
                    "isActive": true
                },
                {
                    "id": 4,
                    "name": "Car",
                    "type": "ASSET_BLE_CAR",
                    "metadata": "{ \"ble_id\": \"XYZ789\" }",
                    "createdAt": 1693048323,
                    "isActive": true
                }
            ],
            "subscriptions": [
                {
                    "subscriptionId": 1,
                    "eventStateType": "GeofenceEventState",
                    "type": "USER",
                    "placeTagId": 1,
                    "appInfo": "",
                    "createdAt": 1693048323,
                    "isActive": true,
                    "expirationDt": null,
                    "eventType": "GEOFENCE_ENTRY"
                },
                {
                    "subscriptionId": 2,
                    "type": "USER",
                    "placeTagId": 2,
                    "appInfo": "",
                    "createdAt": 1693048323,
                    "isActive": true,
                    "expirationDt": null,
                    "eventType": "GEOFENCE_ENTRY"
                },
                {
                    "subscriptionId": 3,
                    "type": "APP",
                    "placeTagId": 1,
                    "appInfo": "{ \"app_name\": \"Grocery Shopping App\", \"app_id\": \"com.example.app\" }",
                    "createdAt": 1693048323,
                    "isActive": true,
                    "expirationDt": null,
                    "eventType": "GEOFENCE_ENTRY"
                },
                {
                    "subscriptionId": 4,
                    "type": "USER",
                    "placeTagId": 3,
                    "appInfo": "",
                    "createdAt": 1693048323,
                    "isActive": true,
                    "expirationDt": null,
                    "eventType": "TRACK_BLE_ASSET_DISCONNECTED"
                }
            ],
            "eventStates": [
                {
                    "subscriptionId": 1,
                    "eventStateType": "GeofenceEventState",
                    "eventState": {
                        "subscriptionId": 1,
                        "type": "GEOFENCE_ENTRY",
                        "geofenceCenter": "{ \"lat\": \"12.9716\", \"long\": \"77.5946\" }",
                        "geofenceRadius": 50,
                        "state": {
                            "mostRecentState": "OUT",
                            "mostRecentStateChangeTs": 1693048323
                        }
                    }
                },
                {
                    "subscriptionId": 2,
                    "eventStateType": "GeofenceEventState",
                    "eventState": {
                        "subscriptionId": 2,
                        "type": "GEOFENCE_ENTRY",
                        "geofenceCenter": "{ \"lat\": \"12.9656\", \"long\": \"77.5889\" }",
                        "geofenceRadius": 50,
                        "state": {
                            "mostRecentState": "OUT",
                            "mostRecentStateChangeTs": 1693048323
                        }
                    }
                },
                {
                    "subscriptionId": 3,
                    "eventStateType": "GeofenceEventState",
                    "eventState": {
                        "subscriptionId": 3,
                        "type": "GEOFENCE_ENTRY",
                        "geofenceCenter": "{ \"lat\": \"12.9716\", \"long\": \"77.5946\" }",
                        "geofenceRadius": 50,
                        "state": {
                            "mostRecentState": "OUT",
                            "mostRecentStateChangeTs": 1693048323
                        }
                    }
                },
                {
                    "subscriptionId": 4,
                    "eventStateType": "AssetTrackEventState",
                    "eventState": {
                        "subscriptionId": 4,
                        "type": "ASSET_TRACK_CONNECT_DISCONNECT",
                        "assetId": "ABC123",
                        "assetName": "Headphones",
                        "assetType": "BLE",
                        "assetMetadata": "{ \"description\": \"Headphones disconnected from BLE\" }",
                        "state": "DISCONNECTED"
                    }
                },
                {
                    "subscriptionId": 5,
                    "eventStateType": "AssetTrackEventState",
                    "eventState": {
                        "subscriptionId": 5,
                        "type": "ASSET_TRACK_CONNECT_DISCONNECT",
                        "assetId": "XYZ789",
                        "assetName": "Car",
                        "assetType": "BLE",
                        "assetMetadata": "{ \"description\": \"Car BLE disconnected\" }",
                        "state": "DISCONNECTED"
                    }
                }
            ]
        }
    """.trimIndent()

    /**
     * Provide data by parsing the JSON string.
     */
    fun getData(): Triple<List<PlaceTag>, List<PlaceTag>, List<Subscription>> {
        // Parse the JSON string into a Map
        val dataMap: Map<String, Any> = gson.fromJson(jsonString, Map::class.java) as Map<String, Any>

        // Parse places
        val placesJson = gson.toJson(dataMap["places"])
        val placeTagListType: Type = object : TypeToken<List<PlaceTagEntity>>() {}.type
        val _placesList: List<PlaceTagEntity> =
            gson.fromJson(placesJson, placeTagListType)
        val placesList: List<PlaceTag> = _placesList.map { it.toDomain() }

        // Parse assets
        val assetsJson = gson.toJson(dataMap["assets"])
        val _assetsList: List<PlaceTagEntity> = gson.fromJson(assetsJson, placeTagListType)
        val assetsList: List<PlaceTag> = _assetsList.map { it.toDomain() }


        // Parse subscriptions
        val subscriptionsJson = gson.toJson(dataMap["subscriptions"])
        val subscriptionListType: Type = object : TypeToken<List<Subscription>>() {}.type
        val subscriptionsList: List<Subscription> = gson.fromJson(subscriptionsJson, subscriptionListType)
        // Add place names to each of the Subscription objects from placeList lookup
        subscriptionsList.forEach {
            it.placeTagName = (assetsList+placesList).find { place -> place.id == it.placeTagId }?.name.toString()
        }

        return Triple(placesList, assetsList, subscriptionsList)
    }

    /**
     * Process subscriptions into AppContainers.
     */
    fun processAppContainers(subscriptions: List<Subscription>): List<AppContainer> {
        // Group subscriptions by app
        val appSubscriptions = subscriptions.filter { it.type == SubscriptionType.APP }
        val groupedByApp = appSubscriptions.groupBy { it.getAppName() }

        // Create AppContainers
        return groupedByApp.map { (appName, subs) ->
            AppContainer(
                name = appName,
                uniquePlaces = subs.map { it.placeTagId }.distinct().size,
                uniqueSubscriptions = subs.size,
                subscriptions = subs,
                isExpanded = false
            )
        }
    }

    fun getEventStates(): Map<Int, EventState> {
        // Used internally with algorithms to maintain state.
        val dataMap: Map<*, *> = gson.fromJson(jsonString, Map::class.java)
        val eventStatesJson = gson.toJson(dataMap["eventStates"])

        val eventStatesListType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val eventStatesList: List<Map<String, Any>> = gson.fromJson(eventStatesJson, eventStatesListType)

        val eventStatesMap = mutableMapOf<Int, EventState>()

        for (eventStateWrapper in eventStatesList) {
            val subscriptionId = (eventStateWrapper["subscriptionId"] as Double).toInt()
            val eventStateType = eventStateWrapper["eventStateType"] as String

            val eventStateJson = gson.toJson(eventStateWrapper["eventState"])
            val eventState: EventState = when (eventStateType) {
                "GeofenceEventState" -> gson.fromJson(eventStateJson, GeofenceEventState::class.java)
                "AssetTrackEventState" -> gson.fromJson(eventStateJson, AssetTrackEventState::class.java)
                else -> throw IllegalArgumentException("Unknown event state type: $eventStateType")
            }
            eventStatesMap[subscriptionId] = eventState
        }

        return eventStatesMap
    }
}
