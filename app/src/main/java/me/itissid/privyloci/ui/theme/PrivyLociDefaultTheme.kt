package me.itissid.privyloci.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import me.itissid.privyloci.R

@Composable
fun getColorFromXml(colorResId: Int): Color {
    val context = LocalContext.current
    return Color(ContextCompat.getColor(context, colorResId))
}

@Composable
fun getLightColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = getColorFromXml(R.color.colorPrimary),        // #6200EE
        onPrimary = getColorFromXml(R.color.colorOnPrimary),    // #FFFFFF
        primaryContainer = getColorFromXml(R.color.colorPrimaryDark), // #3700B3
        secondary = getColorFromXml(R.color.colorSecondary),    // #03DAC5
        onSecondary = getColorFromXml(R.color.black),           // Assuming black color defined in XML
        background = getColorFromXml(R.color.white),            // White background
        onBackground = getColorFromXml(R.color.black),          // Black text on background
        surface = getColorFromXml(R.color.white),               // White surface
        onSurface = getColorFromXml(R.color.black)              // Black text on surface
    )
}

@Composable
fun getDarkColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = getColorFromXml(R.color.colorPrimary),        // #BB86FC (defined in colors-night.xml)
        onPrimary = getColorFromXml(R.color.colorOnPrimary),    // #000000 (Black text on primary)
        primaryContainer = getColorFromXml(R.color.colorPrimaryDark), // #3700B3
        secondary = getColorFromXml(R.color.colorSecondary),    // #03DAC5 (fallback to colors.xml since not defined in colors-night.xml)
        onSecondary = getColorFromXml(R.color.black),           // Black text on secondary
        background = Color(0xFF121212),                         // Custom background for dark mode
        onBackground = Color.White,                             // White text on dark background
        surface = Color(0xFF121212),                            // Dark surface
        onSurface = Color.White                                 // White text on dark surface
    )
}
@Composable
fun PrivyLociTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Use dynamic color scheme for API 31+
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use predefined color scheme for lower API levels
        if (darkTheme) getDarkColorScheme() else getLightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )

}