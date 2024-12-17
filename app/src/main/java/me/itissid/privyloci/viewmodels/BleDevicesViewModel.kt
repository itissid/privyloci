package me.itissid.privyloci.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothA2dp
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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.datamodels.toEntity
import me.itissid.privyloci.util.BTDevicesRepository
import me.itissid.privyloci.util.Logger
import java.util.UUID
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
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val manager = application.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }

    private var _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter.isEnabled)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()


    private var _connectedDevices = MutableStateFlow<Set<InternalBtDevice>>(emptySet())
    val connectedDevices: StateFlow<Set<InternalBtDevice>> = _connectedDevices

    private  var btStateReceiver  = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                if (_isBluetoothEnabled.value) {
                    viewModelScope.launch {
                        loadBondedBleDevices()
                    }
                } else {
                    clearBondedDevices()
                }
            } else {
                val device: BluetoothDevice? = intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
                if (device == null) {
                    Logger.w("BleDevicesViewModel", "Device in intent is null!")
                    return
                }
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        addConnectedDevice(device)
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
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
    }

    // Add a device to the connected set
    @SuppressLint("MissingPermission")
    private fun addConnectedDevice(device: BluetoothDevice) {
        val internalDevice = InternalBtDevice(device.name, device.address, isConnected = true)
        _connectedDevices.value += internalDevice
    }

    // Remove a device from the connected set
    private fun removeConnectedDevice(device: BluetoothDevice) {
        _connectedDevices.value = _connectedDevices.value.filter {
            it.address != device.address
        }.toSet()
    }

    private val _bleDevices = MutableStateFlow<List<InternalBtDevice>>(emptyList())

    val bleDevices = _bleDevices.combine(
        btPermissionRepository.bluetoothPermissionsGranted
    ) { devices, granted -> if (granted) devices else null }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = null
    )

    private fun BluetoothDevice.whichType(): String {

        return if (ActivityCompat.checkSelfPermission(
                application,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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


    @SuppressLint("MissingPermission")
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

    // TODO also create a broadcast reciever to update the bonded device list with "live" devices when a device connect/disconnects.
    suspend fun loadBondedBleDevices(filterAudio: Boolean = false) {
        // TODO: If we already have checked the permission why do we need to do it again here?
        if (ActivityCompat.checkSelfPermission(
                application,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val audioDevices = mutableListOf<InternalBtDevice>()
            for (device in bonded) {
                Logger.v("BleDevicesViewModel", "Bonded device: ${device.strPprint()}")
                val isAudio = btRepository.isAudioCapable(device)
                if (isAudio && device.name != null) {
                    audioDevices.add(InternalBtDevice(device.name, device.address, false))
                }
            }
            Logger.v(
                "BleDevicesViewModel",
                " ${bonded.size} Bonded BLE devices found.  ${audioDevices.size} are potentially audio capable"
            )
            _bleDevices.value = audioDevices
        } else {
            Logger.e(
                "BleDevicesViewModel",
                "Bluetooth permissions not granted! This method should not be called before you do that"
            )
        }
    }

    private fun clearBondedDevices() {
        Logger.v("BleDevicesViewModel", "Clearing bonded devices")
        _bleDevices.value = emptyList()
    }

    // Potentially expose a method to update device selection in DB
    fun selectDeviceForPlaceTag(placeTag: PlaceTag, deviceAddress: String) {
        viewModelScope.launch {
            val updated = placeTag.withSelectedDeviceAddress(deviceAddress)
            placeTagDao.insertPlaceTags(listOf(updated.toEntity())) // Or a dedicated update method
        }
    }

    // Check if the selected device is currently connected
    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(deviceAddress: String): Boolean {
        val a2dp = bluetoothAdapter.getProfileProxy(
            application,
            null,
            BluetoothProfile.A2DP
        ) as? BluetoothA2dp
        return a2dp?.connectedDevices?.any { it.address == deviceAddress } == true
    }

    override fun onCleared() {
        super.onCleared()
        clearBondedDevices()
        application.unregisterReceiver(btStateReceiver)
    }
}
