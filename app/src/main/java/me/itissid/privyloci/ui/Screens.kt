package me.itissid.privyloci.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import me.itissid.privyloci.viewmodels.BleDevicesViewModel
import me.itissid.privyloci.viewmodels.ExperimentFlagViewModel


@Composable
fun HomeScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    locationPermissionGranted: Boolean,
    onLocationIconClick: () -> Unit
) {
    // Remember the expanded state for each app
    val expandedStateMap = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
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
                locationPermissionGranted=locationPermissionGranted,
                onLocationIconClick=onLocationIconClick
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
    val bleDevices by bleViewModel.bleDevices.collectAsState()
    val isBluetoothEnabled by bleViewModel.isBluetoothEnabled.collectAsState()
    // TODO: Use these. if these are not in the bleDevices, we should merge them
    Logger.v(
        "PlacesAndAssetsScreen",
        "Connected bleDevices: ${bleDevices?.filter { it.isConnected }}"
    )
    val coroutineScope = rememberCoroutineScope()

    val experimentFlagViewModel: ExperimentFlagViewModel = hiltViewModel()
    val experimentOn by experimentFlagViewModel.experimentOn.collectAsState()

    LaunchedEffect(bleDevices) {
        // Preloading prevents one less click in the UI.
        bleViewModel.loadBondedBleDevices()
    }

    val btDevicesRescanHandler = {
        coroutineScope.launch {
            Logger.v("PlacesAndAssetsScreen", "Loading Bonded devices from rescan handler")
            bleViewModel.loadBondedBleDevices()
        }
        Unit
    }

    var bluetoothEnableHandler: () -> Unit = (
            {
                if (!isBluetoothEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    ContextCompat.startActivity(context, intent, null)
                }
            })

    // State to track the selected device and whether we are waiting for connection
    var waitingForConnection by remember { mutableStateOf<InternalBtDevice?>(null) }

    // If waitingForConnection is not null, we show a dialog prompting user
    if (waitingForConnection != null && experimentOn) {
        if (waitingForConnection?.isConnected == true) {
            OnboardingWaitDialog(
                deviceName = waitingForConnection?.name ?: "Your Device",
                onDismiss = {
                    waitingForConnection = null
                }
            )
        } else {
            // Device already connected
            // Play a media and then wait
        }
    }

    val onDeviceSelected = { placeTag: PlaceTag, selectedAddress: String ->
        // Also inserts device selection in the place tag in the database.
        bleViewModel.selectDeviceForPlaceTag(placeTag, selectedAddress)

        if (experimentOn && placeTag.isTypeBLE()) {
            val selectedDevice = bleDevices?.firstOrNull { it.address == selectedAddress }
            if (selectedDevice != null) {
                Logger.v("PlacesAndAssetsScreen", "Device $selectedDevice is selected")
                waitingForConnection = selectedDevice
            }
        } // else

    }
    LaunchedEffect(bleDevices) {
        val targetDevice = waitingForConnection
        if (targetDevice != null && bleDevices != null) {
            val isNowConnected =
                bleDevices!!.any { it.address == targetDevice.address && it.isConnected }
            if (isNowConnected) {
                Logger.v("PlacesAndAssetsScreen", "Device $targetDevice is connected")
                // Onboarding success: user put on their headphones
                waitingForConnection = null
                // Trigger next step: onOnboardingComplete could tell the service to play media
//                onOnboardingComplete()
                
            }
        }
    }

    PlaceCards(
        places,
        blePermissionGranted,
        noPermissionUIHandler,
        bleDevices,
        btDevicesRescanHandler,
        isBluetoothEnabled,
        bluetoothEnableHandler,
        onDeviceSelected
    )
}

@Composable
fun OnboardingWaitDialog(
    deviceName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Put On Your Headphones") },
        text = {
            Text("Please put on and connect $deviceName. We will begin a short onboarding process as soon as they're connected.")
        }
    )
}

@Composable
fun PlaceCards(
    places: List<PlaceTag>,
    blePermissionGranted: Boolean,
    noPermissionOnClickHandler: () -> Unit,
    bleDevices: Set<InternalBtDevice>?,
    btDevicesRescanHandler: () -> Unit,
    bluetoothEnabled: Boolean,
    bluetoothNotEnabledHandler: () -> Unit,
    onDeviceSelectedForPlaceTag: PlaceTag.(address: String) -> Unit
) {
    // This map is needed to maintain a list of device choices selected by the user internally.
    // TODO: When this code is stable move this logic to the ViewModel and hold this state there.
    // to do this I need to create an intermediate flow with another derived type of PlaceTag and that prepopulates it with
    // the logic in getSelectedDeviceAddress. Then I can use tha directly.
    var selectedDevicesMap by remember {
        mutableStateOf<Map<Int, InternalBtDevice?>>(emptyMap())
    }
    LaunchedEffect(places, bleDevices) {
        val newMap = places.filter { it.isTypeBLE() }.associate { placeTag ->
            val selectedAddress = placeTag.getSelectedDeviceAddress()
            val selectedDevice = bleDevices?.firstOrNull { it.address == selectedAddress }
            placeTag.id to selectedDevice
        }
        selectedDevicesMap = newMap
    }
    Logger.v("PlaceCards", "Selected Devices: $selectedDevicesMap")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(places, key = { it.id }) { placeTag ->
//            Logger.v(
//                "PlaceCards",
//                "bluetoothEnabled? $bluetoothEnabled "
//            )
            val selectedDevice = selectedDevicesMap[placeTag.id]
            selectedDevice?.run {
                Logger.v(
                    "PlaceCards",
                    "PlaceCard: ${placeTag.name} has preSelectedDevice: $selectedDevice"
                )
            }
            // N2S: Instead consider just consider passing a BTDeviceState like object that  sets state
            // do this action or that action. The different states are:
            // 1. BT not enabled
            // 2. BT enabled but no bonded user devices are not loaded.
            // 3. BT enabled but no bonded user devices could be found.
            PlaceCard(
                placeTag = placeTag,
                blePermissionGranted,
                noPermissionOnClickHandler,
                bleDevices,
                bluetoothEnabled,
                bluetoothNotEnabledHandler = bluetoothNotEnabledHandler,
                onDeviceSelectedForPlaceTag = { address ->
                    onDeviceSelectedForPlaceTag(placeTag, address)

                    val newlySelectedDevice = bleDevices?.firstOrNull { it.address == address }
                    selectedDevicesMap = selectedDevicesMap.toMutableMap().apply {
                        this[placeTag.id] = newlySelectedDevice
                    }
                },
                btDevicesRescanHandler = btDevicesRescanHandler,
                preSelectedDevice = selectedDevice

            )
        }
    }
}