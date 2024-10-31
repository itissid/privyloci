package me.itissid.privyloci

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable

import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted

import me.itissid.privyloci.data.DataProvider

import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.ui.HomeScreen
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

import me.itissid.privyloci.ui.PlacesAndAssetsScreen
import me.itissid.privyloci.ui.theme.PrivyLociTheme
import dagger.hilt.android.AndroidEntryPoint
import me.itissid.privyloci.ui.AdaptiveIcon
import me.itissid.privyloci.ui.LocationPermissionRationaleDialogue
import me.itissid.privyloci.ui.PermissionDeniedScreen

// TODO(Sid): Replace with real data after demo.
data class MockData(
    val places: List<PlaceTag>,
    val assets: List<PlaceTag>,
    val subscriptions: List<Subscription>
)

const val TAG = "me.itissid.privyloci.MainActivity"
// move to jetpack compose
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val locationPermissionState = LocationPermissionState(this) {
//            if (it.hasPermission()) {
//                Logger.w("MainActivity", "Location permission granted")
//            } else {
//                Logger.w("MainActivity", "Location permission denied")
//            }
//        }
        val (placesList, assetsList, subscriptionsList) = DataProvider.getData()
        val places = placesList + assetsList
        val userSubscriptions = subscriptionsList.filter { it.type == SubscriptionType.USER }
        val appContainers = DataProvider.processAppContainers(subscriptionsList)
        setContent {
            PrivyLociTheme {
                MainScreenWrapper(
                    appContainers = appContainers,
                    userSubscriptions = userSubscriptions,
                    places = places,
                )
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreenWrapper(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    places: List<PlaceTag>,
) {
    val context = LocalContext.current as Activity

    var rationaleState by remember { mutableStateOf(false) }

    val foregroundLocationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (!foregroundLocationPermissionState.allPermissionsGranted) {
        Log.d(TAG, "PERMISSION NOT GRANTED")
    } else {
        Log.d(TAG, "PERMISSION GRANTED")
    }
    if (foregroundLocationPermissionState.shouldShowRationale) {
        Log.d(TAG, "SHOULD SHOW RATIONALE")
    } else {
        Log.d(TAG, "SHOULD NOT SHOW RATIONALE")
    }

    if (rationaleState) {
        Log.d(TAG, "RATIONALE STATE")
    } else {
        Log.d(TAG, "NO RATIONALE STATE")
    }

    // Modify the rationalState in the click handler
    val onLocationIconClick = {
        if (foregroundLocationPermissionState.shouldShowRationale) {
            Log.w(TAG, "Setting rationale state to true")
            rationaleState = true
        } else {
            Log.w(TAG, "Launching multiple permission request")
            foregroundLocationPermissionState.launchMultiplePermissionRequest()
            // TODO: The system can choose to ignore this request. Raise a warning for the user.
        }
    }

    // read the state modified in the click handler
    if (rationaleState) {
        LocationPermissionRationaleDialogue(
            onConfirm = {
                Log.w(TAG, "Launching multiple permission request from onConfirm")
                foregroundLocationPermissionState.launchMultiplePermissionRequest()
                rationaleState = false
            },
            onDismiss = {
                rationaleState = false
            }
        )
    }
    // Now we want to
    MainScreen(
        appContainers,
        userSubscriptions,
        places,
        foregroundLocationPermissionState.allPermissionsGranted,
        onLocationIconClick
    )
}


@Composable
fun MainScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    places: List<PlaceTag>,
    locationPermissionGranted: Boolean,
    onLocationIconClick: () -> Unit
) {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopBar(
                locationPermissionGranted = locationPermissionGranted,
                onLocationIconClick = onLocationIconClick
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavItem.Home.route) {
                HomeScreen(
                    appContainers,
                    userSubscriptions,
                    locationPermissionGranted,
                    onLocationIconClick
                )
            }
            composable(NavItem.Places.route) {
                PlacesAndAssetsScreen(places)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    locationPermissionGranted: Boolean = false,
    onLocationIconClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Privy Loci") },
        actions = {
            if (locationPermissionGranted) {
                IconButton(onClick = onLocationIconClick) {
                    AdaptiveIcon(locationPermissionGranted = true)
                }
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            } else {
                IconButton(onClick = {/*TODO: Explainer dial that is dismissable*/ }) {
                    AdaptiveIcon(locationPermissionGranted = false)
                }
                Icon(Icons.Filled.Menu, contentDescription = "Menu")

            }

        }
    )
}

@Composable
fun ScrollContent(innerPadding: PaddingValues) {
    TODO("Not yet implemented")
}

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