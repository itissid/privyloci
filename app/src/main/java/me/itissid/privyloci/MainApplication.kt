package me.itissid.privyloci

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.itissid.privyloci.data.DataProvider
import me.itissid.privyloci.datamodels.toEntity
import me.itissid.privyloci.db.AppDatabase
import me.itissid.privyloci.eventprocessors.GeofenceEventProcessor
import me.itissid.privyloci.service.PrivyForegroundService
import me.itissid.privyloci.util.Logger

@HiltAndroidApp
class MainApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        initializeDatabase()
        createNotificationChannel()
    }

    private fun initializeDatabase() {
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "privy-loci-database"
        )
            .addCallback(DatabaseCallback())
            .fallbackToDestructiveMigration()
            .build()
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            //TODO:Testing code for hardcoded subscriptions.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Logger.d(this::class.toString(), "Populating database")
                    recreateDatabase()
                } catch (e: Exception) {
                    Logger.e(this::class.toString(), "Error populating database", e)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Logger.d(this::class.toString(), "Database opened")
            CoroutineScope(Dispatchers.IO).launch {
                Logger.d(this::class.toString(), "Overwriting database")
                recreateDatabase()
            }
        }

        private suspend fun recreateDatabase() {
            val placeTagDao = database.placeTagDao()
            val subscriptionDao = database.subscriptionDao()

            // Get data from DataProvider
            val (placesList, assetsList, subscriptionsList) = DataProvider.getData()
            val places = placesList + assetsList
            Log.w("MainApplication", "Populating database with ${places.size} places")
            Log.w(
                "MainApplication",
                "Populating database with ${subscriptionsList.size} subscriptions"
            )
            // Testing code for subscriptions.
            assert(places.isNotEmpty())
            subscriptionsList.forEach {
                subscriptionDao.deleteSubscription(it)
            }
            // Insert data into the database
            placeTagDao.insertPlaceTags(places.map { it.toEntity() })
            subscriptionDao.insertSubscriptions(subscriptionsList)
        }
    }


    private fun createNotificationChannel() {
        Logger.d("MainApplication", "Creating Notification Channel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels =
                listOf(PrivyForegroundService.CHANNEL_ID, GeofenceEventProcessor.CHANNEL_ID)
            channels.forEach { channelName ->
                val channel = NotificationChannel(
                    channelName,
                    "PrivyLociForegroundServiceChannel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for PrivyLoci Foreground Service"
                }
                Logger.d("MainApplication", "Created Notification Channel")
                val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}