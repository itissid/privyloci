package me.itissid.privyloci.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.itissid.privyloci.util.Logger
import me.itissid.privyloci.viewmodels.BlePermissionEvent
import me.itissid.privyloci.viewmodels.BTPermissionViewModel

/**
 *  Ideally we would only need the view model and the UI.
 *  But this is needed for being able to use the Composable rememberMultiplePermissionsState which I can't use
 *  in a non-composable scope of the view model.
 * */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BlePermissionHandler(bleViewModel: BTPermissionViewModel) {
    val context = LocalContext.current
    val blePermissionState =
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH
            )
        )
    val rationaleState by bleViewModel.blePermissionRationaleState.collectAsState()

    rationaleState?.let { rationale ->
        BlePermissionRationaleDialogue(
            message = rationale.rationaleText,
            proceedText = rationale.proceedButtonText,
            dismissText = rationale.dismissButtonText,
            onConfirm = rationale.onConfirm,
            onDismiss = rationale.onDismiss
        )
    }

    bleViewModel.setBlePermissionGranted(blePermissionState.allPermissionsGranted)
    bleViewModel.setShouldShowRationale(blePermissionState.shouldShowRationale)
    // N2S: Could this be done by rememberCoroutineScope instead?
    LaunchedEffect(Unit) {
        bleViewModel.permissionEvents.collect {
            when (it) {
                BlePermissionEvent.OpenSettings -> {
                    try {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data =
                                    Uri.fromParts(
                                        "package",
                                        context.applicationContext.packageName,
                                        null
                                    )
                            }
                        context.startActivity(intent)
                        // N2S: Consider setting a userpreference for firebase testing. If users can't/won't give permission after
                        // visiting settings == bad.
                    } catch (e: Exception) {
                        Logger.e("BLEPermissionHandler", "Error opening settings: ${e.message}", e)
                    }
                }

                BlePermissionEvent.RequestBlePermissions -> {
                    blePermissionState.launchMultiplePermissionRequest()
                }
            }
        }
    }
}

@Composable
fun BlePermissionRationaleDialogue(
    message: String,
    proceedText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(proceedText)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(dismissText)
            }
        },
        text = {
            Text(message)
        }
    )
}
