package me.itissid.privyloci

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.itissid.privyloci.SensorManager.updateActiveSensors
import me.itissid.privyloci.datamodels.EventType
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionDao
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.datamodels.requiredSensors
import me.itissid.privyloci.db.AppDatabase
import me.itissid.privyloci.eventprocessors.EventProcessor
import me.itissid.privyloci.eventprocessors.GeofenceEventProcessor

object SubscriptionManager {
    private val activeSubscriptions = mutableListOf<Subscription>()
    private val eventProcessors = mutableMapOf<Int, EventProcessor>()
    private lateinit var db: AppDatabase
    private lateinit var subscriptionDao: SubscriptionDao

    suspend fun initialize(context: Context) {
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "privy-loci-database"
        ).build()
        subscriptionDao = db.subscriptionDao()

        // Load subscriptions from database
        CoroutineScope(Dispatchers.IO).launch {
            //  val subscriptions = subscriptionDao.getAllSubscriptions()
            // For testing, add a mock subscription
            val subscriptions = listOf(
                Subscription(
                    subscriptionId = 1,
                    type = SubscriptionType.USER,
                    placeTagId = 1,
                    placeTagName = "Test Place",
                    appInfo = "",
                    createdAt = System.currentTimeMillis(),
                    isActive = true,
                    expirationDt = null,
                    eventType = EventType.GEOFENCE_ENTRY
                )
            )
            subscriptions.forEach {
                subscriptionDao.insertSubscription(it)
            }
            // Create subscriptions and start the processing.
            subscriptions.forEach {
                addSubscription(it, context)
            }
        }
    }

    private fun addSubscription(subscription: Subscription, context: Context) {
        activeSubscriptions.add(subscription)
        val processor = GeofenceEventProcessor(subscription, context)
        eventProcessors[subscription.subscriptionId] = processor
        processor.startProcessing()

        manageSensors()
    }

    private fun createEventProcessor(subscription: Subscription, context: Context): EventProcessor {
        return when (subscription.eventType) {
            EventType.GEOFENCE_ENTRY,
            EventType.GEOFENCE_EXIT -> GeofenceEventProcessor(subscription, context)
            // Add other event types as needed
            else -> throw IllegalArgumentException("Unsupported event type")
        }
    }

    private fun manageSensors() {
        val requiredSensors = activeSubscriptions.flatMap { it.requiredSensors() }.toSet()
        updateActiveSensors(requiredSensors)
    }

    suspend fun removeSubscription(subscriptionId: Int) {
        activeSubscriptions.removeAll { it.subscriptionId == subscriptionId }
        eventProcessors[subscriptionId]?.stopProcessing()
        eventProcessors.remove(subscriptionId)

        // Stop SensorManager if no subscriptions remain
        if (activeSubscriptions.isEmpty()) {
            SensorManager.stopLocationUpdates()
        }
        val subscriptionEntity = subscriptionDao.getSubscriptionById(subscriptionId)
        if (subscriptionEntity != null) {
            subscriptionDao.deleteSubscription(subscriptionEntity)
        }
        manageSensors()
    }

    fun shutdown() {
        TODO("Not yet implemented")
    }
}

