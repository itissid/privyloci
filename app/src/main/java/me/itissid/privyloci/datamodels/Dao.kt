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

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun subscriptionExists(): Int

    @Query("DELETE FROM subscriptions")
    suspend fun deleteSubscriptions()

}


@Dao
interface PlaceTagDao {
    @Query("SELECT * FROM place_tags")
    fun getAllPlaceTags(): Flow<List<PlaceTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaceTags(placeTags: List<PlaceTagEntity>)

    @Query("SELECT count(*) FROM place_tags")
    suspend fun placeTagExists(): Int

    @Query("DELETE FROM place_tags")
    suspend fun deletePlaces()
}