package me.itissid.privyloci.datamodels

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// SubscriptionDao.kt
@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions" /* TODO: Get active and unexpired subscriptions*/)
    suspend fun getAllSubscriptions(): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)

    @Query("SELECT * FROM subscriptions WHERE subscriptionId = :id")
    suspend fun getSubscriptionById(id: Int): SubscriptionEntity?
}
