package me.itissid.privyloci

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.itissid.privyloci.datamodels.AppContainer

class AppAdapter(private val apps: List<AppContainer>) :
    RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView.findViewById(R.id.app_name)
        val menuMore: ImageView = itemView.findViewById(R.id.menu_more)
        val subscriptionDetails: TextView = itemView.findViewById(R.id.subscription_details)
        val subscriptionRecyclerView: RecyclerView = itemView.findViewById(R.id.subscription_recycler_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_card, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name
        holder.subscriptionDetails.text = "${app.uniquePlaces} Places, ${app.uniqueSubscriptions} Subscriptions"

        // Handle the menu click (more options)
        holder.menuMore.setOnClickListener {
            // Show menu with options to delete or pause all subscriptions
        }

        // Handle card click to expand and show subscriptions
        holder.itemView.setOnClickListener {
            val isExpanded = app.isExpanded
            app.isExpanded = !isExpanded
            holder.subscriptionRecyclerView.visibility = if (app.isExpanded) View.VISIBLE else View.GONE
        }

        // Set up the nested RecyclerView with SubscriptionAdapter
        if (app.isExpanded) {
            holder.subscriptionRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.subscriptionRecyclerView.adapter = SubscriptionAdapter(app.subscriptions, false)
            holder.subscriptionRecyclerView.visibility = View.VISIBLE
        } else {
            holder.subscriptionRecyclerView.visibility = View.GONE
        }
    }

    override fun getItemCount() = apps.size
}
