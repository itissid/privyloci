package me.itissid.privyloci.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.PlaceTagType
import me.itissid.privyloci.datamodels.tagString
import me.itissid.privyloci.ui.theme.PrivyLociTheme

@SuppressLint("MissingPermission")
@Composable
fun PlaceCard(
    placeTag: PlaceTag,
    blePermissionGranted: Boolean,
    noPermissionOnClick: () -> Unit,
    btDevices: List<InternalBtDevice>?,
    bluetoothEnabled: Boolean,
    bluetoothNotEnabledHandler: () -> Unit,
    onDeviceSelectedForPlaceTag: PlaceTag.(String) -> Unit,
    btDevicesRescanHandler: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<InternalBtDevice?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Place Name
                Text(
                    text = placeTag.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)

                )
                if (placeTag.isTypeBLE()) {
                    if (!blePermissionGranted) {// if ble permissions are not granted show the icon else nothing
                    IconButton(onClick = { noPermissionOnClick.invoke() }) {
                        AdaptiveIconWrapper(
                            permissionGranted = false,
                            iconResource = IconResource.BLEIcon
                        )
                    } // N2S: The triple dot could show the BLE turned on if its not.
                    } else {  // only when permission is not granted for BT devices
                    IconButton(onClick = { showDialog = !showDialog }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    if (showDialog) {
                        if (!bluetoothEnabled) {
                            BluetoothEnablePrompt(
                                assetName = placeTag,
                                onDismiss = { showDialog = false }
                            ) {
                                bluetoothNotEnabledHandler.invoke()
                                showDialog = false
                            }
                        } else {
                            if (btDevices.isNullOrEmpty()) {
                                ScanForBTDevices(
                                    placeTag,
                                    onDismiss = { showDialog = false },
                                    onConfirm = btDevicesRescanHandler
                                )
                            } else {
                                LazyRadioDialogue(
                                    "Select a device for ${placeTag.name}",
                                    btDevices,
                                    selectedDevice,
                                    { showDialog = false },
                                    {
                                        selectedDevice = this
                                        onDeviceSelectedForPlaceTag(
                                            placeTag,
                                            this.address
                                        )
                                        showDialog = false
                                    },
                                )
                            }
                        }
                    }
                    }
                }// else TODO: Logic for non BT Assets/Places.
            }
            // Place Type
            Text(
                text = placeTag.type.tagString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun LazyRadioDialogue(
    titleText: String = "Select from the list",
    bleDevices: List<InternalBtDevice>,
    selectedDevice: InternalBtDevice?,
    onDismiss: () -> Unit,
    onclick: InternalBtDevice.() -> Unit
) {

    Dialog(
        properties = DialogProperties(dismissOnBackPress = true, usePlatformDefaultWidth = false),
        onDismissRequest = { onDismiss() }
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .wrapContentSize()
                .widthIn(max = 400.dp) // Constrain width
                .heightIn(max = 600.dp) // Constrain height
                .padding(16.dp)
        ) {


            Column {
                Text(
                    text = titleText,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(bleDevices) { device ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onclick(device)
                                }
                        ) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            RadioButton(
                                selected = selectedDevice == device,
                                onClick = { onclick(device) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

    }
}

@Composable
fun ScanForBTDevices(
    assetName: PlaceTag,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialogWrapper(
        title = "Refresh Bluetooth device list for ${assetName.name}",
        body = "Press the button below to proceed with the refreshing of Bluetooth device list with a scan ",
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm() },
        onConfirmText = "RefreshScan",
    )
}

@Composable
fun BluetoothEnablePrompt(
    assetName: PlaceTag,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialogWrapper(
        title = "This feature requires you to turn  Bluetooth on",
        body = "You may choose not to enable it, but then you can't use ${assetName.name} for any subscriptions",
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onConfirmText = "Enable Bluetooth"
    )
}

@Composable
fun AlertDialogWrapper(
    title: String,
    body: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onConfirmText: String
) {
    AlertDialog(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            if (body != null) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm() },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(onConfirmText)
            }
        },
        onDismissRequest = { onDismiss() },
    )
}
@Preview(
    name = "Night mode",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(name = "Day mode", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun BluetoothEnablePromptPreview() {
    PrivyLociTheme(dynamicColor = false) {
        BluetoothEnablePrompt(
            assetName = PlaceTag(
                id = 123,
                name = "Test Place",
                type = PlaceTagType.ASSET.BTHeadphones,
                metadata = "",
                createdAt = 1234L,
                isActive = false
            ),
            onDismiss = { /*TODO*/ }) {

        }
    }
}