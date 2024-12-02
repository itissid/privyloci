package me.itissid.privyloci

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi

import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.ui.HomeScreen
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import me.itissid.privyloci.ui.PlacesAndAssetsScreen
import me.itissid.privyloci.ui.theme.PrivyLociTheme
import dagger.hilt.android.AndroidEntryPoint
import me.itissid.privyloci.data.DataProvider.processAppContainers
import me.itissid.privyloci.service.FG_NOTIFICATION_DISMISSED
import me.itissid.privyloci.ui.AdaptiveIcon
import me.itissid.privyloci.ui.LocationPermissionRationaleDialogue
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.ForegroundPermissionRationaleState
import me.itissid.privyloci.viewmodels.MainViewModel
import me.itissid.privyloci.viewmodels.RationaleState

// TODO(Sid): Replace with real data after demo.
data class MockData(
    val places: List<PlaceTag>,
    val assets: List<PlaceTag>,
    val subscriptions: List<Subscription>
)

const val TAG = "MainActivity"

// move to jetpack compose
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val wasFGNotificationDismissed = mutableStateOf(false)
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivyLociTheme {
                MainScreenWrapper(
                    wasFGNotificationDismissed = wasFGNotificationDismissed.value,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            Logger.v(TAG, "Intent received: $it.")
            val isServiceStopped = it.getBooleanExtra(FG_NOTIFICATION_DISMISSED, false)
            Logger.v(TAG, "handleIntent: FG_NOTIF_DISMISSED = $isServiceStopped")

            wasFGNotificationDismissed.value = isServiceStopped
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreenWrapper(wasFGNotificationDismissed: Boolean = false, viewModel: MainViewModel) {
    val context = LocalContext.current as Activity
    val lifecycleOwner = LocalLifecycleOwner.current


    var fgPermissionRationaleState by
    remember { mutableStateOf<ForegroundPermissionRationaleState?>(null) }
    var fgRunning by remember { mutableStateOf(false) }

    val userVisitedPermissionLauncherPreference by viewModel.userVisitedPermissionLauncher.collectAsState()
    val userPausedLocationCollection by viewModel.userPausedLocationCollection.collectAsState()

    viewModel.isServiceRunning.observe(lifecycleOwner) { isRunning ->
        fgRunning = isRunning
    }


    var fgServiceNotificationUserDismissed by remember(wasFGNotificationDismissed) {
        mutableStateOf(
            wasFGNotificationDismissed
        )
    }
    Logger.v(
        TAG,
        "wasFGNotificationDismissed: $wasFGNotificationDismissed, fgServiceNotificationUserDismissed: $fgServiceNotificationUserDismissed"
    )
    var deactivateFGByUser by remember { mutableStateOf(false) }


    // TODO: Encapsulate the permision code in its own class.
    // TODO: Ask for background permissions if I don't take the foreground permissions route.
    val foregroundLocationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS // Add this permission
        )
    ) { it -> Logger.i(TAG, it.entries.joinToString(separator = "\n")) }

    if (fgRunning) {
        Logger.v(TAG, "Privy FG Service is running")
    } else {
        Logger.v(TAG, "Privy FG Service is not running")
    }

    if (!foregroundLocationPermissionState.allPermissionsGranted) {
        Logger.v(TAG, "PERMISSION NOT GRANTED")
    } else {
        Logger.v(TAG, "PERMISSION GRANTED")
    }
    if (foregroundLocationPermissionState.shouldShowRationale) {
        Logger.v(TAG, "SHOULD SHOW RATIONALE")
    } else {
        Logger.v(TAG, "SHOULD NOT SHOW RATIONALE")
    }

    if (fgPermissionRationaleState == null) {
        Logger.v(TAG, "RATIONALE STATE")
    } else {
        Logger.v(TAG, "NO RATIONALE STATE")
    }

    fgPermissionRationaleState?.let {
        LocationPermissionRationaleDialogue(
            onConfirm = {
                Logger.w(TAG, "Launching multiple permission request from onConfirm")
                when (it.reason) {
                    RationaleState.LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN -> {
                        try {
                            foregroundLocationPermissionState.launchMultiplePermissionRequest()
                            // TODO: At this point  should ALWAYS launch the permissions since the RationaleState is set to this only if shouldShowRationale is true.
                            //  But testing is needed. Setting this user preference guards against trying to repeatedly launch the permission request, because in android
                            // 14 it does nothing.
                            viewModel.setUserVisitedPermissionLauncherPreference(true) //
                        } finally {
                            fgPermissionRationaleState = null
                        }
                    }

                    RationaleState.MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED -> {
                        try {
                            foregroundLocationPermissionState.launchMultiplePermissionRequest()
                            viewModel.setUserVisitedPermissionLauncherPreference(true) //
                        } finally {
                            fgPermissionRationaleState = null
                        }
                    }

                    RationaleState.VISIT_SETTINGS -> {
                        try {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        } finally {
                            fgPermissionRationaleState = null
                        }
                    }

                    RationaleState.PAUSE_LOCATION_COLLECTION -> {
                        // TODO: set the pause flag that shuts down location collection.
                        try {
                            viewModel.setUserPausedLocationCollection(true)
                        } finally {
                            fgPermissionRationaleState = null
                        }
                    }

                    RationaleState.RESUME_LOCATION_COLLECTION -> {
                        try {
                            viewModel.setUserPausedLocationCollection(false)
                        } finally {
                            fgPermissionRationaleState = null
                        }
                    }
                }
            },
            onDismiss = {
                Logger.v(TAG, "User dismissed the rationale dialogue")
                fgPermissionRationaleState = null
            },
            message = it.rationaleText,
            proceedText = it.proceedButtonText,
            dismissText = it.dismissButtonText
        )
    }

    // Modify the rationalState in the spirit of the Unidirectional flow model
    val onLocationIconClick = {
        if (!foregroundLocationPermissionState.allPermissionsGranted) {
            if (foregroundLocationPermissionState.shouldShowRationale) {
                Logger.v(TAG, "Setting rationale state to be shown")
                fgPermissionRationaleState = ForegroundPermissionRationaleState(
                    RationaleState.LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN,
                    rationaleText = "In order to use PrivyLoci's location features,  please grant access by accepting the location permission dialog."
                )
                viewModel.setUserVisitedPermissionLauncherPreference(false) // Remove the user preference since the system is allowing us to show the rationale.
            } else {
                if (!userVisitedPermissionLauncherPreference) {// User has not visited permission launcher code.
                    Logger.v(TAG, "Setting launch flag for multiple permission request")
                    fgPermissionRationaleState =
                        ForegroundPermissionRationaleState(
                            RationaleState.MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED,
                            rationaleText = "In order to use PrivyLoci's location feature, press 'Proceed' and grant the 'While in Use' location permissions."
                        )

                } else { //
                    // Multiple permissions have been launched once. Now the user will always be directed to the settings when clicking this..
                    Logger.v(TAG, "Setting rationale state to visit settings")
                    fgPermissionRationaleState =
                        ForegroundPermissionRationaleState(
                            RationaleState.VISIT_SETTINGS,
                            rationaleText = "In order to use PrivyLoci's location features, press 'Proceed', and in the settings and  select 'Allow only while using the app'"
                        )
                }
            }
        } else {
            // Granted location permission, we can choose to "pause" the collection of the location data and via a user dialogue. Set a preference flag.
            // N2S: We can also choose to revoke the permissions, but that may make it too difficult for the user to reactivate it on Google's flavor of permission for devices.
            // Alert dialogue to do this
            if (!userPausedLocationCollection) {
                fgPermissionRationaleState =
                    ForegroundPermissionRationaleState(
                        RationaleState.PAUSE_LOCATION_COLLECTION,
                        rationaleText = "You can Proceed App wide location collection by pressing proceed.",
                        proceedButtonText = "Pause",
                        dismissButtonText = "Dismiss"
                    )
            } else {
                fgPermissionRationaleState =
                    ForegroundPermissionRationaleState(
                        RationaleState.RESUME_LOCATION_COLLECTION,
                        rationaleText = "You can resume App wide location collection by pressing proceed.",
                        proceedButtonText = "Resume",
                        dismissButtonText = "Dismiss"
                    )
            }
        }
    }

    /*End logic to deal with removed FG notification */

    // TODO: Consider using a viewmodel to get the data from the daos.
    val database = MainApplication.database

    val subscriptionDao = database.subscriptionDao()
    val placeTagDao = database.placeTagDao()
    // Coro for the win!
    val places by placeTagDao.getAllPlaceTags().collectAsState(initial = emptyList())
    val subscriptions by subscriptionDao.getAllSubscriptions().collectAsState(initial = emptyList())
    Logger.v(TAG, "Places: ${places.size}")
    Logger.v(TAG, "Subscriptions: ${subscriptions.size}")
    val userSubscriptions = subscriptions.filter { it.type == SubscriptionType.USER }
    val appContainers = processAppContainers(subscriptions)

    MainScreen(
        appContainers,
        userSubscriptions,
        places,
        foregroundLocationPermissionState.allPermissionsGranted && !userPausedLocationCollection,
        onLocationIconClick
    )

//    if (foregroundLocationPermissionState.allPermissionsGranted) {
//        // if the service is not started and the user has not dismissed the FG notification call the service
//        // if the service is not started and the user has dismissed the the FG notification, show the user a dialogue which starts the FG service on confirmation.
//        // TODO: On starting the service I want to show some of the data about how many subscriptions are active
//        // and being tracked.
//        if (!fgRunning) {
//            if (fgServiceNotificationUserDismissed) {
//                // User swipes left and dismisses the foreground service
//                if (!deactivateFGByUser) {
//                   Logger.v(TAG, "Displaying the FG service notification dismissed dialogue")
//                    FGDismissedStoppedDialog(onDismiss = {
//                       Logger.v(
//                            TAG,
//                            "User dismissed the FG service notification and does not want to start  it again."
//                        )
//                        deactivateFGByUser = true
//                    }, onRestartService = {
//                       Logger.v(TAG, "Attempting to start the FG service again.")
//                        fgServiceNotificationUserDismissed = false
//                        startPrivyForegroundService(context)
//                    })
//                }
//            }
////            } else {
////                // Service is not running due to some other reason like killed by the system
////               Logger.v(TAG, "Attempting to start the FG service as normal")
////                startPrivyForegroundService(context)
////            }
//        } else {
//           Logger.v(TAG, "Privy FG Service detected as already running")
//        }
//    } else if (!foregroundLocationPermissionState.allPermissionsGranted) {
//        // call if the service is not stopped already.
//        // TODO: Warn the user after some time(probably like in a timer) that the service is not running
//        // because the permission is not granted.
//        if (fgRunning) {
//            stopPrivyForegroundService(context)
//        }
//    }
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
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("createEvent") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event")
            }
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
                IconButton(onClick = onLocationIconClick) {
                    AdaptiveIcon(locationPermissionGranted = locationPermissionGranted)
                }
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
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