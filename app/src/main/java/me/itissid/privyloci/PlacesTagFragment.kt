import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.itissid.privyloci.PlacesAdapter
import me.itissid.privyloci.R
import me.itissid.privyloci.datamodels.PlaceTag

class PlacesTagFragment(private val places: List<PlaceTag>) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_places, container, false)

        // Find the RecyclerView in the fragment's layout
        val recyclerView = view.findViewById<RecyclerView>(R.id.places_recycler_view)

        // Set up the RecyclerView with a layout manager and the adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = PlacesAdapter(places)

        return view
    }
}
