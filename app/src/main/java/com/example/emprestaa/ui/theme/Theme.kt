package com.example.emprestaa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueBright,
    onPrimary = Color.White,
    secondary = BrandBlue,
    onSecondary = Color.White,
    tertiary = MutedText,
    background = DarkSurface,
    onBackground = LightSurface,
    surface = DarkSurface,
    onSurface = LightSurface,
    surfaceVariant = Color(0xFF22263B),
    onSurfaceVariant = Color(0xFFC2C6D6)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandBlueBright,
    onSecondary = Color.White,
    tertiary = MutedText,
    background = LightSurface,
    onBackground = DarkSurface,
    surface = Color.White,
    onSurface = DarkSurface,
    surfaceVariant = Color(0xFFE3E6FA),
    onSurfaceVariant = Color(0xFF363B51)
)

@Composable
fun EmprestaAêTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
