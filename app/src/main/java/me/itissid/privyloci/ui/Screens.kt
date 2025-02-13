package me.itissid.privyloci.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.service.PrivyForegroundService
import me.itissid.privyloci.service.startPrivyForegroundService
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.BTDevicesStatus
import me.itissid.privyloci.viewmodels.ExperimentFlagViewModel
import me.itissid.privyloci.viewmodels.LocationPermissionState
import me.itissid.privyloci.viewmodels.PlaceTagsWithDevicesState

@Composable
fun HomeScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    locationPermissionState: LocationPermissionState?,
    onLocationIconClick: () -> Unit
) {
    // Remember the expanded state for each app
    val expandedStateMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        // App Subscriptions Section
        item {
            Text(
                text = "App Subscriptions",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(appContainers) { appContainer ->
            val isExpanded = expandedStateMap[appContainer.name] ?: false
            AppCard(
                appContainer = appContainer,
                isExpanded = isExpanded,
                onMenuClick = {
                    // Handle menu click (e.g., show options to delete or pause subscriptions)
                },
                onCardClick = {
                    // Toggle expansion state
                    expandedStateMap[appContainer.name] = !isExpanded
                },
                locationPermissionState = locationPermissionState,
                onLocationIconClick = onLocationIconClick
            )
        }
        // User Subscriptions Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "User Subscriptions",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(userSubscriptions) { subscription ->
            SubscriptionCard(
                subscription = subscription,
                locationPermissionState = locationPermissionState,
                onLocationIconClick = onLocationIconClick,
            )
        }
    }
}

data class OnboardingInternalBTDeviceTracker(
    val internalBtDevice: InternalBtDevice,
    val version: Long = System.currentTimeMillis()
)

