package me.itissid.privyloci

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.itissid.privyloci.data.DataProvider
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val (placesList, assetsList, subscriptionsList) = DataProvider.getData()
    val places = placesList + assetsList
    val userSubscriptions = subscriptionsList.filter { it.type == SubscriptionType.USER }
    val appContainers = DataProvider.processAppContainers(subscriptionsList)

    MainScreen(
        appContainers = appContainers,
        userSubscriptions = userSubscriptions,
        places = places
    )
}