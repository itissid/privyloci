package me.itissid.privyloci.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.kvrepository.UserPreferences
import me.itissid.privyloci.kvrepository.Repository
import me.itissid.privyloci.service.PrivyForegroundService
import me.itissid.privyloci.util.Logger
import javax.inject.Inject

enum class ForeGroundServiceRationaleState {
    /** Dialogue rationale states for controlling the foreground service
     * */
    FG_PERMISSION_RATIONALE_DISMISSED,
    PERSISTENT_NOTIFICATION_DISMISSED,
    REACTIVATE_FG_RATIONALE_DISMISSED,
}

enum class RationaleState {

    /**
    Dialogue rationale states For the foreground permissions.
     * */
    LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN,
    MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED,
    VISIT_SETTINGS,
    PAUSE_LOCATION_COLLECTION,
    RESUME_LOCATION_COLLECTION,
}

data class ForegroundPermissionRationaleState(
    val reason: RationaleState,
    val rationaleText: String = "",
    val proceedButtonText: String = "Proceed",
    val dismissButtonText: String = "Cancel"

)


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences,
    private val repository: Repository
) :
    AndroidViewModel(application) {

    val isServiceRunning: StateFlow<Boolean>
        get() = repository.isServiceRunning.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    // Expose preferences as StateFlow public variables.
    val wasFGPermissionRationaleDismissed =
        repository.wasFGPermissionRationaleDismissed
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )

    val wasFGPersistentNotificationDismissed =
        repository.wasFGPersistentNotificationDismissed
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )

    val wasReactivateFGRationaleDismissed: StateFlow<Boolean> =
        repository.wasReactivateFGRationaleDismissed
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )

    val userVisitedPermissionLauncher: StateFlow<Boolean> =
        userPreferences.userVisitedPermissionLauncher
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )
    val userPausedLocationCollection: StateFlow<Boolean> =
        userPreferences.userPausedLocationCollection
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )

    fun setFGPermissionRationaleDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            repository.setFGPermissionRationaleDismissed(dismissed)
        }
    }

    fun setFGPersistentNotificationDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            repository.setFGPersistentNotificationDismissed(dismissed)
        }
    }

    fun setReactivateFGRationaleDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            repository.setReactivateFGRationaleDismissed(dismissed)
        }
    }

    fun setUserPausedLocationCollection(paused: Boolean) {
        viewModelScope.launch {
            userPreferences.setUserPausedLocationCollection(paused)
        }
    }

    fun setUserVisitedPermissionLauncherPreference(dismissed: Boolean) {

        viewModelScope.launch {
            try {
                userPreferences.setUserVisitedPermissionLauncher(dismissed)
            } catch (e: Exception) {
                Logger.e("MainViewModel", "Error setting UserVisitedPermissionLauncher", e)
            }
        }
    }

    fun setServiceRunning(isRunning: Boolean) {
        viewModelScope.launch {
            try {
                Logger.v("MainViewModel", "Setting ServiceRunning to $isRunning")
                repository.setServiceRunning(isRunning)
            } catch (e: Exception) {
                Logger.e("MainViewModel", "Error setting ServiceRunning", e)
            }
        }
    }
}
