package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VidoPrimaryBlue,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = VidoPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = VidoAccentCyan,
    onSecondary = Color.Black,
    background = VidoNavyDark,
    surface = VidoSurfaceDark,
    surfaceVariant = VidoCardDark,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = VidoTextMuted,
    error = VidoCallRed
)

private val LightColorScheme = lightColorScheme(
    primary = VidoLightPrimary,
    onPrimary = Color.White,
    secondary = VidoAccentCyan,
    background = VidoLightBg,
    surface = VidoLightSurface,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    error = VidoCallRed
)

@Composable
fun VidoTheme(
    darkTheme: Boolean = true, // Default to dark for sleek call utility experience
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    VidoTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
