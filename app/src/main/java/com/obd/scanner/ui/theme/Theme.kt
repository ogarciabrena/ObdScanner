package com.obd.scanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PrimaryBlue = Color(0xFF1A73E8)
val PrimaryDark = Color(0xFF4FC3F7)
val Secondary = Color(0xFF00BFA5)
val Tertiary = Color(0xFFFF6B35)
val Error = Color(0xFFE53935)
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val CardDark = Color(0xFF2C2C2C)

val GaugeGreen = Color(0xFF4CAF50)
val GaugeYellow = Color(0xFFFFC107)
val GaugeOrange = Color(0xFFFF9800)
val GaugeRed = Color(0xFFF44336)
val GaugeBlue = Color(0xFF2196F3)
val GaugeCyan = Color(0xFF00BCD4)
val GaugeWhite = Color(0xFFE0E0E0)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = Secondary,
    tertiary = Tertiary,
    error = Error,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EAF6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = Secondary,
    tertiary = Tertiary,
    error = Error,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
)

@Composable
fun ObdScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
