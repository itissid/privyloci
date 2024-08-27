package me.itissid.privyloci

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import me.itissid.privyloci.datamodels.PlaceTag

class PlacesAdapter(private val places: List<PlaceTag>) :
    RecyclerView.Adapter<PlacesAdapter.PlacesViewHolder>() {

    class PlacesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeName: TextView = itemView.findViewById(R.id.place_name)
        val placeType: TextView = itemView.findViewById(R.id.place_type)
        val cardView: MaterialCardView = itemView.findViewById(R.id.place_card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlacesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.place_card, parent, false)
        return PlacesViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlacesViewHolder, position: Int) {
        val place = places[position]
        holder.placeName.text = place.name
        holder.placeType.text = place.type.name // Display the enum name (PLACE, ASSET, etc.)

        // Handle card click, you might want to open a detailed view or perform a CRUD operation
        holder.cardView.setOnClickListener {
            // Perform an action like showing details or editing the place
        }
    }

    override fun getItemCount() = places.size
}