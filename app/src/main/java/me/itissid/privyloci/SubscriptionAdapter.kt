package me.itissid.privyloci

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.itissid.privyloci.datamodels.EventType
import me.itissid.privyloci.datamodels.Subscription
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName


class SubscriptionAdapter(
    private val subscriptions: List<Subscription>,
    private val isUserSubscription: Boolean // Differentiates between user and app subscriptions
) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tile_name)
        val subscriptionInfo: TextView = itemView.findViewById(R.id.tile_subscription_count)
        val lastUpdated: TextView = itemView.findViewById(R.id.tile_last_updated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.subscription_tile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subscription = subscriptions[position]
        if (isUserSubscription) {
            // For user subscriptions (Places/Assets)
            holder.name.text = subscription.placeTagId.toString() // Replace with actual place/asset name lookup
            holder.subscriptionInfo.text = when (subscription.event.type) {
                EventType.GEOFENCE_ENTRY -> "Geofence Entry Event"
                EventType.GEOFENCE_EXIT -> "Geofence Exit Event"
                EventType.TRACK_BLE_ASSET_DISCONNECTED -> "BLE Asset Disconnected"
                EventType.TRACK_BLE_ASSET_NEARBY -> "BLE Asset Nearby"
                EventType.QIBLA_DIRECTION_PRAYER_TIME -> "Qibla Direction Calculated"
                EventType.DISPLAY_PINS_MAP_TILE -> "Displaying Pins on Map"
                else -> "Unknown Event"
            }
            holder.lastUpdated.text = "Last Updated: ${subscription.createdAt}" // Convert timestamp to date
        } else {
            // For app subscriptions
            val appInfo = Gson().fromJson(subscription.appInfo, AppInfo::class.java)
            holder.name.text = appInfo.appName
            holder.subscriptionInfo.text = "Active Subscriptions: ${subscriptions.size}"
            holder.lastUpdated.text = "Last Updated: ${subscription.createdAt}" // Convert timestamp to date
        }
    }


    override fun getItemCount() = subscriptions.size
}

// Example AppInfo data class for parsing the appInfo JSON
data class AppInfo(
    @SerializedName("app_name") val appName: String,
    @SerializedName("app_id") val appId: String
)
