package me.itissid.privyloci

import HomeFragment
import PlacesTagFragment
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.service.startMyForegroundService
import me.itissid.privyloci.util.Logger

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

        // TODO(Sid): Remove Load data from JSON.
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
                    val fragment = HomeFragment.newInstance(ArrayList(appContainers), ArrayList(userSubscriptions))
                    replaceFragment(fragment, getString(R.string.home))
                    true
                }

                R.id.nav_places -> {
                    val fragment = PlacesTagFragment.newInstance(ArrayList(places))
                    replaceFragment(fragment, getString(R.string.places_assets))
                    true
                }

                else -> false
            }
        }

        // Load the default fragment (Home) when the app starts
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
        // Start the foreground service if there are subscriptions that require sensors.
        // Check and request notification permission
        if (!checkNotificationPermission()) {
            requestNotificationPermission()
        } else {
            // Permission is already granted, start the service
            startMyForegroundService(this)
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
    // Foreground service permission related code
    companion object {
        const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Permission is automatically granted on devices lower than Android 13
            true
        }
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_POST_NOTIFICATIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, you can start your foreground service here
                    startMyForegroundService(this)
                } else {
                    // Permission denied, you can show a message or handle accordingly
                    Logger.w("MainActivity", "POST_NOTIFICATIONS permission denied")
                }
            }
        }
    }

}