package me.itissid.privyloci.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.launch
import me.itissid.privyloci.kvrepository.UserPreferences
import me.itissid.privyloci.util.Logger
import javax.inject.Inject

enum class BleRationaleState {
    BLE_PERMISSION_RATIONALE_SHOULD_BE_SHOWN,
    MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED,
    VISIT_SETTINGS
    // N2S: No Resume/Pause needed(like I did in location collection code) because we aren't collecting anything here.
}

data class BlePermissionRationaleState(
    val reason: BleRationaleState,
    val rationaleText: String,
    val proceedButtonText: String = "Proceed",
    val dismissButtonText: String = "Cancel",
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

sealed class BlePermissionEvent {
    object OpenSettings : BlePermissionEvent()
    object RequestBlePermissions : BlePermissionEvent()
    // ... add more events as needed
}

@HiltViewModel
class BlePermissionViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences,
) : AndroidViewModel(application) {

    // Tracks the current BLE rationale state
    private val _blePermissionRationaleState = MutableStateFlow<BlePermissionRationaleState?>(null)
    val blePermissionRationaleState: StateFlow<BlePermissionRationaleState?> =
        _blePermissionRationaleState.asStateFlow()

    private val _events = MutableSharedFlow<BlePermissionEvent>()
    val permissionEvents = _events.asSharedFlow()

    // Flow indicating if the user visited the BLE permission launcher before
    private val userVisitedBlePermissionLauncher = MutableStateFlow<Boolean>(false)

    init {
        viewModelScope.launch {
            userPreferences.userVisitedBlePermissionLauncher.collect {
                val visited = it.getOrDefault(false)
                userVisitedBlePermissionLauncher.value = visited
            }
        }
    }

//    private val userVisitedBlePermissionLauncher: StateFlow<Result<Boolean>> =
//        userPreferences.userVisitedBlePermissionLauncher.stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(),
//            initialValue = Result.success(false)
//        )

    // Flow or a boolean indicating if BLE permission is currently granted
    private val _blePermissionGranted: MutableStateFlow<Boolean> =
        // Set externally from the accompanist library. We cant have it here because it is a composable.
        MutableStateFlow(false) // placeholder

    private val _shouldShowRationale: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val shouldShowRationale: StateFlow<Boolean> = _shouldShowRationale.asStateFlow()
    fun setShouldShowRationale(show: Boolean) {
        _shouldShowRationale.value = show
    }

    val blePermissionGranted: StateFlow<Boolean> = _blePermissionGranted.asStateFlow()

    fun setBlePermissionGranted(granted: Boolean) {
        _blePermissionGranted.value = granted
    }

    /**
     * Called when the BLE icon is clicked. Decides what rationale or action to show.
     *
     * The composable only calls this method. This method checks current conditions:
     * - Are permissions granted?
     * - Should show rationale?
     * - Has the user visited the launcher before?
     */
    fun onBleIconClicked() {
        val allGranted = blePermissionGranted.value
        if (allGranted) {
            return
        }
        val visitedBefore = userVisitedBlePermissionLauncher.value

        Logger.v(
            "BlePermissionViewModel", ""
        )
        Logger.v("BlePermissionViewModel", "Visited before: $visitedBefore")
        when {
            shouldShowRationale.value -> {
                // Show a rationale dialogue
                Logger.v("BlePermissionViewModel", "Setting state for BLE Permission Rationale")
                _blePermissionRationaleState.value = BlePermissionRationaleState(
                    reason = BleRationaleState.BLE_PERMISSION_RATIONALE_SHOULD_BE_SHOWN,
                    rationaleText = "To discover and connect to your Bluetooth devices, we need Bluetooth permission.",
                    onConfirm = {
                        setUserVisitedBlePermissionLauncher(false)
                        launchBlePermissionRequest()
                    },
                    onDismiss = { clearBlePermissionRationale() }
                )
            }

            !visitedBefore -> {
                // Multiple permissions should be launched (first time user sees it)
                Logger.v(
                    "BlePermissionViewModel",
                    "Setting state for Multiple Permissions Rationale"
                )
                _blePermissionRationaleState.value = BlePermissionRationaleState(
                    reason = BleRationaleState.MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED,
                    rationaleText = "To use BLE device features, please grant the permission.",
                    onConfirm = {
                        setUserVisitedBlePermissionLauncher(true)
                        launchBlePermissionRequest()
                    },
                    onDismiss = { clearBlePermissionRationale() }
                )
            }

            else -> {
                // User visited permission launcher before and still no permission → Visit Settings
                Logger.v("BlePermissionViewModel", "Setting state for Visit Settings Rationale")
                _blePermissionRationaleState.value = BlePermissionRationaleState(
                    reason = BleRationaleState.VISIT_SETTINGS,
                    rationaleText = "Please open app settings to grant Bluetooth permission.",
                    proceedButtonText = "Open Settings",
                    onConfirm = { openAppSettingsForBlePermission() },
                    onDismiss = { clearBlePermissionRationale() }
                )
            }
        }
    }

    fun setUserVisitedBlePermissionLauncher(visited: Boolean) {
        viewModelScope.launch {
            userPreferences.setUserVisitedBlePermissionLauncher(visited)
        }
    }

    private fun clearBlePermissionRationale() {
        _blePermissionRationaleState.value = null
    }


    private fun openAppSettingsForBlePermission() {
        // Launch intent to open App Settings
        // After launching settings, clear the rationale
        viewModelScope.launch {
            try {
                _events.emit(BlePermissionEvent.OpenSettings)
            } finally {
                clearBlePermissionRationale()
            }
        }
        // TODO: Use below code in the listener to `events`

    }

    /**
     * Called from onConfirm callback in Rationale to actually request BLE permissions
     * Use Accompanist Permission method:
     *
     */
    private fun launchBlePermissionRequest() {
        // This function might not be able to directly request permissions
        // since Accompanist permission requests typically happen inside a Composable.
        // Instead, store a state that the UI observes, prompting it to call
        // permissionState.launchPermissionRequest() from a composable side-effect.

        // For example, you could have a SharedFlow event that Composable listens to.
        // Or simply rely on a callback. For now let's assume a SharedFlow event:
        // E.g. _requestBlePermissionEvent.tryEmit(Unit)
        viewModelScope.launch {
            try {
                _events.emit(BlePermissionEvent.RequestBlePermissions)
            } finally {
                clearBlePermissionRationale()
            }
        }
    }

    // Additional logic if you have multiple BLE permissions:
    // For example, if you need both BLUETOOTH_SCAN and BLUETOOTH_CONNECT:
    // Check all needed permissions and request them similarly.
}