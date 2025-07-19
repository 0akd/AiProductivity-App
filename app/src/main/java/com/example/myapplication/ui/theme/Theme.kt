package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
val SemiTransparentPink = Color(0x80F498AD) // 0x80 = 50% alpha

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0D7FF),       // Light lavender
    onPrimaryContainer = Color(0xFF1E0061),


    secondary = PurpleGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E0F2),     // Pale gray-purple
    onSecondaryContainer = Color(0xFF2F2A3A),
    tertiary = Color.LightGray,

    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD6E8),      // Light pink
    onTertiaryContainer = Color(0xFF4D0030),
 // Correct 8-digit ARGB hex

    background = Color.White,
    onBackground = Color.Black,

    surface = Color(0xFFFAFAFA),                // Light card surface
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A408A),       // Dark lavender
    onPrimaryContainer = Color(0xFFF0E8FF),

    secondary = PurpleGrey80,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3D3A51),     // Dark gray-purple
    onSecondaryContainer = Color(0xFFE5E0F2),
    tertiary = SemiTransparentPink,

    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF633B54),      // Dark pink-brown
    onTertiaryContainer = Color(0xFFFFD6E8),

    background = Color(0xFF121212),
    onBackground = Color.White,

    surface = Color(0xFF1E1E1E),                // Dark card surface
    onSurface = Color(0xFFECECEC),
)


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */


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
