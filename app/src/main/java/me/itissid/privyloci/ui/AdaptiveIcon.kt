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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import me.itissid.privyloci.R
import me.itissid.privyloci.ui.theme.getDarkColorScheme
import me.itissid.privyloci.ui.theme.getLightColorScheme

@Composable
fun AdaptiveIcon(locationPermissionGranted: Boolean) {
    val tint =  if(locationPermissionGranted) {
        if (isSystemInDarkTheme()) getDarkColorScheme().onSurface else getLightColorScheme().onSurface
    } else {
        if(isSystemInDarkTheme()) getDarkColorScheme().error else getLightColorScheme().error
    }
    val scale by if (!locationPermissionGranted) {
        val infiniteTransition = rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
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