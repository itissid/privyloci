package me.itissid.privyloci.kvrepository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.itissid.privyloci.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

import androidx.datastore.preferences.preferencesDataStore

// Per SO Answer: https://arc.net/l/quote/kebwdeqe
private val Context.privyLociDataStore by preferencesDataStore("user_preferences")


@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.privyLociDataStore


    val userPausedLocationCollection = dataStore.readAsResult(pausedLocationCollectionKey, false)

    val userVisitedPermissionLauncher = dataStore.readAsResult(visitedPermissionLauncher, false)

    val userVisitedBlePermissionLauncher =
        dataStore.readAsResult(visitedBlePermissionLauncher, false)

    companion object {
        fun <T> DataStore<Preferences>.readAsResult(
            key: Preferences.Key<T>,
            defaultValue: T
        ): Flow<T> {
            return this.data
                .map { preferences ->
                    val result = preferences[key] ?: defaultValue
                    Logger.v("UserPreferences", "Read $key: $result")
                    result
                }
                .catch { exception ->
                    Logger.e(
                        "UserPreferences",
                        "Error reading preferences: Message: `${exception.message}`",
                        exception
                    )
                    emit(defaultValue)
                }
        }

        suspend fun <T> DataStore<Preferences>.editKey(
            key: Preferences.Key<T>,
            value: T
        ): Result<Preferences> {
            return runCatching {
                this.edit { preferences ->
                    Logger.v("UserPreferences", "Editing $key: $value")
                    preferences[key] = value
                }
            }.onFailure { exception ->
                Logger.e(
                    "UserPreferences",
                    "Error editing preferences for $key for preference store, Message: `${exception.message}`",
                    exception,

                    )
            }
        }

        // Do not change these names, they might cause composables already in production to break
        val visitedPermissionLauncher = booleanPreferencesKey("visited_permission_launcher")
        val pausedLocationCollectionKey = booleanPreferencesKey("paused_location_collection")
        val visitedBlePermissionLauncher =
            booleanPreferencesKey("visited_ble_permission_launcher")

    }

    suspend fun setUserVisitedPermissionLauncher(dismissed: Boolean): Result<Preferences> {
        return dataStore.editKey(
            visitedPermissionLauncher, dismissed
        )
    }

    suspend fun setUserPausedLocationCollection(paused: Boolean): Result<Preferences> {
        return dataStore.editKey(
            pausedLocationCollectionKey, paused
        )
    }

    // BT Related pregs
    suspend fun setUserVisitedBlePermissionLauncher(visited: Boolean): Result<Preferences> {
        return dataStore.editKey(
            visitedBlePermissionLauncher, visited
        )
    }

}