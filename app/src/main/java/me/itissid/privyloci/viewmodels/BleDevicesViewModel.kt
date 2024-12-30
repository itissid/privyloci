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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.datamodels.toEntity
import me.itissid.privyloci.util.BTDevicesRepository
import me.itissid.privyloci.util.Logger
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

    private var btProfile: BluetoothProfile? = null
    private var profileType: Int = -1

    private var _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter.isEnabled)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()


    private val _bleDevices = MutableStateFlow<MutableSet<InternalBtDevice>>(mutableSetOf())

    val bleDevices = _bleDevices.combine(
        btPermissionRepository.bluetoothPermissionsGranted
    ) { devices, granted -> if (granted) devices else null }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = null
    )

    private fun Int.isHiDefinitionAudio(): Boolean =
        (this == BluetoothProfile.A2DP || this == BluetoothProfile.HEADSET)

    private val serviceListener = object : ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            Logger.v("BluetoothAdapter", "onServiceConnected: Profile $profile")
            btProfile = proxy
            profileType = profile

            addToConnectedDevicesFromProfileHelper(
                isHiDefinitionAudio = profile.isHiDefinitionAudio()
            )
        }

        override fun onServiceDisconnected(profile: Int) {
            btProfile = null
            reconnectProxy() // Allow for devices to connect and be detected later..
        }
    }

    @SuppressLint("MissingPermission")
    private fun addToConnectedDevicesFromProfileHelper(
        isHiDefinitionAudio: Boolean
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
        val connectedDevices = btProfile?.connectedDevices ?: emptyList()
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
        var storedDevices = _bleDevices.value.toMutableSet()
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
                name = btDevice.name ?: "Unknown",
                address = btDevice.address,
                isConnected = true,
                hasHighDefinitionAudioCapabilities = isHiDefinitionAudio
            )
        }.forEach { btDevice ->
            callback?.invoke(btDevice)
            if (btDevice in storedDevices) {
                storedDevices.remove(btDevice) // Teh device might have changed connected status
                storedDevices.add(btDevice)
            } else {
                storedDevices.add(btDevice)
            }
            storedDevices
        }
        _bleDevices.value = storedDevices
    }

    private fun setupBluetoothProxy() {
        Logger.v(TAG, "In :setupBluetoothProxy()")
        allProfiles.forEach { bluetoothAdapter.getProfileProxy(application, serviceListener, it) }
    }

    private fun reconnectProxy() {
        // Attempt to reconnect if service disconnected unexpectedly
        if (btProfile == null)
            setupBluetoothProxy()
    }


    private  var btStateReceiver  = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Logger.v(javaClass.simpleName, "Received action: $action")
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                if (_isBluetoothEnabled.value) {
                    viewModelScope.launch {
                        loadBondedBleDevices()
                    }
                } else {
                    clearBondedAndConnectedDeviceCache()
                }
            } else {
                val device: BluetoothDevice? = intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
                if (device == null) {
                    Logger.w(
                        "BleDevicesViewModel",
                        "Device in intent is null. Intent: $intent"
                    )
                    return
                }
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        if (btProfile == null) {
                            // Profile not ready yet, re-setup and handle later
                            // Optionally store the device in a pending list to handle in onServiceConnected
                            setupBluetoothProxy()
                        } else {
                            // Profile is ready, check connectedDevices directly
                            addToConnectedDevicesFromProfileHelper(profileType.isHiDefinitionAudio())
                        }
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        // TODO: Cleanup of the proxy? what if more than one device is connected?
                        removeConnectedDevice(device)
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
        setupBluetoothProxy()
    }

    // Remove a device from the connected set
    @SuppressLint("MissingPermission")
    private fun removeConnectedDevice(device: BluetoothDevice) {
        if (device.name != null && device.address != null) {
            var internalDevice = InternalBtDevice(device.name, device.address, false)
            val wasRemoved = _bleDevices.value.remove(internalDevice)
            _bleDevices.value.add(internalDevice)
        }

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

    @SuppressLint("MissingPermission")
    suspend fun loadBondedBleDevices() {
        if (isConnectPermissionGranted()) {
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val btDevices = mutableSetOf<InternalBtDevice>()
            for (device in bonded) {
                Logger.v("BleDevicesViewModel", "Bonded device: ${device.strPprint()}")
                if (device.name != null) {
                    val internalDevice = InternalBtDevice(
                        device.name,
                        device.address,
                        false, // We can never know if a device is connected at this point unless we do a scan which is way too many resources.
                        btRepository.isAudioCapableFastCheck(device) || btRepository.isAudioCapableOffline(
                            device
                        )
                    )
                    // There may be devices that are connected we don't want to
                    if (!_bleDevices.value.contains(internalDevice)) {
                        btDevices.add(internalDevice)
                    }
                }
            }
            val anyAdded = _bleDevices.value.addAll(btDevices)
            Logger.v(
                "BleDevicesViewModel",
                " ${bonded.size} Bonded BLE devices found. We added some devices?: $anyAdded;  ${btDevices.size}  devices added"
            )
        } else {
            Logger.w(
                "BleDevicesViewModel",
                "Bluetooth connect permissions not granted! This method should not be called before you do that."
            )
        }
    }

    private fun clearBondedAndConnectedDeviceCache() {
        clearBondedDevices()
//        clearConnectedDevices()
    }

//    private fun clearConnectedDevices() {
//        Logger.v("BleDevicesViewModel", "Clearing connected devices")
//        _connectedDevices.value = emptySet()
//    }

    private fun clearBondedDevices() {
        Logger.v("BleDevicesViewModel", "Clearing bonded devices")
        _bleDevices.value = mutableSetOf()
    }

    private fun clearProxy() {
        try {
            allProfiles.forEach {
                bluetoothAdapter.closeProfileProxy(it, btProfile)
            }
        } catch (e: Exception) {
            Logger.e(this::class.java.name, "Error closing profile proxy", e)
        } finally {
            btProfile = null
        }
    }


    // Potentially expose a method to update device selection in DB
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
        }

        // Separate try-finally for profile cleanup
        clearProxy()
    }

    companion object {
        private val allProfiles = listOf(
            BluetoothProfile.HEADSET,
            BluetoothProfile.A2DP,
            BluetoothProfile.GATT,
            BluetoothProfile.GATT_SERVER,
            BluetoothProfile.LE_AUDIO,
            BluetoothProfile.HEARING_AID,
            BluetoothProfile.HID_DEVICE
        )
    }
}
