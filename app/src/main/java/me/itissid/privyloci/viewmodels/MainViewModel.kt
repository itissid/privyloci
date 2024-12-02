package me.itissid.privyloci.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.UserPreferences
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
    private val userPreferences: UserPreferences
) :
    AndroidViewModel(application) {

    private val _isServiceRunning = MutableLiveData<Boolean>()
    val isServiceRunning: LiveData<Boolean> get() = _isServiceRunning

    // Expose preferences as StateFlow public variables.
    val wasFGPermissionRationaleDismissed: StateFlow<Boolean> =
        userPreferences.wasFGPerrmissionRationaleDismissed
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )

    val wasFGPersistentNotificationDismissed: StateFlow<Boolean> =
        userPreferences.wasFGPersistentNotificationDismissed
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false
            )

    val wasReactivateFGRationaleDismissed: StateFlow<Boolean> =
        userPreferences.wasReactivateFGRationaleDismissed
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

    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PrivyForegroundService.ACTION_SERVICE_STARTED -> _isServiceRunning.value = true
                PrivyForegroundService.ACTION_SERVICE_STOPPED -> _isServiceRunning.value = false
            }
        }
    }

    init {
        // Register the receiver
        val filter = IntentFilter().apply {
            addAction(PrivyForegroundService.ACTION_SERVICE_STARTED)
            addAction(PrivyForegroundService.ACTION_SERVICE_STOPPED)
        }
        LocalBroadcastManager.getInstance(getApplication())
            .registerReceiver(serviceStatusReceiver, filter)
    }

    fun setFGPermissionRationaleDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            userPreferences.setFGPermissionRationaleDismissed(dismissed)
        }
    }

    fun setFGPersistentNotificationDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            userPreferences.setFGPersistentNotificationDismissed(dismissed)
        }
    }

    fun setReactivateFGRationaleDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            userPreferences.setReactivateFGRationaleDismissed(dismissed)
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

    override fun onCleared() {
        super.onCleared()
        // Unregister the receiver
        LocalBroadcastManager.getInstance(getApplication())
            .unregisterReceiver(serviceStatusReceiver)
    }
}
