package me.itissid.privyloci

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.itissid.privyloci.R
import me.itissid.privyloci.datamodels.Subscription

class UserSubscriptionAdapter(private val subscriptions: List<Subscription>) :
    RecyclerView.Adapter<UserSubscriptionAdapter.UserSubscriptionViewHolder>() {

    class UserSubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.place_name)
        val eventType: TextView = itemView.findViewById(R.id.event_type)
        val dateTime: TextView = itemView.findViewById(R.id.date_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_subscription_card, parent, false)
        return UserSubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserSubscriptionViewHolder, position: Int) {
        val subscription = subscriptions[position]
        holder.placeName.text = "Place ID: ${subscription.placeTagId}" //TODO(Sid): Replace with actual place name when hooking in the data model
        holder.eventType.text = subscription.eventType.name
        holder.dateTime.text = subscription.formattedDate // Format as needed
    }

    override fun getItemCount() = subscriptions.size
}
