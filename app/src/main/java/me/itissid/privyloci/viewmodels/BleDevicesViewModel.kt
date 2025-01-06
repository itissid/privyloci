package me.itissid.privyloci.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Immutable
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.datamodels.toDomain
import me.itissid.privyloci.datamodels.toEntity
import me.itissid.privyloci.util.BTDevicesRepository
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.InternalBTProfile.Companion.profileOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject


inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
// See https://stackoverflow.com/a/73311814
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
// See https://stackoverflow.com/a/73311814
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key)
}

sealed class PlaceTagsWithDevicesState {
    data object NoPlaceHasBTType : PlaceTagsWithDevicesState()
    data object PermissionNotGranted : PlaceTagsWithDevicesState()
    data object BtNotEnabled : PlaceTagsWithDevicesState()
    data object BTDevicesNotFound : PlaceTagsWithDevicesState()
    data object Loading : PlaceTagsWithDevicesState()
    data class Success(val placeTagsWithDevices: List<Pair<PlaceTag, InternalBtDevice?>>) :
        PlaceTagsWithDevicesState()
}

sealed class BTDevicesStatus {
    data object Loading : BTDevicesStatus()
    data object NoPermissionGranted : BTDevicesStatus()
    data object BtNotEnabled : BTDevicesStatus()
    data class BTDevicesFound(val devices: Set<InternalBtDevice>) : BTDevicesStatus()
}

