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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.BleDevicesViewModel


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
    val connectedDevices by bleViewModel.connectedDevices.collectAsState()
    val btDevicesRescanHandler = {
        bleViewModel.loadBondedBleDevices()
    }
    var bluetoothNotEnabledHandler: () -> Unit = (
            {
                if (isBluetoothEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    ContextCompat.startActivity(context, intent, null)
                }
            })

    val onDeviceSelected = { placeTag: PlaceTag, selectedAddress: String ->
        bleViewModel.selectDeviceForPlaceTag(placeTag, selectedAddress)
    }
    PlaceCards(
        places,
        blePermissionGranted,
        noPermissionUIHandler,
        bleDevices,
        btDevicesRescanHandler,
        isBluetoothEnabled,
        bluetoothNotEnabledHandler,
        onDeviceSelected
    )
}

@Composable
fun PlaceCards(
    places: List<PlaceTag>,
    blePermissionGranted: Boolean,
    noPermissionOnClickHandler: () -> Unit,
    bleDevices: List<InternalBtDevice>?,
    btDevicesRescanHandler: () -> Unit,
    bluetoothEnabled: Boolean,
    bluetoothNotEnabledHandler: () -> Unit,
    onDeviceSelectedForPlaceTag: PlaceTag.(address: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(places) { placeTag ->
            Logger.v(
                "PlaceCards",
                "bluetoothEnabled? $bluetoothEnabled "
            )
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
                onDeviceSelectedForPlaceTag = onDeviceSelectedForPlaceTag,
                btDevicesRescanHandler = btDevicesRescanHandler
            )
        }
    }
}