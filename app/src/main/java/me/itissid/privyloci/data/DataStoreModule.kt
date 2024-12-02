package me.itissid.privyloci.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    // Add DataStore related bindings here
//    private val Context.dataStore by preferencesDataStore("settings")

//    @Provides
//    @Singleton
//    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> =
//        PreferenceDataStoreFactory.create(
//            produceFIle = { appContext.preferencesDataStoreFile("user_preferences") }
//        )
//
}