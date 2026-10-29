package com.example.emprestaa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlue,
    onPrimaryContainer = DeepBlue,
    secondary = AvailableGreen,
    onSecondary = Color.White,
    tertiary = PendingOrange,
    onTertiary = Color.White,
    error = ErrorRed,
    onError = Color.White,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = SoftBlue,
    onSurfaceVariant = TextSecondary,
    outline = BorderNeutral,
    outlineVariant = BorderNeutral
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9AAEFF),
    onPrimary = Color(0xFF10225E),
    primaryContainer = Color(0xFF263C82),
    onPrimaryContainer = Color(0xFFE8EEFF),
    secondary = Color(0xFF65C59D),
    onSecondary = Color(0xFF062C1F),
    tertiary = Color(0xFFF0B96C),
    onTertiary = Color(0xFF392100),
    error = Color(0xFFFF9298),
    onError = Color(0xFF4B0713),
    background = DarkBackground,
    onBackground = Color(0xFFF1F4FA),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F4FA),
    surfaceVariant = Color(0xFF202D45),
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF46536B),
    outlineVariant = Color(0xFF35435C)
)

@Composable
fun EmprestaAêTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
