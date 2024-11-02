package me.itissid.privyloci.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.itissid.privyloci.datamodels.SubscriptionDao
import me.itissid.privyloci.datamodels.Subscription

// AppDatabase.kt
@Database(entities = [Subscription::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
}
