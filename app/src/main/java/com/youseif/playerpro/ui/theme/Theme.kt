package com.youseif.playerpro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryRed = Color(0xFFFF0033)
val PrimaryRedDark = Color(0xFFCC0029)
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceVariantDark = Color(0xFF1A1A1A)
val OnSurfaceDark = Color(0xFFFFFFFF)
val OutlineDark = Color(0xFF333333)

private val DarkColors = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = PrimaryRedDark,
    secondary = PrimaryRed,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = OutlineDark,
    error = Color(0xFFFF5252)
)

private val LightColors = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    secondary = PrimaryRed,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFFCCCCCC),
    error = Color(0xFFB00020)
)

@Composable
fun YouseifPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