@Composable
fun PlacesAndAssetsScreen(
    places: List<PlaceTag>,
    noPermissionUIHandler: () -> Unit,
    btDevicesRescanHandler: () -> Unit,
    placeTagsWithSelectedDeviceStatus: PlaceTagsWithDevicesState?,
    onBTDeviceSelectedHandler: (PlaceTag, InternalBtDevice) -> Unit,
    btDeviceStatus: BTDevicesStatus?
) {
    if (places.isEmpty()) {
        // So much empty!
        Spacer(modifier = Modifier.fillMaxSize())
        return
    }
    val context = LocalContext.current

    val experimentFlagViewModel: ExperimentFlagViewModel = hiltViewModel()
    val experimentOn by experimentFlagViewModel.onboardingExperimentOn.collectAsState()
    val onboardingExperimentComplete by experimentFlagViewModel.onboardingExperimentComplete.collectAsState()

    Logger.v(
        "PlacesAndAssetScreen",
        "Onboarding experiment on?: $experimentOn, onboarding complete?: $onboardingExperimentComplete"
    )


    // State to track the selected device and whether we are waiting for connection
    var onboardingDeviceWaitingForConnection by remember {
        mutableStateOf<OnboardingInternalBTDeviceTracker?>(
            null
        )
    }
    // N2S: If we make onboarding a permanent feature, we don't want to re-trigger this.
    if (onboardingDeviceWaitingForConnection != null && experimentOn) {
        val waitForConnectionDevice = onboardingDeviceWaitingForConnection!!.internalBtDevice
        if (!onboardingExperimentComplete) {
            if (!waitForConnectionDevice.isConnected) {
                OnboardingWaitDialog(
                    deviceName = waitForConnectionDevice.name,
                    onDismiss = { onboardingDeviceWaitingForConnection = null }
                )
            } else {
                // Device already connected
                // TODO: Move this out to a handler
                startPrivyForegroundService(
                    context,
                    PrivyForegroundService.ACTION_PLAY_ONBOARDING_SOUND
                    // putExtra(PrivyForegroundService.EXTRA_DEVICE_ADDRESS, deviceTracker.internalBtDevice.address)
                )
                Logger.v("PlacesAndAssetsScreen", "On boarding is not complete")
            }
        } else {
            Logger.i("PlacesAndAssetsScreen", "Onboarding experiment is complete")
            onboardingDeviceWaitingForConnection = null
        }
    }
    when (placeTagsWithSelectedDeviceStatus) {
        is PlaceTagsWithDevicesState.NoPlaceHasBTType -> {
            // We don't need permissions, just initialize the place cards.
            PlaceCards(places)
        }

        is PlaceTagsWithDevicesState.PermissionNotGranted -> {
            PlaceCards(places, false, noPermissionUIHandler)
        }

        is PlaceTagsWithDevicesState.Loading -> {
            // TODO: Test by adding an artificial delay here.
            // N2S: If no devices are found, How to trigger a rescan?
            CircularProgressIndicator()
        }

        is PlaceTagsWithDevicesState.BtNotEnabled -> {
            PlaceCards(
                places,
                blePermissionGranted = true,
                bluetoothEnabled = false,
                bluetoothNotEnabledHandler = {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    ContextCompat.startActivity(context, intent, null)
                },
            )
        }

        is PlaceTagsWithDevicesState.BTDevicesNotFound -> {
            PlaceCards(
                places,
                blePermissionGranted = true,
                bluetoothEnabled = true,
                btDevicesRescanHandler = btDevicesRescanHandler
            )
        }

        is PlaceTagsWithDevicesState.Success -> {
            // Game on
            val selectedDevices =
                placeTagsWithSelectedDeviceStatus.placeTagsWithDevices.associate {
                    it.first.id to it.second
                }

            Logger.v(
                "PlacesAndAssetsScreen",
                "selectedDevices: ${selectedDevices}}"
            )
            if (!onboardingExperimentComplete && experimentOn && btDeviceStatus is BTDevicesStatus.BTDevicesFound) {
                val device =
                    btDeviceStatus.devices.find { it == onboardingDeviceWaitingForConnection?.internalBtDevice }

                if (device != null && onboardingDeviceWaitingForConnection?.internalBtDevice?.isConnected != device.isConnected) {
                    // At this point the user is in the onboarding flow and selected the device. The onboarding
                    // dialogue is showing. The user has connected the device or connected/disconnected the device
                    // connection state of device represented by waitingForConnection has changed.
                    onboardingDeviceWaitingForConnection =
                        OnboardingInternalBTDeviceTracker(device, System.currentTimeMillis())
                }
            }

            val bleDevices =
                if (btDeviceStatus is BTDevicesStatus.BTDevicesFound) {
                    btDeviceStatus.devices
                } else {
                    Logger.w(
                        "PlacesAndAssetsScreen",
                        "No BLE devices found. This should not happen."
                    )
                    null
                }

            val onDeviceSelected = { placeTag: PlaceTag, selectedDevice: InternalBtDevice ->
                // Also inserts device selection in the place tag in the database.
                onBTDeviceSelectedHandler(placeTag, selectedDevice)

                if (!onboardingExperimentComplete && experimentOn && placeTag.isTypeBLE()) {
                    if (!selectedDevice.hasHighDefinitionAudioCapabilities) {
                        Logger.i(
                            "PlacesAndAssetScreen",
                            "Rejecting ${selectedDevice.name} because it has no audio capabilities"
                        )
                    } else if (!placeTag.isTypeHeadphone()) {
                        Logger.i(
                            "PlacesAndAssetScreen",
                            "Rejecting ${selectedDevice.name} because it is not created under a headphone type"
                        )
                    } else {
                        val selectedDeviceInModel = bleDevices?.firstOrNull { it == selectedDevice }
                        if (selectedDeviceInModel != null) {
                            val hasSelectedDeviceChangedState =
                                (onboardingDeviceWaitingForConnection?.internalBtDevice != selectedDevice) || (onboardingDeviceWaitingForConnection?.internalBtDevice?.isConnected != selectedDevice.isConnected)
                            if (onboardingDeviceWaitingForConnection == null || hasSelectedDeviceChangedState) {
                                // We have not selected this device before and it has a different connection state
                                Logger.v(
                                    "PlacesAndAssetsScreen",
                                    "Device $selectedDeviceInModel will be used for onboarding"
                                )
                                // TODO: If we make onboarding permanent we should save the device chosen for onboarding
                                // this would not make this flow trigger repeatedly
                                onboardingDeviceWaitingForConnection =
                                    OnboardingInternalBTDeviceTracker(
                                    selectedDeviceInModel,
                                    System.currentTimeMillis()
                                )
                            } else {
                                Logger.v(
                                    "PlacesAndAssetScreen",
                                    "No change to device state for ${selectedDevice.name} for ${placeTag.name}. No onboarding triggered."
                                )
                            }
                        } else {
                            Logger.w(
                                "PlacesAndAssetScreen",
                                "No device found for selected device ${selectedDevice.name}. Possibly a view model bug."
                            )
                        }
                    }
                } else {
                    Logger.v(
                        "PlacesAndAssertScreen",
                        "onboardingExperimentComplete?>: $onboardingExperimentComplete, OnboardingExperiemntOn?: $experimentOn, Asset is BLE type: ${placeTag.isTypeBLE()}"
                    )
                }
            }
            PlaceCards(
                places,
                blePermissionGranted = true,
                bleDevices = bleDevices,
                bluetoothEnabled = true,
                selectedDevices = selectedDevices,
                onDeviceSelectedForPlaceTag = onDeviceSelected
            )
        }

        null -> {
            PlaceCards(places)
        }
    }
}

