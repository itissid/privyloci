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

    companion object {
        fun <T> DataStore<Preferences>.readAsResult(
            key: Preferences.Key<T>,
            defaultValue: T
        ): Flow<Result<T>> {
            return this.data
                .map { preferences ->
                    Result.success(
                        preferences[key]
                            ?: defaultValue
                    ).also {
                        Logger.v("UserPreferences", "Read $key: ${it.getOrNull()}")
                    }
                }
                .catch { exception ->
                    Logger.e(
                        "UserPreferences",
                        "Error reading preferences: Message: `${exception.message}`",
                        exception
                    )
                    emit(Result.failure(exception))
                }
        }

        suspend fun <T> DataStore<Preferences>.editKey(
            key: Preferences.Key<T>,
            value: T
        ) {
            runCatching {
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

        val visitedPermissionLauncher = booleanPreferencesKey("visited_permission_launcher")
        val pausedLocationCollectionKey = booleanPreferencesKey("paused_location_collection")

    }

    suspend fun setUserVisitedPermissionLauncher(dismissed: Boolean) {
        dataStore.editKey(
            visitedPermissionLauncher, dismissed
        )
    }

    suspend fun setUserPausedLocationCollection(paused: Boolean) {
        dataStore.editKey(
            pausedLocationCollectionKey, paused
        )
    }

}