package me.itissid.privyloci.kvrepository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.itissid.privyloci.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

// Per SO Answer: https://arc.net/l/quote/kebwdeqe
private val Context.privyLociUserPreferencesDataStore by preferencesDataStore("user_preferences")

private const val TAG = "UserPreference"
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.privyLociUserPreferencesDataStore
    private val datastoreLoggingTag: String = "UserPreferences"

    val userPausedLocationCollection =
        dataStore.readWrapper(pausedLocationCollectionKey, false, datastoreLoggingTag)

    val userVisitedPermissionLauncher =
        dataStore.readWrapper(visitedPermissionLauncher, false, datastoreLoggingTag)

    val userVisitedBlePermissionLauncher =
        dataStore.readWrapper(visitedBlePermissionLauncher, false, datastoreLoggingTag)


    companion object {
        fun <T> DataStore<Preferences>.readWrapper(
            key: Preferences.Key<T>,
            defaultValue: T,
            loggerTag: String
        ): Flow<T> {
            return this.data
                .map { preferences ->
                    val result = preferences[key] ?: defaultValue
                    Logger.v(loggerTag, "Read $key: $result")
                    result
                }
                .catch { exception ->
                    Logger.e(
                        loggerTag,
                        "Error reading preferences: Message: `${exception.message}`",
                        exception
                    )
                    emit(defaultValue)
                }
        }

        suspend fun <T> DataStore<Preferences>.editKey(
            key: Preferences.Key<T>,
            value: T,
            loggerTag: String
        ): Result<Preferences> {
            return runCatching {
                this.edit { preferences ->
                    Logger.v(loggerTag, "Editing $key: $value")
                    preferences[key] = value
                }
            }.onFailure { exception ->
                Logger.e(
                    loggerTag,
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
            visitedPermissionLauncher, dismissed, TAG
        )
    }

    suspend fun setUserPausedLocationCollection(paused: Boolean): Result<Preferences> {
        return dataStore.editKey(
            pausedLocationCollectionKey, paused, TAG
        )
    }

    // BT Related pregs
    suspend fun setUserVisitedBlePermissionLauncher(visited: Boolean): Result<Preferences> {
        return dataStore.editKey(
            visitedBlePermissionLauncher, visited, TAG
        )
    }

}