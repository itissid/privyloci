package me.itissid.privyloci.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.itissid.privyloci.MainScreen
import me.itissid.privyloci.data.DataProvider
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.ui.theme.PrivyLociTheme
import me.itissid.privyloci.viewmodels.LocationPermissionState


@Preview(showBackground = true, widthDp = 320, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun MainScreenPreview() {
    val (placesList, assetsList, subscriptionsList) = DataProvider.getData()
    val places = placesList + assetsList
    val userSubscriptions = subscriptionsList.filter { it.type == SubscriptionType.USER }
    val appContainers = DataProvider.processAppContainers(subscriptionsList)
    val locationPermissionState = LocationPermissionState(
        permissionsGranted = false,
        isPaused = false
    )
    PrivyLociTheme {
        MainScreen(
            appContainers = appContainers,
            userSubscriptions = userSubscriptions,
            places = places,
            locationPermissionState = locationPermissionState,
            locationPermissionHandler = {},
            btPermissionHandler = {},
            btDevicesRescanHandler = {},
            btDevicesSelectedHandler = { _, _ -> },
        )
    }
}
