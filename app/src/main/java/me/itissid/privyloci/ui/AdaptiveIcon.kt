package me.itissid.privyloci.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import me.itissid.privyloci.R
import me.itissid.privyloci.ui.theme.darkScheme
import me.itissid.privyloci.ui.theme.lightScheme

@Composable
fun AdaptiveIcon(locationPermissionGranted: Boolean) {
    val tint =  if(locationPermissionGranted) {
        if (isSystemInDarkTheme()) darkScheme.onSurface else lightScheme.onSurface
    } else {
        if (isSystemInDarkTheme()) darkScheme.error else lightScheme.error
    }
    val scale by if (!locationPermissionGranted) {
        val infiniteTransition = rememberInfiniteTransition(label = "Infinite Transition")
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "animation"
        )
    } else {
        // When condition is false, simply return a static scale of 1f
        remember { mutableFloatStateOf(1f) }
    }
    Icon(
        painter = if (locationPermissionGranted) painterResource(id = R.drawable.ic_location) else painterResource(
            id = R.drawable.ic_no_location_icon
        ),
        contentDescription = "Custom Icon",
        tint = tint,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}

sealed class IconResource(val notGranted: Int, val granted: Int) {
    // TODO: Use me when calling from SubscriptionCard  for location usecase
    data object LocationIcon : IconResource(R.drawable.ic_no_location_icon, R.drawable.ic_location)
    data object BLEIcon :
        IconResource(R.drawable.noun_bluetooth_off_482384, R.drawable.noun_bluetooth_482388)
}

@Composable
fun AdaptiveIconWrapper(
    permissionGranted: Boolean,
    iconResource: IconResource
) {
    val tint = if (permissionGranted) {
        if (isSystemInDarkTheme()) darkScheme.onSurface else lightScheme.onSurface
    } else {
        if (isSystemInDarkTheme()) darkScheme.error else lightScheme.error
    }

    val scale by if (!permissionGranted) {
        val infiniteTransition = rememberInfiniteTransition(label = "Infinite Transition")
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "animation"
        )
    } else {
        // When condition is false, simply return a static scale of 1f
        remember { mutableFloatStateOf(1f) }
    }
    Icon(
        painter = if (permissionGranted) painterResource(id = iconResource.granted) else painterResource(
            id = iconResource.notGranted
        ),
        contentDescription = "Custom Icon",
        tint = tint,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    )
}