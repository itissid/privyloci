package me.itissid.privyloci.datamodels

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// SubscriptionDao.kt
@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions" /* TODO: Get active and unexpired subscriptions*/)
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("SELECT * FROM subscriptions WHERE subscriptionId = :id")
    suspend fun getSubscriptionById(id: Int): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<Subscription>)

}


@Dao
interface PlaceTagDao {
    @Query("SELECT * FROM place_tags")
    fun getAllPlaceTags(): Flow<List<PlaceTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaceTags(placeTags: List<PlaceTag>)
}