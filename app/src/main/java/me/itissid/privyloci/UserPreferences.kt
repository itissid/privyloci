package me.itissid.privyloci

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Per SO Answer: https://arc.net/l/quote/kebwdeqe
private val Context.privyLociDataStore by preferencesDataStore("user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = context.privyLociDataStore

    val wasFGPerrmissionRationaleDismissed = dataStore.data.map {
        it[fgPerrmissionRationaleDismissedKey] ?: false
    }
    val wasFGPersistentNotificationDismissed = dataStore.data.map {
        it[fgPersistentNotificationDismissedKey] ?: false
    }
    val wasReactivateFGRationaleDismissed = dataStore.data.map {
        it[reactivateFGRationaleDismissedKey] ?: false
    }

    val userPausedLocationCollection = dataStore.data.map {
        it[pausedLocationCollectionKey] ?: false
    }

    val userVisitedPermissionLauncher = dataStore.data.map {
        it[visitedPermissionLauncher] ?: false
    }


    suspend fun setFGPermissionRationaleDismissed(dismissed: Boolean) =
        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[fgPerrmissionRationaleDismissedKey] = dismissed
            }
        }

    suspend fun setFGPersistentNotificationDismissed(dismissed: Boolean) =
        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[fgPersistentNotificationDismissedKey] = dismissed
            }
        }

    suspend fun setReactivateFGRationaleDismissed(dismissed: Boolean) =
        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[reactivateFGRationaleDismissedKey] = dismissed
            }
        }

    suspend fun setUserVisitedPermissionLauncher(dismissed: Boolean) =
        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[visitedPermissionLauncher] = dismissed
            }
        }

    suspend fun setUserPausedLocationCollection(paused: Boolean) =
        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[pausedLocationCollectionKey] = paused
            }
        }

    private companion object {
        val fgPerrmissionRationaleDismissedKey =
            booleanPreferencesKey("fg_permission_rationale_dismissed")
        val fgPersistentNotificationDismissedKey =
            booleanPreferencesKey("fg_persistent_notification_dismissed")
        val reactivateFGRationaleDismissedKey =
            booleanPreferencesKey("reactivate_fg_rationale_dismissed")

        val visitedPermissionLauncher = booleanPreferencesKey("visited_permission_launcher")
        val pausedLocationCollectionKey = booleanPreferencesKey("paused_location_collection")
        // More keys for other sensors and permissions.

    }
}
