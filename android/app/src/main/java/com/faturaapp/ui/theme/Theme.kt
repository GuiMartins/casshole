package com.faturaapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = IndigoPrimary,
    secondary = TealSecondary,
    onSecondary = Color.White,
    tertiary = TealSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    error = ErrorRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF241A5C),
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = IndigoPrimaryDark,
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF00332C),
    tertiary = TealSecondaryDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRedDark,
    onError = Color(0xFF3D0A0A),
)

@Composable
fun FaturaAppTheme(
    temaEscuro: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val usarEscuro = temaEscuro ?: isSystemInDarkTheme()
    val colorScheme = if (usarEscuro) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
