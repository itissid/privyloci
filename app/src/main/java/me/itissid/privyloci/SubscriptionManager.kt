package me.itissid.privyloci

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.EventType
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionDao
import me.itissid.privyloci.datamodels.requiredSensors
import me.itissid.privyloci.eventprocessors.EventProcessor
import me.itissid.privyloci.eventprocessors.GeofenceEventProcessor
import me.itissid.privyloci.eventprocessors.NoopEventProcessor
import me.itissid.privyloci.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    private val sensorManager: SensorManager,
    private val subscriptionDao: SubscriptionDao
) {
    private val activeSubscriptions = mutableListOf<Subscription>()
    private val eventProcessors = mutableMapOf<Int, EventProcessor>()

    suspend fun initialize(context: Context) {

        // Load subscriptions from database
        CoroutineScope(Dispatchers.IO).launch {
            //  val subscriptions = subscriptionDao.getAllSubscriptions()
            // For testing, add a mock subscription
            subscriptionDao.getAllSubscriptions().collect { subscriptions ->
                activeSubscriptions.forEach { subscription ->
                    eventProcessors[subscription.subscriptionId]?.stopProcessing()
                }
                activeSubscriptions.clear()
                eventProcessors.clear()

                // Start processors for new subscriptions
                activeSubscriptions.addAll(subscriptions)
                this::class.simpleName?.let {
                    Logger.d(
                        it,
                        "Active subscriptions processed by SubscriptionManager: ${activeSubscriptions.size}"
                    )
                }
                subscriptions.forEach { subscription ->
                    val processor = createEventProcessor(subscription, context)
                    processor.startProcessing()
                    eventProcessors[subscription.subscriptionId] = processor
                }
                this::class.simpleName?.let {
                    Logger.d(
                        it,
                        "calling manageSensors for ${activeSubscriptions.size} subscriptions"
                    )
                }
                manageSensors()
            }
        }
        // N2S: Leaving the sensor manager start code here for now. Not sure if this is the right place for it.
    }

    private fun createEventProcessor(subscription: Subscription, context: Context): EventProcessor {
        Logger.d("SubscriptionManager", "Creating event processor for subscription: $subscription")
        return when (subscription.eventType) {
            EventType.GEOFENCE_ENTRY,
            EventType.GEOFENCE_EXIT -> GeofenceEventProcessor(subscription, context, sensorManager)

            // Add other event types as needed
            EventType.TRACK_BLE_ASSET_DISCONNECTED -> {
                //TODO: implement me
                NoopEventProcessor()
            }

            EventType.TRACK_BLE_ASSET_NEARBY -> {
                //TODO: implement me
                NoopEventProcessor()
            }

            EventType.QIBLA_DIRECTION_PRAYER_TIME -> {
                //TODO: implement me
                NoopEventProcessor()
            }

            EventType.DISPLAY_PINS_MAP_TILE -> {
                //TODO: implement me
                NoopEventProcessor()
            }
        }
    }

    private fun manageSensors() {
        val requiredSensors = activeSubscriptions.flatMap { it.requiredSensors() }.toSet()
        sensorManager.updateActiveSensors(requiredSensors)
    }

    // TODO: Used by the UI to remove a subscription.
    suspend fun removeSubscription(subscriptionId: Int) {
        activeSubscriptions.removeAll { it.subscriptionId == subscriptionId }
        eventProcessors[subscriptionId]?.stopProcessing()
        eventProcessors.remove(subscriptionId)

        // Stop SensorManager if no subscriptions remain
        if (activeSubscriptions.isEmpty()) {
            Logger.d("SubscriptionManager", "No active subscriptions. Stopping SensorManager")
            sensorManager.updateActiveSensors(emptySet())
        }

        val subscriptionEntity = subscriptionDao.getSubscriptionById(subscriptionId)
        if (subscriptionEntity != null) {
            subscriptionDao.deleteSubscription(subscriptionEntity)
        }
        manageSensors()
    }

    fun shutdown() {
        Logger.d("SubscriptionManager", "Shutting down SubscriptionManager")
        activeSubscriptions.forEach { subscription ->
            eventProcessors[subscription.subscriptionId]?.stopProcessing()
        }
        activeSubscriptions.clear()
        eventProcessors.clear()
        sensorManager.shutdown()
    }
}

