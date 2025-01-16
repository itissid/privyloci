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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.BTDevicesStatus
import me.itissid.privyloci.viewmodels.BleDevicesViewModel
import me.itissid.privyloci.viewmodels.ExperimentFlagViewModel
import me.itissid.privyloci.viewmodels.PlaceTagsWithDevicesState

@Composable
fun HomeScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    locationPermissionGranted: Boolean,
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
                locationPermissionGranted = locationPermissionGranted,
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
                locationPermissionGranted = locationPermissionGranted,
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
    blePermissionGranted: Boolean,
    noPermissionUIHandler: () -> Unit,
    bleViewModel: BleDevicesViewModel,
) {
    if (places.isEmpty()) {
        // So much empty!
        Spacer(modifier = Modifier.fillMaxSize())
        return
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val experimentFlagViewModel: ExperimentFlagViewModel = hiltViewModel()
    val experimentOn by experimentFlagViewModel.onboardingExperimentOn.collectAsState()

    Logger.v("PlacesAndAssetScreen", "Onboarding experiment is $experimentOn")


    // State to track the selected device and whether we are waiting for connection
    var waitingForConnection by remember { mutableStateOf<OnboardingInternalBTDeviceTracker?>(null) }

    if (waitingForConnection != null && experimentOn) {
        if (waitingForConnection?.internalBtDevice?.isConnected == false) {
            OnboardingWaitDialog(
                deviceName = waitingForConnection?.internalBtDevice?.name ?: "Your Device",
                onDismiss = { waitingForConnection = null }
            )
        } else {
            // Device already connected
            // TODO: Play a media and then wait
            Logger.v("PlacesAndAssetsScreen", "This should Trigger a media")
        }
    }

    val placeTagsWithSelectedDeviceStatus =
        bleViewModel.placeTagsWithSelectedDevicesState.collectAsState().value
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
                blePermissionGranted = blePermissionGranted,
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
                blePermissionGranted,
                bluetoothEnabled = true,
                btDevicesRescanHandler = {
                    coroutineScope.launch {
                        Logger.v(
                            "PlacesAndAssetsScreen",
                            "Loading Bonded devices from rescan handler"
                        )
                        // This is on the UI thread main thread.
                        bleViewModel.loadBondedBleDevices()
                    }
                },
            )
        }

        is PlaceTagsWithDevicesState.Success -> {
            // Game on
            val selectedDevices =
                placeTagsWithSelectedDeviceStatus.placeTagsWithDevices.associate {
                    it.first.id to it.second
                }
            val bleDeviceStatus = bleViewModel.bleDevices.collectAsState().value

            // For onboarding flow
            if (bleDeviceStatus is BTDevicesStatus.BTDevicesFound) {
                val device =
                    bleDeviceStatus.devices.find { it == waitingForConnection?.internalBtDevice }

                if (device != null && waitingForConnection != null) {
                    if (waitingForConnection?.internalBtDevice?.isConnected != device.isConnected) {
                        waitingForConnection =
                            OnboardingInternalBTDeviceTracker(device, System.currentTimeMillis())
                    }
                }
            }

            val bleDevices =
                if (bleDeviceStatus is BTDevicesStatus.BTDevicesFound) {
                    bleDeviceStatus.devices
                } else {
                    Logger.w(
                        "PlacesAndAssetsScreen",
                        "No BLE devices found. This should not happen."
                    )
                    null
                }

            val onDeviceSelected = { placeTag: PlaceTag, selectedAddress: String ->
                // Also inserts device selection in the place tag in the database.
                bleViewModel.selectDeviceForPlaceTag(placeTag, selectedAddress)

                if (experimentOn && placeTag.isTypeBLE()) {
                    val selectedDevice = bleDevices?.firstOrNull { it.address == selectedAddress }
                    if (selectedDevice != null) {
                        Logger.v("PlacesAndAssetsScreen", "Device $selectedDevice is selected")
                        waitingForConnection = OnboardingInternalBTDeviceTracker(
                            selectedDevice,
                            System.currentTimeMillis()
                        )
                    }
                } // else
            }
            PlaceCards(
                places,
                blePermissionGranted,
                bleDevices = bleDevices,
                bluetoothEnabled = true,
                selectedDevices = selectedDevices,
                onDeviceSelectedForPlaceTag = onDeviceSelected
            )
        }
    }
    LaunchedEffect(waitingForConnection) {
        if (waitingForConnection != null && waitingForConnection?.internalBtDevice?.isConnected == true) {

            Logger.v("PlacesAndAssetsScreen", "Device $waitingForConnection is connected")
            // Onboarding success: user put on their headphones
            waitingForConnection = null
            // Trigger next step: onOnboardingComplete could tell the service to play media
            // onOnboardingComplete()

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
    onDeviceSelectedForPlaceTag: (PlaceTag.(address: String) -> Unit)? = null,
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
                    onDeviceSelectedForPlaceTag?.invoke(placeTag, address)
                    selectedDevice.value = this
                },
                btDevicesRescanHandler = btDevicesRescanHandler ?: {},
                preSelectedDevice = selectedDevices?.get(placeTag.id)
            )
        }
    }
}
