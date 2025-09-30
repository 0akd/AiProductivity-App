package com.arjundubey.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val SemiTransparentBlue = Color(0x8080D0FF) // 0x80 = 50% alpha, light blue

// Blue color definitions (replacing purple)
val Blue40 = Color(0xFF3454D1)  // Primary blue for light theme
val Blue80 = Color(0xFF7B9AFF)  // Primary blue for dark theme
val BlueGrey40 = Color(0xFF5C6BC0)  // Secondary blue-grey for light theme
val BlueGrey80 = Color(0xFF9FA8DA)  // Secondary blue-grey for dark theme

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3FF),       // Light blue
    onPrimaryContainer = Color(0xFF001B3D),

    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E2F2),     // Pale gray-blue
    onSecondaryContainer = Color(0xFF2A2A3A),
    tertiary = Color.LightGray,

    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E8FF),      // Light blue
    onTertiaryContainer = Color(0xFF003052),

    background = Color.White,
    onBackground = Color.Black,

    surface = Color(0xFFFAFAFA),                // Light card surface
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A408A),       // Dark blue
    onPrimaryContainer = Color(0xFFE8F0FF),

    secondary = BlueGrey80,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D3A51),     // Dark gray-blue
    onSecondaryContainer = Color(0xFFE0E2F2),
    tertiary = SemiTransparentBlue,

    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF3B5463),      // Dark blue-gray
    onTertiaryContainer = Color(0xFFD6E8FF),

    background = Color(0xFF121212),
    onBackground = Color.White,

    surface = Color(0xFF1E1E1E),                // Dark card surface
    onSurface = Color(0xFFECECEC),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}