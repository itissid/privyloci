package me.itissid.privyloci.ui

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription


@Composable
fun HomeScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>
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
                }
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
            SubscriptionCard(subscription = subscription)
        }
    }
}

@Composable
fun PlacesAndAssetsScreen(places: List<PlaceTag>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(places) { place ->
            PlaceCard(placeTag = place)
        }
    }
}
