package me.itissid.privyloci.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import me.itissid.privyloci.MainScreen
import me.itissid.privyloci.data.DataProvider
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.ui.theme.PrivyLociTheme


@Preview(showBackground = true, widthDp = 320, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun MainScreenPreview() {
    val (placesList, assetsList, subscriptionsList) = DataProvider.getData()
    val places = placesList + assetsList
    val userSubscriptions = subscriptionsList.filter { it.type == SubscriptionType.USER }
    val appContainers = DataProvider.processAppContainers(subscriptionsList)
    PrivyLociTheme {
        MainScreen(
            appContainers = appContainers,
            userSubscriptions = userSubscriptions,
            places = places,
            locationPermissionGranted = false,
            onLocationIconClick = {  }
        )
    }
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
fun PlacesAndAssetScreenPreview() {
    PrivyLociTheme(dynamicColor = true) {
        val (placesList, assetsList) = DataProvider.getData()
        PlacesAndAssetsScreen(placesList + assetsList)
    }
}
