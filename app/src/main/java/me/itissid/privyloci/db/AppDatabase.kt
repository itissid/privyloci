package me.itissid.privyloci.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.itissid.privyloci.datamodels.DeviceCapabilitiesDao
import me.itissid.privyloci.datamodels.DeviceCapabilityEntity
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.datamodels.PlaceTagEntity
import me.itissid.privyloci.datamodels.SubscriptionDao
import me.itissid.privyloci.datamodels.Subscription

// AppDatabase.kt
@Database(
    entities = [Subscription::class, PlaceTagEntity::class, DeviceCapabilityEntity::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeTagDao(): PlaceTagDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun deviceCapabilityDao(): DeviceCapabilitiesDao
}
