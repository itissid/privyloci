package me.itissid.privyloci

import HomeFragment
import PlacesTagFragment
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.system.Os.open
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.navigation.compose.composable

import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost

import kotlinx.coroutines.withContext
import me.itissid.privyloci.data.DataProvider


import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.service.startMyForegroundService
import me.itissid.privyloci.ui.HomeScreen
import me.itissid.privyloci.util.Logger

import me.itissid.privyloci.ui.LocationPermissionScreen
import me.itissid.privyloci.ui.PlacesAndAssetsScreen
import me.itissid.privyloci.ui.theme.PrivyLociTheme
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

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
            // TODO(Sid): Check and start all services for managing subscriptions.
            if (appContainers.isNotEmpty() || userSubscriptions.isNotEmpty()) {
                userSubscriptions.find { subscription -> subscription.isValid() && subscription.isTypeLocation()}?.let {
                        startMyForegroundService(this)
                        Logger.d("MainActivity", "Some valid location subscriptions found")
                }?.run{ Logger.d("MainActivity", "Some valid location subscriptions found") }
            }
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

private fun parseJsonToData(jsonString: String): Triple<List<PlaceTag>, List<PlaceTag>, List<Subscription>> {
    val gson = Gson()

    // Define the type tokens for each section of the JSON
    val placeTagListType = object : TypeToken<List<PlaceTag>>() {}.type
    val subscriptionListType = object : TypeToken<List<Subscription>>() {}.type

    // Parse the JSatN
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

@Preview(showBackground = true, widthDp = 320, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun PlacesAndAssetScreenPreview() {
    PrivyLociTheme {
        val context = LocalContext.current
        val jsonString =  readAssetFile(context, "mock_data.json")

        val (placesList, assetsList, subscriptionsList) = parseJsonToData(jsonString)
        PlacesAndAssetsScreen(placesList)
    }
}

fun readAssetFile(context: Context, fileName: String): String {
    return context.assets.open(fileName).use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { bufferedReader ->
            bufferedReader.readText()
        }
    }
}


@Composable
fun MainScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    places: List<PlaceTag>
) {
    val navController = rememberNavController()
    Scaffold(
        topBar = { TopBar() },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
//            composable("home") { HomeScreen(appContainers, userSubscriptions) }
//            composable("places") { PlacesAndAssetsScreen(places) }
            composable(NavItem.Home.route) {
                HomeScreen(appContainers, userSubscriptions)
            }
            composable(NavItem.Places.route) {
                PlacesAndAssetsScreen(places)
            }
            // Add other destinations as needed
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    TopAppBar(
        title = { Text("Privy Loci") },
        actions = {
            IconButton(onClick = { /* Handle menu action */ }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    )
}

@Composable
fun ScrollContent(innerPadding: PaddingValues) {
    TODO("Not yet implemented")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        NavItem.Home,
        NavItem.Places,
        // Add other items as needed
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute(navController) == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up all the screens up to but not including the start destination.
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

// NavItem.kt

sealed class NavItem(val route: String, val icon: ImageVector, val title: String) {
    object Home : NavItem("home", Icons.Filled.Home, "Home")
    object Places : NavItem("places", Icons.Filled.Place, "Places")
    // Add other items as needed
}


@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial=null )
    return navBackStackEntry?.destination?.route
}