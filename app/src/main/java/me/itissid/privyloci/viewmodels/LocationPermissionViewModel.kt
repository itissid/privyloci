package me.itissid.privyloci.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.itissid.privyloci.kvrepository.Repository
import me.itissid.privyloci.kvrepository.UserPreferences
import javax.inject.Inject
import me.itissid.privyloci.util.Logger

/**
 * Sealed class representing UI events that drive the location permission flow.
 */
sealed class LocationPermissionEvent {
    object RequestLocationPermissions : LocationPermissionEvent()
    object OpenSettings : LocationPermissionEvent()
    // Add more events as needed
}

/**
 * Rationale states, similar to the BLE approach but for location.
 * This enum is used only in this module
 */
enum class LocationRationaleState {
    LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN,
    MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED,
    VISIT_SETTINGS,
    PAUSE_LOCATION_COLLECTION,
    RESUME_LOCATION_COLLECTION
}

data class LocationPermissionRationale(
    val reason: LocationRationaleState,
    val rationaleText: String = "",
    val proceedButtonText: String = "Proceed",
    val dismissButtonText: String = "Cancel",
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

// Encapsulation to capture states to pass to different UI widgets in the app to act
// based on location permission state.,
// We don't want to use/pass the LocationRationaleState case to these widgets is only meant for the LocationRationale to handle
// permission flow, this one is used to indicate to other widgets the result of those
// permission actions by the user.
data class LocationPermissionState(
    val permissionsGranted: Boolean,
    val isPaused: Boolean,
    // At the moment I am not sure if dismissal should be the same thing as pause.
    // Dismissing the notification stops the foreground service, so its not a technical pause
    // its a stop. But the user does not need to know this distinction. Because the way
    // they activate it is the same: They go into the app and they start the service.
    //val fgNotificationDismissed: Boolean
)

const val TAG = "LocationPermissionViewModel"

@HiltViewModel
class LocationPermissionViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences,
    private val repository: Repository
) : AndroidViewModel(application) {
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    // Tracks whether the system says we “should show” a rationale (e.g., user denied once)
    private val _shouldShowRationale = MutableStateFlow(false)
    private val shouldShowRationale: StateFlow<Boolean> = _shouldShowRationale.asStateFlow()

    private val userPausedCollection = userPreferences.userPausedLocationCollection.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    private val userVisitedPermissionLauncher =
        userPreferences.userVisitedPermissionLauncher.stateIn(
            viewModelScope, SharingStarted.Eagerly, false
        )

    // StateFlow for the ephemeral rationale UI
    private val _locationPermissionRationale = MutableStateFlow<LocationPermissionRationale?>(null)
    val locationPermissionRationale: StateFlow<LocationPermissionRationale?> =
        _locationPermissionRationale.asStateFlow()

    // A shared flow for the LocationPermissionHandler Composable.
    private val _events = MutableSharedFlow<LocationPermissionEvent>()
    val events = _events.asSharedFlow()

    /**
     * Used by UI to make a decision on their state based on whatever permission
     * state and pause state exists in this view model.
     */
    private val _locationPermissionState = MutableStateFlow(
        LocationPermissionState(
            permissionsGranted = false,
            isPaused = false,
        )
    )

    val locationPermissionState: StateFlow<LocationPermissionState> =
        _locationPermissionState.asStateFlow()

    private fun updatePermissionStatus(granted: Boolean) {
        _locationPermissionState.update { current ->
            current.copy(permissionsGranted = granted)
        }
    }

    private fun togglePauseLocation(pause: Boolean) {
        _locationPermissionState.update { current ->
            current.copy(isPaused = pause)
        }
    }

    init {
        viewModelScope.launch {
            userPreferences.userPausedLocationCollection.collect { paused ->
                Logger.v(TAG, "userPreferences.userPausedLocationCollection $paused")
//                _userPausedCollection.value = paused
                togglePauseLocation(paused)
            }
            repository.wasFGPermissionRationaleDismissed.collect { pausedFromDismissal ->
                Logger.v(TAG, "userPreferences.userPausedLocationCollection $pausedFromDismissal")
//                _userPausedCollection.value = pausedFromDismissal
                togglePauseLocation(pausedFromDismissal)
            }
        }
    }

    /**
     * Externally triggered from the LocationPermissionHandler Composable when we check actual permissions
     */
    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermissionGranted.value = granted
        updatePermissionStatus(granted)
    }

    fun setShouldShowRationale(show: Boolean) {
        _shouldShowRationale.value = show
    }

    /**
     * Called from onConfirm in the rationale dialog
     */
    private fun launchLocationPermissionRequest() {
        viewModelScope.launch {
            Logger.v("LocationPermissionViewModel", "Emitting RequestLocationPermissions")
            _events.emit(LocationPermissionEvent.RequestLocationPermissions)
        }
    }

    private fun openAppSettingsForLocationPermission() {
        viewModelScope.launch {
            Logger.v("LocationPermissionViewModel", "Emitting OpenSettings event")
            _events.emit(LocationPermissionEvent.OpenSettings)
        }
    }

    /**
     * Resets the ephemeral rationale so the UI no longer shows a dialog
     */
    private fun clearLocationPermissionRationale() {
        _locationPermissionRationale.value = null
    }


    /**
     * This is how we unify the user's state with system state per the VM pattern.
     * Use in UI components to grant permission to location if it is not
     * and pause/resume collection if it is.
     */
    fun mutateLocationCollectionState() {
        // If permission is granted, show Pause/Resume logic
        Logger.v(TAG, "...")
        if (locationPermissionGranted.value) {
            handlePauseResumeFlow()
            return
        }
        // If permission not granted, see if we show rationale
        Logger.v(
            TAG,
            "onLocationIconClicked: userVisitedLauncher = ${userVisitedPermissionLauncher.value}" +
                    ", shouldShowRationale = ${shouldShowRationale.value}"
        )
        when {
            shouldShowRationale.value -> showRationale(LocationRationaleState.LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN)
            userVisitedPermissionLauncher.value.not() -> showRationale(LocationRationaleState.MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED)
            else -> showRationale(LocationRationaleState.VISIT_SETTINGS)
        }
    }

    private fun handlePauseResumeFlow() {
        if (!userPausedCollection.value) {
            // userPausedLocationCollection is false => show "Pause" dialog
            showRationale(LocationRationaleState.PAUSE_LOCATION_COLLECTION)
        } else {
            // userPausedLocationCollection is true => show "Resume" dialog
            showRationale(LocationRationaleState.RESUME_LOCATION_COLLECTION)
        }
    }

    private fun showRationale(state: LocationRationaleState) {
        Logger.v(TAG, ":showRational(${state.name})")
        _locationPermissionRationale.value = when (state) {
            LocationRationaleState.LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN -> {
                LocationPermissionRationale(
                    reason = state,
                    rationaleText = "In order to use PrivyLoci's location features,  please grant access by accepting the location permission dialog.",
                    onConfirm = {
                        // Because system says "should show rationale", let's not mark user visited
                        // until after they see the actual system dialog
                        launchLocationPermissionRequest()
                        clearLocationPermissionRationale()
                    },
                    onDismiss = {
                        clearLocationPermissionRationale()
                    }
                )
            }

            LocationRationaleState.MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED -> {
                LocationPermissionRationale(
                    reason = state,
                    rationaleText = "In order to use PrivyLoci's location feature, press 'Proceed' and grant the 'While in Use' location permissions.",
                    onConfirm = {
                        viewModelScope.launch {
                            Logger.v(TAG, "Settings user visited permission launcher")
                            userPreferences.setUserVisitedPermissionLauncher(true)
                        }
                        launchLocationPermissionRequest()
                        clearLocationPermissionRationale()
                    },
                    onDismiss = { clearLocationPermissionRationale() }
                )
            }

            LocationRationaleState.VISIT_SETTINGS -> {
                LocationPermissionRationale(
                    reason = state,
                    rationaleText = "In order to use PrivyLoci's location features, press 'Proceed', and in the settings and  select 'Allow only while using the app'",
                    proceedButtonText = "Open Settings",
                    onConfirm = {
                        openAppSettingsForLocationPermission()
                        clearLocationPermissionRationale()
                    },
                    onDismiss = {
                        clearLocationPermissionRationale()
                    }
                )
            }

            LocationRationaleState.PAUSE_LOCATION_COLLECTION -> {
                LocationPermissionRationale(
                    reason = state,
                    rationaleText = "You can pause App wide location collection by pressing Pause",
                    proceedButtonText = "Pause",
                    onConfirm = {
                        viewModelScope.launch {
                            userPreferences.setUserPausedLocationCollection(true)
                        }
                        clearLocationPermissionRationale()
                    },
                    onDismiss = { clearLocationPermissionRationale() }
                )
            }

            LocationRationaleState.RESUME_LOCATION_COLLECTION -> {
                LocationPermissionRationale(
                    reason = state,
                    rationaleText = "Resume location collection globally?",
                    proceedButtonText = "Resume",
                    onConfirm = {
                        viewModelScope.launch {
                            userPreferences.setUserPausedLocationCollection(false)
                        }
                        clearLocationPermissionRationale()
                    },
                    onDismiss = { clearLocationPermissionRationale() }
                )
            }
        }
    }
}
