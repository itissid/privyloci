package me.itissid.privyloci

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost

import me.itissid.privyloci.datamodels.AppContainer
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.Subscription
import me.itissid.privyloci.datamodels.SubscriptionType
import me.itissid.privyloci.ui.HomeScreen

import me.itissid.privyloci.ui.theme.PrivyLociTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.itissid.privyloci.data.DataProvider.processAppContainers
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.toDomain
//import me.itissid.privyloci.service.FG_NOTIFICATION_DISMISSED
import me.itissid.privyloci.service.startPrivyForegroundService
import me.itissid.privyloci.service.stopPrivyForegroundService
import me.itissid.privyloci.ui.AdaptiveIcon
import me.itissid.privyloci.ui.BlePermissionHandler
import me.itissid.privyloci.ui.LocationPermissionHandler
import me.itissid.privyloci.ui.PlacesAndAssetsScreen
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.BTDevicesStatus
import me.itissid.privyloci.viewmodels.BleDevicesViewModel
import me.itissid.privyloci.viewmodels.BTPermissionViewModel
import me.itissid.privyloci.viewmodels.LocationPermissionViewModel
import me.itissid.privyloci.viewmodels.LocationPermissionState
import me.itissid.privyloci.viewmodels.MainViewModel
import me.itissid.privyloci.viewmodels.PlaceTagsWithDevicesState
import org.maplibre.android.MapLibre

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
    //    private val wasFGNotificationDismissed = mutableStateOf(false)
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrivyLociTheme {
                MainScreenWrapper(viewModel)
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
            Logger.i(TAG, "Intent received: $it.")
//            val potentiallyFGServiceStopped = it.getBooleanExtra(FG_NOTIFICATION_DISMISSED, false)
//            Logger.v(TAG, "handleIntent: FG_NOTIF_DISMISSED = $potentiallyFGServiceStopped")
//            viewModel.setUserPausedLocationCollection(potentiallyFGServiceStopped)
//            wasFGNotificationDismissed.value = potentiallyFGServiceStopped
        }
    }
}

@Composable
fun MainScreenWrapper(mainViewModel: MainViewModel) {
    Logger.v(TAG, "MainScreenWrapperRedo()")
    // N2S: We don't push this down to SubscriptionCard or PlacesAndAssetScreen
    // because location permissions will be required
    val database = MainApplication.database

    val subscriptionDao = database.subscriptionDao()
    val placeTagDao = database.placeTagDao()
    // Coro for the win: Observe changes don't reevaluate.
    val places by placeTagDao.getAllPlaceTags().map { entities -> entities.map { it.toDomain() } }
        .collectAsState(initial = emptyList())
    val subscriptions by subscriptionDao.getAllSubscriptions().collectAsState(initial = emptyList())
    Logger.v(TAG, "Places: ${places.size}")
    Logger.v(TAG, "Subscriptions: ${subscriptions.size}")
    val userSubscriptions = subscriptions.filter { it.type == SubscriptionType.USER }
    val appContainers = processAppContainers(subscriptions)
    val needsLocationPermissions = remember(userSubscriptions, appContainers, places) {
        // appContainers.any { it.isTypeLocation() } || TODO implement me for 3p apps
        userSubscriptions.any { it.isTypeLocation() } ||
                places.any { it.isTypeLocation() }
    }
    val needsBLEPermissions = remember(userSubscriptions, appContainers, places) {
        userSubscriptions.any { it.requiresTypeBLE() } ||
                places.any { it.isTypeBLE() }

    }
    var btDevicesRescanHandler: () -> Unit = {}
    var btPermissionHandler: () -> Unit = {}
    var btDevicesSelectedHandler: (PlaceTag, InternalBtDevice) -> Unit = { _, _ -> }
    var locationPermissionHandler: () -> Unit = {}
    var btDeviceStatus: BTDevicesStatus? = null
    var locationPermissionState: LocationPermissionState? = null
    var placeTagsWithSelectedDeviceStatus: PlaceTagsWithDevicesState? = null
    Logger.i(TAG, "App needsBLEPermissions?: $needsBLEPermissions")
    if (needsBLEPermissions) {
        val BTPermissionViewModel: BTPermissionViewModel = hiltViewModel()
        val blePermissionGranted: Boolean by BTPermissionViewModel.blePermissionGranted.collectAsState()
        BlePermissionHandler(BTPermissionViewModel)
        btPermissionHandler = remember(blePermissionGranted) {
            // made this a remember (see https://chatgpt.com/share/679a6932-6bb8-800c-ad17-a363b04cef36)
            // because it can reliably re-trigger the recomposition of PlacesAndAssetsScreen
            // Before this I was passing a blePermissionGranted to PlacesAndAssetScreen
            {
                Logger.v(TAG, "BLE Permission icon clicked")
                BTPermissionViewModel.onBleIconClicked()
            }
        }
        //if (blePermissionGranted) N2S: Had this check withiut it there was a permission exception, but it was adding too much complextity in the uI, handled it
        // moving the check to the view model in its init {} in BleDevicesViewModel to deal with it more easily.
        val bleDevicesViewModel: BleDevicesViewModel = hiltViewModel()
        btDeviceStatus = bleDevicesViewModel.bleDevices.collectAsState().value
        btDevicesSelectedHandler = { placeTag, selectedDevice ->
            bleDevicesViewModel.selectDeviceForPlaceTag(
                placeTag,
                selectedDevice.address
            )
        }
        placeTagsWithSelectedDeviceStatus =
            bleDevicesViewModel.placeTagsWithSelectedDevicesState.collectAsState().value
        Logger.v(TAG, "placeTagsWithSelectedDeviceStatus: $placeTagsWithSelectedDeviceStatus")
        val coroutineScope = rememberCoroutineScope()

        btDevicesRescanHandler =
            {
                coroutineScope.launch {
                    Logger.v(
                        "PlacesAndAssetsScreen",
                        "Loading Bonded devices from rescan handler"
                    )
                    // This is on the UI thread main thread.
                    bleDevicesViewModel.loadBondedBleDevices()
                }
                Unit
            }
    }
    Logger.v(TAG, "App needs location permissions? $needsLocationPermissions")
    if (needsLocationPermissions) {
        val locationPermissionViewModel: LocationPermissionViewModel = hiltViewModel()

        LocationPermissionHandler(locationPermissionViewModel)

        locationPermissionState =
            locationPermissionViewModel.locationPermissionState.collectAsState().value
        locationPermissionHandler = remember(locationPermissionState.permissionsGranted) {
            {
                locationPermissionViewModel.mutateLocationCollectionState()
            }
        }

    }


    Logger.v(TAG, "locationPermissionState: $locationPermissionState")
    MainScreen(
        appContainers = appContainers, // or real data
        userSubscriptions = userSubscriptions,
        places = places,
        locationPermissionState = locationPermissionState,
        //        locationPermissionGranted = locationPermissionViewModel.locationPermissionGranted.collectAsState().value,
        locationPermissionHandler = locationPermissionHandler,
        btDeviceState = btDeviceStatus,
        btPermissionHandler = btPermissionHandler,
        btDevicesRescanHandler = btDevicesRescanHandler,
        btDevicesSelectedHandler = btDevicesSelectedHandler,
        placeTagsWithSelectedDeviceStatus = placeTagsWithSelectedDeviceStatus
    )
    LaunchPrivyForeGroundService(
        isRunningFlow = mainViewModel.isServiceRunning,
        locationPermissionState = locationPermissionState
    )

}

