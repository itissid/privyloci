package me.itissid.privyloci.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.datamodels.SubscriptionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DBModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext appContext: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "privy-loci-database"
        ).build()
    }

    @Provides
    fun provideSubscriptionDao(appDatabase: AppDatabase): SubscriptionDao {
        return appDatabase.subscriptionDao()
    }

    @Provides
    fun providePlaceTagDao(appDatabase: AppDatabase): PlaceTagDao {
        return appDatabase.placeTagDao()
    }

}