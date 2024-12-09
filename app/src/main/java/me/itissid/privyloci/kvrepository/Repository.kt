package me.itissid.privyloci.kvrepository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.itissid.privyloci.kvrepository.UserPreferences.Companion.editKey
import me.itissid.privyloci.kvrepository.UserPreferences.Companion.readAsResult
import javax.inject.Inject
import javax.inject.Singleton

private val Context.privyLociRepository by preferencesDataStore("privy_loci_repo")
private const val keyIsServiceRunning = "is_service_running"

// Random preferences needed for the app that need to stay for longer periods of time.
@Singleton
class Repository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore: DataStore<Preferences> = context.privyLociRepository
    private val isServiceRunningKey = booleanPreferencesKey(keyIsServiceRunning)
    val wasFGPermissionRationaleDismissed =
        dataStore.readAsResult(fgPerrmissionRationaleDismissedKey, false)


    val wasFGPersistentNotificationDismissed =
        dataStore.readAsResult(fgPersistentNotificationDismissedKey, false)

    val wasReactivateFGRationaleDismissed =
        dataStore.readAsResult(reactivateFGRationaleDismissedKey, false)

    // We should make all these functions accept lambdas that make them log with a tag and message
    suspend fun setFGPermissionRationaleDismissed(dismissed: Boolean) {
        dataStore.editKey(
            fgPerrmissionRationaleDismissedKey, dismissed
        )
    }

    suspend fun setFGPersistentNotificationDismissed(dismissed: Boolean) {
        dataStore.editKey(
            fgPersistentNotificationDismissedKey, dismissed
        )
    }

    suspend fun setReactivateFGRationaleDismissed(dismissed: Boolean) {
        dataStore.editKey(
            reactivateFGRationaleDismissedKey, dismissed
        )
    }

    val isServiceRunning: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[isServiceRunningKey] ?: false
        }

    suspend fun setServiceRunning(isRunning: Boolean) {
        dataStore.edit { preferences ->
            preferences[isServiceRunningKey] = isRunning
        }
    }

    private companion object {
        val fgPerrmissionRationaleDismissedKey =
            booleanPreferencesKey("fg_permission_rationale_dismissed")
        val fgPersistentNotificationDismissedKey =
            booleanPreferencesKey("fg_persistent_notification_dismissed")
        val reactivateFGRationaleDismissedKey =
            booleanPreferencesKey("reactivate_fg_rationale_dismissed")

    }

}