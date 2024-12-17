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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.InternalBtDevice
import me.itissid.privyloci.datamodels.PlaceTag
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.datamodels.toEntity
import me.itissid.privyloci.util.Logger
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class BleDevicesViewModel @Inject constructor(
    private val application: Application,
    private val placeTagDao: PlaceTagDao,
    private val bleRepository: BleRepository,
    // Possibly inject BluetoothManager or handle via system service
) : AndroidViewModel(application) {
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val manager = application.getSystemService(BluetoothManager::class.java)
        manager.adapter
    }
    private var _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter.isEnabled)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled

    private  var btStateReceiver  = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                if (_isBluetoothEnabled.value) {
                    loadBondedBleDevices()
                } else {
                    clearBondedDevices()
                }
            }
        }
    }
    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        application.registerReceiver(btStateReceiver, filter)
    }

    fun refreshBluetoothState() {
        Logger.v(
            "BleDevicesViewModel",
            "Refreshing Bluetooth state enabled to: ${bluetoothAdapter.isEnabled}"
        )
        _isBluetoothEnabled.value = bluetoothAdapter.isEnabled
    }

    private val _bleDevices = MutableStateFlow<List<InternalBtDevice>>(emptyList())

    val bleDevices = _bleDevices.combine(
        bleRepository.bluetoothPermissionsGranted
    ) { devices, granted -> if (granted) devices else null }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = null
    )

    private fun BluetoothDevice.whichType(): String {
        return when (type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
            else -> "Unknown"
        }
    }

    private val targetUUIDs = setOf(
        UUID.fromString("00001108-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000110e-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb")
    )

    // TODO:
    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.isAudio(): Boolean {
        return this.uuids?.map { UUID.fromString(it.toString()) }?.any { it in targetUUIDs }
            ?: false
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
    fun loadBondedBleDevices() {
        // TODO: If we already have checked the permission why do we need to do it again here?
        if (ActivityCompat.checkSelfPermission(
                application,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val bonded = bluetoothAdapter.bondedDevices
                .also { devices ->
                    Logger.v(
                        "BleDevicesViewModel",
                        "Bonded devices" +
                                devices.joinToString(separator = "\n\n") { it.pprint() })
                }
                ?.filter { it.isAudio() }
                ?.sortedBy { it.name ?: it.address }
                ?.map {InternalBtDevice(it.name, it.address)}
                ?: emptyList()
            _bleDevices.value = bonded
            Logger.v("BleDevicesViewModel", " ${bonded.size} Bonded BLE devices found")
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

    @SuppressLint("MissingPermission")
    private fun isA2dpSinkSupported(device: BluetoothDevice): Boolean {
        // Use BluetoothProfile to check A2DP support

        try {
            // Get the A2DP proxy object
            val a2dp = bluetoothAdapter.getProfileProxy(
                application,
                object : ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        // Profile proxy connected
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        // Profile proxy disconnected
                    }
                },
                BluetoothProfile.A2DP
            ) as BluetoothA2dp

            // Check if the device is connected via A2DP
            if (a2dp != null) {
                val connectedDevices = a2dp.connectedDevices
                return connectedDevices.contains(device)
            }
        } catch (e: Exception) {
            Log.e("BluetoothAudio", "Error checking A2DP support", e)
        }

        return false
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
        clearBondedDevices()
        application.unregisterReceiver(btStateReceiver)
    }
}
