package me.itissid.privyloci

import HomeFragment
import PlacesTagFragment
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType

// TODO(Sid): Replace with real data after demo.
data class MockData(
    val places: List<PlaceTag>,
    val assets: List<PlaceTag>,
    val subscriptions: List<Subscription>
)

class MainActivity : AppCompatActivity() {


    private lateinit var appContainers: List<AppContainer>
    private lateinit var userSubscriptions: List<Subscription>
    private lateinit var places: List<PlaceTag>
    private lateinit var toolbarTitle: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Ensure this line is present

        // Load data from JSON
        val (placesList, assetsList, subscriptionsList) = loadDataFromJson()
        appContainers = processAppContainers(subscriptionsList)
        userSubscriptions = subscriptionsList.filter { it.type == SubscriptionType.USER }

        places = placesList + assetsList // Combine places and assets into one list

        toolbarTitle = findViewById(R.id.toolbar_title)

        // Set up bottom navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment(appContainers, userSubscriptions), getString(R.string.home))
                    true
                }

                R.id.nav_places -> {
                    replaceFragment(PlacesTagFragment(places), getString(R.string.places_assets))
                    true
                }

                else -> false
            }
        }

        // Load the default fragment (Home) when the app starts
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        toolbarTitle.text = title

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadDataFromJson(): Triple<List<PlaceTag>, List<PlaceTag>, List<Subscription>> {
        val jsonString = assets.open("mock_data.json").bufferedReader().use { it.readText() }
        return parseJsonToData(jsonString)
    }

    private fun parseJsonToData(jsonString: String): Triple<List<PlaceTag>, List<PlaceTag>, List<Subscription>> {
        val gson = Gson()

        // Define the type tokens for each section of the JSON
        val placeTagListType = object : TypeToken<List<PlaceTag>>() {}.type
        val subscriptionListType = object : TypeToken<List<Subscription>>() {}.type

        // Parse the JSON
        val jsonData = gson.fromJson<Map<String, Any>>(jsonString, Map::class.java)

        // Extract and parse places
        val placesJson = gson.toJson(jsonData["places"])
        val places: List<PlaceTag> = gson.fromJson(placesJson, placeTagListType)

        // Extract and parse assets
        val assetsJson = gson.toJson(jsonData["assets"])
        val assets: List<PlaceTag> = gson.fromJson(assetsJson, placeTagListType)

        // Extract and parse subscriptions
        val subscriptionsJson = gson.toJson(jsonData["subscriptions"])
        val subscriptions: List<Subscription> = gson.fromJson(subscriptionsJson, subscriptionListType)

        return Triple(places, assets, subscriptions)
    }


    private fun processAppContainers(subscriptions: List<Subscription>): List<AppContainer> {
        val appMap = mutableMapOf<String, MutableList<Subscription>>()

        for (subscription in subscriptions) {
            if (subscription.type == SubscriptionType.APP) {
                val appInfo = Gson().fromJson(subscription.appInfo, AppInfo::class.java)
                appMap.getOrPut(appInfo.appName) { mutableListOf() }.add(subscription)
            }
        }

        return appMap.map { (appName, appSubscriptions) ->
            // FIXME(Sid): The num of unique is by placeTagId and also the event type.
            val uniquePlaces = appSubscriptions.map { it.placeTagId }.distinct().count()
            val uniqueSubscriptions = appSubscriptions.size
            AppContainer(
                name = appName,
                uniquePlaces = uniquePlaces,
                uniqueSubscriptions = uniqueSubscriptions,
                subscriptions = appSubscriptions
            )
        }
    }

}