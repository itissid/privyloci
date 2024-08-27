import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.itissid.privyloci.AppAdapter
import me.itissid.privyloci.R
import me.itissid.privyloci.UserSubscriptionAdapter
import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.Subscription

class HomeFragment(private val appContainers: List<AppContainer>, private val userSubscriptions: List<Subscription>) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find the RecyclerView in the fragment's layout
        val appRecyclerView = view.findViewById<RecyclerView>(R.id.app_recycler_view)

        // Set up the RecyclerView with a layout manager and the adapter for App subscriptions
        appRecyclerView.layoutManager = LinearLayoutManager(context)
        appRecyclerView.adapter = AppAdapter(appContainers)

        // If you want to handle user subscriptions separately, set up another RecyclerView here
         val userRecyclerView = view.findViewById<RecyclerView>(R.id.user_recycler_view)
         userRecyclerView.layoutManager = LinearLayoutManager(context)
         userRecyclerView.adapter = UserSubscriptionAdapter(userSubscriptions)

        return view
    }
}