@HiltViewModel
class BleDevicesViewModel @Inject constructor(
    private val application: Application,
    private val placeTagDao: PlaceTagDao,
    private val btRepository: BTDevicesRepository,
    btPermissionRepository: BTPermissionRepository,
    // Possibly inject BluetoothManager or handle via system service
) : AndroidViewModel(application) {
    private val TAG = javaClass.simpleName

    private val bluetoothManager: BluetoothManager by lazy {
        application.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    private var btProfiles: MutableMap<InternalBTProfile, BluetoothProfile?> = mutableMapOf()

    private var _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter.isEnabled)
    private val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()


    data class BTDeviceWrapper(val devices: Set<InternalBtDevice>)

    private var _bleDevicesAlt = MutableStateFlow(BTDeviceWrapper(mutableSetOf()))
    private val _bleDevices = MutableStateFlow<Set<InternalBtDevice>>(mutableSetOf())

    val bleDevices: StateFlow<BTDevicesStatus> = combine(
        isBluetoothEnabled,
        btPermissionRepository.bluetoothPermissionsGranted,
        _bleDevices,
    ) { enabled, granted, devices ->
        when {
            !granted -> BTDevicesStatus.NoPermissionGranted
            !enabled -> BTDevicesStatus.BtNotEnabled
            /*(granted && enabled) && */  devices.isEmpty() -> BTDevicesStatus.Loading
            else -> {
//                Logger.v(
//                    "BleDevicesViewModel::StateFLow",
//                    "Devices:" + devices.joinToString { it.name + " : " + "(Conn?: ${it.isConnected}, HiFi?: ${it.hasHighDefinitionAudioCapabilities})" })
                BTDevicesStatus.BTDevicesFound(devices)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = BTDevicesStatus.Loading
    )


    private fun Int.isHiDefinitionAudio(): Boolean =
        (this == BluetoothProfile.A2DP || this == BluetoothProfile.HEADSET)

    private fun setupBluetoothProxy() {
        Logger.v(TAG, "In :setupBluetoothProxy()")
        allProfiles.forEach {
            bluetoothAdapter.getProfileProxy(application, serviceListener, it)
        }
    }

    private val serviceListener = object : ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            btProfiles[profileOf(profile)] = proxy
            viewModelScope.launch { delay(500L) }
            val numDevices = proxy.connectedDevices.size
            Logger.v(
                "BluetoothAdapter",
                "onServiceConnected: $numDevices connected devices for Profile $profile.."
            )

            addToConnectedDevicesFromProfileHelper(
                profile = profile,
                isHiDefinitionAudio = profile.isHiDefinitionAudio()
            )
        }

        override fun onServiceDisconnected(profile: Int) {
            Logger.d(TAG, "onServiceDisconnected()")
            btProfiles[profileOf(profile)] = null
            // TODO: Trying to connect immediately
            reconnectProxy(profile) // Allow for devices to connect and be detected later..
        }
    }

    @SuppressLint("MissingPermission")
    private fun addToConnectedDevicesFromProfileHelper(
        isHiDefinitionAudio: Boolean,
        profile: Int
    ) {
        // Profile is ready, check connectedDevices directly
        if (ActivityCompat.checkSelfPermission(
                application,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Logger.w(TAG, "Permission not granted!")
            return
        }
        val connectedDevices = btProfiles[profileOf(profile)]?.connectedDevices ?: emptyList()
        if (connectedDevices.isEmpty()) {
            Logger.i(
                TAG,
                "No connected devices found, even though  service listener  was connected."
            )
            return
        }

        updateBleDevicesFromConnectedDevices(connectedDevices, isHiDefinitionAudio) { btDevice ->
            viewModelScope.launch {
                btRepository.addDeviceCapabilities(btDevice, isHiDefinitionAudio)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateBleDevicesFromConnectedDevices(
        connectedDevices: List<BluetoothDevice?>,
        isHiDefinitionAudio: Boolean,
        callback: ((InternalBtDevice) -> Unit)?
    ) {

        Logger.v(
            "BLEDevices",
            "Before:" + _bleDevices.value.joinToString { it.name + " : " + "(Conn?: ${it.isConnected}, HiFi?: ${it.hasHighDefinitionAudioCapabilities})" })

        val storedDevices = _bleDevices.value.toMutableSet()
        connectedDevices.filterNotNull().filter { device ->
            val noName = device.name.isNullOrEmpty()
            if (noName) {
                Logger.w(
                    "BleDevicesViewModel",
                    "Device name is null: ${device.address}, not adding it to connected devices list."
                )
            }
            !noName
        }.map { btDevice ->
            InternalBtDevice(
                name = btDevice.name,
                address = btDevice.address,
                isConnected = true,
                hasHighDefinitionAudioCapabilities = isHiDefinitionAudio
            )
        }.forEach { btDevice ->
            callback?.invoke(btDevice)
            if (btDevice in storedDevices) {
                val removed =
                    storedDevices.remove(btDevice) // Teh device might have changed connected status
//                Logger.v(TAG, "Removed device ${btDevice}? $removed")
            }
            val added = storedDevices.add(btDevice)
//            Logger.v(TAG, "Added device ${btDevice}? $added")
        }
//        Logger.v(
//            "BLEDevices",
//            "After(1):" + storedDevices.joinToString { it.name + " : " + "(Conn?: ${it.isConnected}, HiFi?: ${it.hasHighDefinitionAudioCapabilities})" })
        // Because InternalBtDevice has equals and hashCode only on the other two fields we have to
        // reset it using this hack
        _bleDevices.value = emptySet()
        _bleDevices.value = storedDevices.toSet()
//        Logger.v(
//            "BLEDevices",
//            "After(2):" + _bleDevices.value.joinToString { it.name + " : " + "(Conn?: ${it.isConnected}, HiFi?: ${it.hasHighDefinitionAudioCapabilities})" })
    }


    private fun reconnectProxy(profileID: Int) {
        // Attempt to reconnect if service disconnected unexpectedly
        bluetoothAdapter.getProfileProxy(application, serviceListener, profileID)
    }


    private  var btStateReceiver  = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Logger.v(TAG, "Received action: $action")
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                Logger.v(TAG, "Received action to change state to `$state`")
                _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON

            } else {
                val device: BluetoothDevice? = intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
                if (device == null) {
                    Logger.w(
                        TAG,
                        "Device in intent is null. Intent: $intent"
                    )
                    return
                }
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
//                        Logger.v(
//                            TAG,
//                            "Non null Profiles are ${btProfiles.filter { it.value != null }.keys}"
//                        )
//                        setupBluetoothProxy()
                        updateConnectedDevice(device, true)
                        // TODO: FIgure out why just setting up a proxy does not get bonded devices?
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        updateConnectedDevice(device, false)
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        application.registerReceiver(btStateReceiver, filter)
        viewModelScope.launch {
            isBluetoothEnabled.collect { isEnabled ->
                if (isEnabled) {
                    Logger.v("$TAG[init.isBluetoothEnabled.collect]", "Bluetooth is enabled")
                    loadBondedBleDevices()
                    setupBluetoothProxy() // Gets all connected devices, call loadBondedDevices() to get all the bonded devices.
                } else {
                    Logger.v("$TAG[init.isBluetoothEnabled.collect]", "Bluetooth is disabled.")
                    clearBondedAndConnectedDeviceCache()
                }
            }
        }
//        viewModelScope.launch {
//            _bleDevices.asSharedFlow().collect { devices ->
//                val msg =
//                    devices.joinToString { it.name + " : " + "(Conn?: ${it.isConnected}, HiFi?: ${it.hasHighDefinitionAudioCapabilities})" }
//                Logger.v("BLEDevicesViewModel", "ALT DEVICE CHANGED: $msg")
//            }
//        }
    }

    // Remove a device from the connected set
    @SuppressLint("MissingPermission")
    private fun updateConnectedDevice(device: BluetoothDevice, isConnected: Boolean) {
        if (device.name != null && device.address != null) {
            var internalDevice = InternalBtDevice(device.name, device.address, isConnected)
            val removedDevice = _bleDevices.value.find { internalDevice == it }

            if (removedDevice != null) {
//                Logger.v("BleDevicesViewModel", "Updating device with connected flag to: $isConnected")
                removeDevice(removedDevice)
//                Logger.v("BleDevicesViewModel", "Removed device ${device.name} ${device.address}")
                internalDevice = InternalBtDevice(
                    device.name,
                    device.address,
                    isConnected,
                    removedDevice.hasHighDefinitionAudioCapabilities
                )
                addDevice(internalDevice)
            } else {
                Logger.w(
                    "BleDevicesViewModel",
                    "Failed to remove a connected device ${device.name} ${device.address}"
                )
            }
        }

    }

    private fun removeDevice(device: InternalBtDevice) {
        _bleDevices.value -= device
    }

    private fun addDevice(device: InternalBtDevice) {
        _bleDevices.value += device
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun BluetoothDevice.whichType(): String {
        return if (isConnectPermissionGranted()) {
            when (type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
                else -> "Unknown"
            }
        } else {
            "Unknown"
        }
    }

    private fun isConnectPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            application,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun BluetoothDevice.strPprint(): String {
        return """
        Name: ${this.name}
        Address: ${this.address}
        Type: ${this.whichType()}
        Alias: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) this.alias else "N/A"}
        Class: ${this.bluetoothClass}
        UUIDs: \n+ ${
            this.uuids?.joinToString(separator = "\n\t") ?: "None"
        }
        """.trimIndent()
    }
    */

    @SuppressLint("MissingPermission")
    suspend fun loadBondedBleDevices() {
        // Initialize the list we know about.
        if (isConnectPermissionGranted()) {
            Logger.v(javaClass.simpleName, "Num Devices in cache ${_bleDevices.value.size}")
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val btDevices = mutableSetOf<InternalBtDevice>()
            for (device in bonded) {
//                Logger.v("BleDevicesViewModel", "Bonded device: ${device.strPprint()}")
                if (device.name != null) {
                    device.bondState
                    val internalDevice = InternalBtDevice(
                        device.name,
                        device.address,
                        false, // We can never know if a device is connected at this point unless we do a scan which is way too many resources.
                        btRepository.isAudioCapableFastCheck(device) || btRepository.isAudioCapableOffline(
                            device
                        )
                    )

                    if (!_bleDevices.value.contains(internalDevice)) {
                        btDevices.add(internalDevice)
                    }
                }
            }
            if (btDevices.size > 0) {
                _bleDevices.value += btDevices
                Logger.v(
                    "BleDevicesViewModel",
                    " ${bonded.size} Bonded BLE devices found. We added  ${btDevices.size}  devices and added them"
                )
            }
            Logger.v(javaClass.simpleName, "Num devices: ${_bleDevices.value.size}")
        } else {
            Logger.w(
                "BleDevicesViewModel",
                "Bluetooth connect permissions not granted! This method should not be called before you do that."
            )
        }
    }

    private fun clearBondedAndConnectedDeviceCache() {
        clearBondedDevices()
    }


    private fun clearBondedDevices() {
        Logger.v("BleDevicesViewModel", "Clearing bonded devices")
        _bleDevices.value = setOf()
    }

    private fun clearProxy() {
        btProfiles.forEach { (internalProfile, proxy) ->
            try {
                bluetoothAdapter.closeProfileProxy(internalProfile.id, proxy)
            } catch (e: Exception) {
                Logger.e(this::class.java.name, "Error closing profile proxy", e)
            } finally {
                btProfiles[internalProfile] = null
            }
        }
    }

    private val placeTags: Flow<List<PlaceTag>> = placeTagDao.getAllPlaceTags().map { entities ->
        entities.map { it.toDomain() }
    }

    private val anyPlaceHasBLE: Flow<Boolean> = placeTagDao.getAllPlaceTags().map { entities ->
        entities.any { it.toDomain().isTypeBLE() }
    }


    val placeTagsWithSelectedDevicesState: StateFlow<PlaceTagsWithDevicesState> = combine(
        anyPlaceHasBLE,
        placeTags,
        isBluetoothEnabled,
        btPermissionRepository.bluetoothPermissionsGranted,
        _bleDevices
    ) { anyPlaceIsBTType, places, isEnabled, isPermGranted, bleDevices ->
        when {
            !anyPlaceIsBTType -> PlaceTagsWithDevicesState.NoPlaceHasBTType
            !isPermGranted -> PlaceTagsWithDevicesState.PermissionNotGranted
            isPermGranted && !isEnabled -> PlaceTagsWithDevicesState.BtNotEnabled
            /*isPermGranted && isEnabled == true*/ (bleDevices.isEmpty()) -> PlaceTagsWithDevicesState.BTDevicesNotFound
            else -> PlaceTagsWithDevicesState.Success(
                places.filter { it.isTypeBLE() }.map { placeTag ->
                    val selectedAddress = placeTag.getSelectedDeviceAddress()
                    val selectedDevice =
                        bleDevices.firstOrNull { it.address.lowercase() == selectedAddress?.lowercase() }
                    placeTag to selectedDevice
                }/*.also { it ->
                    Logger.v(
                        TAG,
                        "BLE Devices:" + bleDevices.joinToString { it.name + " : " + "(Conn?: ${it.isConnected}, HiFi?: ${it.hasHighDefinitionAudioCapabilities})" })

                    Logger.v(
                        TAG,
                        "Place device Map: " + it.joinToString { "${it.first.name}: ${it.second?.name}" })
                }*/
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PlaceTagsWithDevicesState.Loading // Initial state
    )

    fun selectDeviceForPlaceTag(placeTag: PlaceTag, deviceAddress: String) {
        viewModelScope.launch {
            val updated = placeTag.withSelectedDeviceAddress(deviceAddress)
            placeTagDao.insertPlaceTags(listOf(updated.toEntity())) // Or a dedicated update method
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            clearBondedAndConnectedDeviceCache()
            application.unregisterReceiver(btStateReceiver)
        } catch (e: Exception) {
            application.unregisterReceiver(btStateReceiver)
            Logger.e(this::class.java.name, "Error during cleanup", e)
        } finally {
            clearProxy()
        }
    }

    companion object {
        private val allProfiles = if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf( // Empty map to avoid null pointer exception,>()
                BluetoothProfile.HEADSET,
                BluetoothProfile.A2DP,
                BluetoothProfile.GATT,
                BluetoothProfile.GATT_SERVER,
                BluetoothProfile.LE_AUDIO,
                BluetoothProfile.HEARING_AID
            )
        } else {
            listOf(
                BluetoothProfile.HEADSET,
                BluetoothProfile.A2DP,
                BluetoothProfile.GATT,
                BluetoothProfile.GATT_SERVER,
            )
        }
    }
}

inline fun <K, V, T> MutableMap<K, V>.setForProfile(
    key: T,
    value: V,
    transform: (T) -> K
) {
    this[transform(key)] = value
}

inline fun <K, V, T> Map<K, V>.getForProfile(
    key: T,
    transform: (T) -> K
): V? {
    return this[transform(key)]
}

sealed class InternalBTProfile(val id: Int) {
    data object A2DP : InternalBTProfile(BluetoothProfile.A2DP)
    data object HEADSET : InternalBTProfile(BluetoothProfile.HEADSET)
    data object GATT : InternalBTProfile(BluetoothProfile.GATT)
    data object GATT_SERVER : InternalBTProfile(BluetoothProfile.GATT_SERVER)

    @SuppressLint("InlinedApi")
    data object LE_AUDIO : InternalBTProfile(BluetoothProfile.LE_AUDIO)
    data object HEARING_AID : InternalBTProfile(BluetoothProfile.HEARING_AID)

    companion object {
        fun profileOf(profileNum: Int): InternalBTProfile {
            return when (profileNum) {
                BluetoothProfile.A2DP -> A2DP
                BluetoothProfile.HEADSET -> HEADSET
                BluetoothProfile.GATT -> GATT
                BluetoothProfile.GATT_SERVER -> GATT_SERVER
                BluetoothProfile.LE_AUDIO -> LE_AUDIO
                BluetoothProfile.HEARING_AID -> HEARING_AID
                else -> throw IllegalArgumentException("Unknown profile number: $profileNum")
            }
        }

    }
}
