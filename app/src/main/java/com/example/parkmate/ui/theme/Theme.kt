package com.example.parkmate.ui.theme

import android.app.Activity
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

// Tonalità chiarissime/scurissime dello stesso verde del brand, usate solo per le superfici
// "di sfondo" (barra di navigazione, card, dialog): non fanno parte della palette ufficiale
// (scuro/principale/chiaro/bianco) riportata in DECISIONI.md, servono solo a evitare che
// Material3 ricada sui suoi grigi/viola di default dove non abbiamo specificato un colore
private val GreenSurfaceTintLight = Color(0xFFF2FAF5)
private val GreenSurfaceTintDark = Color(0xFF0E4634)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenDark,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLight,
    secondary = GreenPrimary,
    onSecondary = Color.White,
    secondaryContainer = GreenDark,
    onSecondaryContainer = GreenLight,
    tertiary = Color.White,
    onTertiary = GreenDark,
    tertiaryContainer = GreenDark,
    onTertiaryContainer = Color.White,
    background = GreenDark,
    onBackground = Color.White,
    surface = GreenDark,
    onSurface = Color.White,
    surfaceVariant = GreenSurfaceTintDark,
    onSurfaceVariant = Color.White,
    surfaceContainerLowest = GreenDark,
    surfaceContainerLow = GreenDark,
    surfaceContainer = GreenSurfaceTintDark,
    surfaceContainerHigh = GreenSurfaceTintDark,
    surfaceContainerHighest = GreenSurfaceTintDark
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = GreenDark,
    onSecondary = Color.White,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenDark,
    tertiary = GreenLight,
    onTertiary = GreenDark,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = GreenDark,
    background = Color.White,
    onBackground = GreenDark,
    surface = Color.White,
    onSurface = GreenDark,
    surfaceVariant = GreenSurfaceTintLight,
    onSurfaceVariant = GreenDark,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = GreenSurfaceTintLight,
    surfaceContainer = GreenSurfaceTintLight,
    surfaceContainerHigh = GreenSurfaceTintLight,
    surfaceContainerHighest = GreenSurfaceTintLight
)

@Composable
fun ParkMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disattivato: la palette dell'app deve sempre essere quella verde del
    // logo, non seguire Material You/il wallpaper dell'utente
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}