@Composable
fun LaunchPrivyForeGroundService(
    isRunningFlow: StateFlow<Boolean>,
    locationPermissionState: LocationPermissionState?
) {
    if (locationPermissionState == null) {
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isRunning by isRunningFlow.collectAsState()
    val context = LocalContext.current
    Logger.v(
        TAG,
        "LaunchPrivyForeGroundService: isRunning=$isRunning, locationPermissionState=$locationPermissionState"
    )
    LaunchedEffect(
        isRunning,
        locationPermissionState.permissionsGranted,
        locationPermissionState.isPaused
    ) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            when {
                !locationPermissionState.permissionsGranted -> stopPrivyForegroundService(context)
                else -> handleServiceStartStop(isRunning, context, locationPermissionState.isPaused)
            }
        }
    }
}

private fun handleServiceStartStop(
    isRunning: Boolean,
    context: Context,
    isPaused: Boolean
) {
    if (!isPaused) {
        if (!isRunning) {
            Logger.v(TAG, "Attempting to start the FG service.")
            startPrivyForegroundService(context)
        } else {
            Logger.w(
                TAG,
                "User has not paused location collection but FG service detected running"
            )
        }
    } else { //
        if (isRunning) {
            Logger.v(TAG, "Attempting to stop the FG service")
            stopPrivyForegroundService(context)
        } else {
            Logger.w(
                TAG,
                "User Paused Location Collection but FG service is not detected"
            )
        }
    }
}
@Composable
fun MainScreen(
    appContainers: List<AppContainer>,
    userSubscriptions: List<Subscription>,
    places: List<PlaceTag>,
    locationPermissionState: LocationPermissionState?,
    locationPermissionHandler: () -> Unit,
    btPermissionHandler: () -> Unit,
    btDevicesRescanHandler: () -> Unit,
    placeTagsWithSelectedDeviceStatus: PlaceTagsWithDevicesState? = null,
    btDeviceState: BTDevicesStatus? = null,
    btDevicesSelectedHandler: (PlaceTag, InternalBtDevice) -> Unit,
) {
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopBar(
                locationPermissionState = locationPermissionState,
                onLocationIconClick = locationPermissionHandler
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
                    locationPermissionState = locationPermissionState,
                    locationPermissionHandler
                )
            }
            composable(NavItem.Places.route) {
                PlacesAndAssetsScreen(
                    places,
                    btPermissionHandler,
                    btDevicesRescanHandler,
                    placeTagsWithSelectedDeviceStatus,
                    btDevicesSelectedHandler,
                    btDeviceState,
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    locationPermissionState: LocationPermissionState?,
    onLocationIconClick: () -> Unit
) {
    Logger.v("TopBar", "locationPermissionState = ${locationPermissionState?.permissionsGranted}")
    val locationPermissionIsGoodState =
        locationPermissionState == null || (locationPermissionState.permissionsGranted && !locationPermissionState.isPaused)
    TopAppBar(
        title = { Text("Privy Loci") },
        actions = {
                IconButton(onClick = onLocationIconClick) {
                    AdaptiveIcon(locationPermissionGranted = locationPermissionIsGoodState)
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