@Composable
fun OnboardingWaitDialog(deviceName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Put On Your Headphones") },
        text = {
            Text(
                "Please put on and connect $deviceName. We will begin a short onboarding process as soon as they're connected."
            )
        }
    )
}

@Composable
fun PlaceCards(
    places: List<PlaceTag>,
    blePermissionGranted: Boolean? = null,
    noPermissionOnClickHandler: (() -> Unit)? = null,
    bleDevices: Set<InternalBtDevice>? = null,
    btDevicesRescanHandler: (() -> Unit)? = null,
    bluetoothEnabled: Boolean? = null,
    bluetoothNotEnabledHandler: (() -> Unit)? = null,
    selectedDevices: Map<Int, InternalBtDevice?>? = null,
    onDeviceSelectedForPlaceTag: (PlaceTag.(device: InternalBtDevice) -> Unit)? = null,
) {
    // This map is needed to maintain a list of device choices selected by the user internally.
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(places, key = { it.id }) { placeTag ->
            // N2S: Instead consider just consider passing a BTDeviceState like object that  sets
            // state
            // do this action or that action. The different states are:
            // 1. BT not enabled
            // 2. BT enabled but no bonded user devices are not loaded.
            // 3. BT enabled but no bonded user devices could be found.
            val selectedDevice = remember { mutableStateOf<InternalBtDevice?>(null) }
//            Logger.v(
//                "PlaceCard",
//                "Selected device for PlaceTag ${placeTag.name}: ${selectedDevices?.get(placeTag.id)}"
//            )
            PlaceCard(
                placeTag = placeTag,
                blePermissionGranted ?: false,
                noPermissionOnClickHandler ?: {},
                bleDevices,
                bluetoothEnabled ?: false,
                bluetoothNotEnabledHandler = bluetoothNotEnabledHandler ?: {},
                onDeviceSelectedForPlaceTag = {
                    onDeviceSelectedForPlaceTag?.invoke(placeTag, this)
                    selectedDevice.value = this
                },
                btDevicesRescanHandler = btDevicesRescanHandler ?: {},
                preSelectedDevice = selectedDevices?.get(placeTag.id)
            )
        }
    }
}
