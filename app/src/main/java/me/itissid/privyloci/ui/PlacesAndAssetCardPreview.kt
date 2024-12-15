package me.itissid.privyloci.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.itissid.privyloci.data.DataProvider
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.ui.theme.PrivyLociTheme


@Preview(
    name = "Night mode Places Card",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(name = "Day mode Places Card", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun PlacesCardPreview() {
    PrivyLociTheme {
        val (placesList, assetList, _) = DataProvider.getData()
        val placesAndAssets = placesList + assetList
        val onDeviceSelected =
            { placeTag: PlaceTag, address: String -> println("Selected $placeTag and $address") }
        val bleDevices =
            listOf<InternalBtDevice>(
                InternalBtDevice("My Device", "00:00:00:00:00:00"),
                InternalBtDevice("Another Device", "00:00:00:00:00:01"),
                InternalBtDevice("Yet Another Device", "00:00:00:00:00:02"),
                InternalBtDevice("One More Device", "00:00:00:00:00:03"),
                // Add 5 more devices
                InternalBtDevice("My Headphones", "00:00:00:00:00:04"),
                InternalBtDevice("My Car", "00:00:00:00:00:05"),
                InternalBtDevice("My Watch", "00:00:00:00:00:06"),
                InternalBtDevice("My Laptop", "00:00:00:00:00:07"),
                InternalBtDevice("My Roomba", "00:00:00:00:00:08"),
                InternalBtDevice("My Fridge, yes, my fridge", "00:00:00:00:00:09"),
                InternalBtDevice("My TV", "00:00:00:00:00:10"),
                InternalBtDevice("My AC", "00:00:00:00:00:11"),
            )
        PlaceCards(
            places = placesAndAssets,
            bleDevices = bleDevices,
            noPermissionOnClickHandler = null,
            bluetoothNotEnabledHandler = null,
            onDeviceSelectedForPlaceTag = onDeviceSelected
        )
    }
}


@Preview(
    name = "Night mode Lazy Radio Dialog",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(name = "Day mode Lazy Radio Dialog", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun LazyRadioDialogueDisplay() {
    val bleDevices =
        listOf<InternalBtDevice>(
            InternalBtDevice("My Device", "00:00:00:00:00:00"),
            InternalBtDevice("Another Device", "00:00:00:00:00:01"),
            InternalBtDevice("Yet Another Device", "00:00:00:00:00:02"),
            InternalBtDevice("One More Device", "00:00:00:00:00:03"),
        )
    PrivyLociTheme {
        LazyRadioDialogue(
            bleDevices = bleDevices,
            selectedDevice = bleDevices[0],
            onDismiss = { println("Dismissed") },
            onclick = { println("Selected $this") }
        )
    }
}