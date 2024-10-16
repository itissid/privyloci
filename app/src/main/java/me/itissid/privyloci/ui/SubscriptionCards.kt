package me.itissid.privyloci.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.EventType
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppCard(
    appContainer: AppContainer,
    isExpanded: Boolean,
    onMenuClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // App Name and Menu
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appContainer.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }
            }

            // Subscription Details
            Text(
                text = "${appContainer.uniquePlaces} Places, ${appContainer.uniqueSubscriptions} Subscriptions",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Expandable List of Subscriptions
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    appContainer.subscriptions.forEach { subscription ->
                        SubscriptionCard(subscription = subscription)
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceCard(placeTag: PlaceTag) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Place Name
            Text(
                text = placeTag.name,
                style = MaterialTheme.typography.headlineSmall
            )
            // Place Type
            Text(
                text = placeTag.type.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SubscriptionCard(subscription: Subscription) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Place Name (Replace with actual place name if available)
            Text(
                text = "${subscription.placeTagName}(id: ${subscription.placeTagId})",
                style = MaterialTheme.typography.headlineSmall
            )
            // Event Type
            Text(
                text = when(subscription.eventType) {
                    EventType.GEOFENCE_ENTRY-> "Entry Alert"
                    EventType.GEOFENCE_EXIT -> "Exit Alert"
                    EventType.TRACK_BLE_ASSET_DISCONNECTED -> "Location Tracked after Disconnection"
                    EventType.TRACK_BLE_ASSET_NEARBY -> "Tracking when in range, but not connected"
                    EventType.QIBLA_DIRECTION_PRAYER_TIME -> "Direction to Kibla"
                    EventType.DISPLAY_PINS_MAP_TILE -> "Displaying Pins on Map"
                    else -> "Unknown Event"
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            // Date and Time
            Text(
                text = "Date: ${formatDate(subscription.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Helper function to format the date
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}