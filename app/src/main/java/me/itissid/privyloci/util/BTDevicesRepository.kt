package me.itissid.privyloci.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import me.itissid.privyloci.datamodels.DeviceCapabilitiesDao
import me.itissid.privyloci.datamodels.DeviceCapabilityEntity
import me.itissid.privyloci.datamodels.InternalBtDevice
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BTDevicesRepository @Inject constructor(
    private val deviceCapabilitiesDao: DeviceCapabilitiesDao,
    @ApplicationContext private val context: Context
) {
    // Source: https://www.bluetooth.com/wp-content/uploads/Files/Specification/Assigned_Numbers.html
    private val audioTargetUUIDs = setOf(
        /**
         * The Advanced Audio Distribution Profile (A2DP) defines how high-quality audio can be streamed from one device to another over a Bluetooth connection. The "Sink" role refers to the device receiving the audio, such as headphones, speakers, or car audio systems.
         *
         * Capabilities:
         * High-Quality Audio Streaming: Supports stereo audio with codecs like SBC, AAC, and aptX.
         * Media Control: Allows for control over playback features such as play, pause, and skip.
         * */
        UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb"),
        /**
         * The Audio/Video Remote Control Profile (AVRCP) allows remote control of media playback on a target device. It is commonly used in scenarios where a user controls music playback on a smartphone using a car's infotainment system or a Bluetooth speaker with remote controls.
         *
         * Capabilities:
         * Media Playback Control: Supports commands like play, pause, stop, next track, and previous track.
         * Metadata Retrieval: Allows retrieval of information about the currently playing media, such as title, artist, and album.
         * */
        UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb"),
        /**
         * Description: A2DP is responsible for the high-quality audio streaming from a source device (e.g., smartphone) to a sink device (e.g., AirPods). It supports stereo audio and various codecs like SBC, AAC, and aptX (though AirPods primarily use AAC).
         *
         * Capabilities:
         *
         * High-Quality Audio Streaming: Enables stereo sound transmission.
         * Codec Support: Handles multiple audio codecs for efficient streaming.
         * */
        UUID.fromString("0000110d-0000-1000-8000-00805f9b34fb")
    )

    // For documentation purposes, but these also might help determine in addition to audio capabilities, control capabilities for user interaction using a button.
    // Apple airbuds have separate UUIDs in this line for control capabilities.
    private val _controllingDevices = setOf(
        /**
         * The Headset Profile (HSP) is designed to allow wireless headsets to communicate with devices like mobile phones and computers. It primarily facilitates basic audio streaming and control functions, such as answering or ending calls.
         *
         * Capabilities:
         * Audio Streaming: Enables mono audio streaming for voice communication.
         * Control Commands: Supports basic commands like volume control, call answer/end.
         * */
        UUID.fromString("00001108-0000-1000-8000-00805f9b34fb"),
        /**
         * The Audio/Video Remote Control Profile (AVRCP) allows remote control of media playback on a target device. It is commonly used in scenarios where a user controls music playback on a smartphone using a car's infotainment system or a Bluetooth speaker with remote controls.
         *
         * Capabilities:
         * Media Playback Control: Supports commands like play, pause, stop, next track, and previous track.
         * Metadata Retrieval: Allows retrieval of information about the currently playing media, such as title, artist, and album.
         *
         * */
        UUID.fromString("0000110e-0000-1000-8000-00805f9b34fb"),
        /**
         * Description: AVRCP allows remote control of media playback on the source device. For AirPods, this enables functionalities like play, pause, skip tracks, and volume control directly from the earbuds.
         *
         * Capabilities:
         *
         * Media Playback Control: Manage playback functions remotely.
         * Metadata Retrieval: Access information about the currently playing media.
         */
        UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb")
    )


    //fast check, avoid scanning.
    fun isAudioCapableFastCheck(device: BluetoothDevice): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val deviceUuids = device.uuids?.map { it.uuid } ?: emptyList()
            val matchesAudio = deviceUuids.any { it in audioTargetUUIDs }
            if (matchesAudio) {
                return true
            }
        }
        return false
    }

    suspend fun addDeviceCapabilities(device: InternalBtDevice, isAudioCapable: Boolean) {
        deviceCapabilitiesDao.insertCapability(
            DeviceCapabilityEntity(
                device.address,
                isAudioCapable
            )
        )
    }

    suspend fun isAudioCapableOffline(device: BluetoothDevice): Boolean {
        /**
         * In case the device is not found in the isAudioCapableFastCheck(used for btAdapter.bondedDevices)
         * I can use this method to check if a previously *connected* device had registered its a2dp capabilities.
         * */
        val cached = deviceCapabilitiesDao.getCapability(device.address)
        if (cached != null) return cached.isAudioCapable
        return false
    }

}