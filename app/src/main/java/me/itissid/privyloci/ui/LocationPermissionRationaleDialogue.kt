package me.itissid.privyloci.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.itissid.privyloci.ui.theme.PrivyLociTheme
import me.itissid.privyloci.viewmodels.LocationPermissionRationale

@Composable
fun LocationPermissionRationaleDialogWrapper(rationale: LocationPermissionRationale) {
    LocationPermissionRationaleDialogue(
        onDismiss = rationale.onDismiss,
        onConfirm = rationale.onConfirm,
        rationale.rationaleText,
        proceedText = rationale.proceedButtonText,
        dismissText = rationale.dismissButtonText
    )
    AlertDialog(
        onDismissRequest = { rationale.onDismiss() },
        confirmButton = {
            TextButton(onClick = { rationale.onConfirm() }) {
                Text(rationale.proceedButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = { rationale.onDismiss() }) {
                Text(rationale.dismissButtonText)
            }
        },
        text = {
            Text(rationale.rationaleText)
        }
    )
}

@Composable
fun LocationPermissionRationaleDialogue(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String,
    proceedText: String = "Proceed",
    dismissText: String = "Cancel"
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

@Composable
fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Location permission is permanently denied. Please enable it in the app settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenSettings) {
                Text(
                    "Open App Settings",
                    color = MaterialTheme.colorScheme.onPrimary,
//                    style = MaterialTheme.typography.bodyMedium
                )
            }

        }
    }

}


@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PermissionDeniedScreenPreview3() {
    PrivyLociTheme(dynamicColor = true) {
        PermissionDeniedScreen(onOpenSettings = {})
    }
}

@Preview(
    name = "Night mode. No Dynamic Color",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Day mode. No Dynamic Color",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PermissionDeniedScreenPreview4() {
    PrivyLociTheme(dynamicColor = false) {
        PermissionDeniedScreen(onOpenSettings = {})
    }
}


@Preview(
    name = "Night mode",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PermissionDeniedScreenPreview1() {
    PrivyLociTheme {
        PermissionDeniedScreen(onOpenSettings = {})
    }
}


@Preview(
    name = "Day mode",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PermissionDeniedScreenPreview2() {
    PrivyLociTheme {
        PermissionDeniedScreen(onOpenSettings = {})
    }
}