package me.itissid.privyloci.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.itissid.privyloci.MainScreen
import me.itissid.privyloci.data.DataProvider
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.ui.theme.PrivyLociTheme


@Preview(showBackground = true)
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
        )
    }
}