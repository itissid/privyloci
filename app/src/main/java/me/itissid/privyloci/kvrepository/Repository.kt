package me.itissid.privyloci.kvrepository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.itissid.privyloci.kvrepository.UserPreferences.Companion.editKey
import me.itissid.privyloci.kvrepository.UserPreferences.Companion.readWrapper
import javax.inject.Inject
import javax.inject.Singleton

private val Context.privyLociRepository by preferencesDataStore("privy_loci_repo")
private const val keyIsServiceRunning = "is_service_running"
private const val TAG = "Repository"
// Random preferences needed for the app that need to stay for longer periods of time.
@Singleton
class Repository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore: DataStore<Preferences> = context.privyLociRepository
    private val isServiceRunningKey = booleanPreferencesKey(keyIsServiceRunning)
    val wasFGPermissionRationaleDismissed =
        dataStore.readWrapper(fgPerrmissionRationaleDismissedKey, false, "Repository")


    val wasFGPersistentNotificationDismissed =
        dataStore.readWrapper(fgPersistentNotificationDismissedKey, false, "Repository")

    val wasReactivateFGRationaleDismissed =
        dataStore.readWrapper(reactivateFGRationaleDismissedKey, false, "Repository")

    // We should make all these functions accept lambdas that make them log with a tag and message
    suspend fun setFGPermissionRationaleDismissed(dismissed: Boolean) {
        dataStore.editKey(
            fgPerrmissionRationaleDismissedKey, dismissed, TAG
        )
    }

    suspend fun setFGPersistentNotificationDismissed(dismissed: Boolean) {
        dataStore.editKey(
            fgPersistentNotificationDismissedKey, dismissed, TAG
        )
    }

    suspend fun setReactivateFGRationaleDismissed(dismissed: Boolean) {
        dataStore.editKey(
            reactivateFGRationaleDismissedKey, dismissed, TAG
        )
    }

    val isServiceRunning: Flow<Boolean> = dataStore.readWrapper(
        isServiceRunningKey, false, "Repository"
    )

    suspend fun setServiceRunning(isRunning: Boolean) {
        dataStore.editKey(
            isServiceRunningKey, isRunning, TAG
        )
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