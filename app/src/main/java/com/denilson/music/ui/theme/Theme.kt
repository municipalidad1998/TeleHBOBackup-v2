package com.denilson.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.denilson.music.data.datastore.ThemeMode

// ============================================================
// Identidad visual estilo HBO MAX: azul marino profundo con
// degradado "Max" (azul -> violeta -> magenta) y blanco puro.
// ============================================================

/** Fondo principal (navy característico de HBO Max) */
val MaxBackground = Color(0xFF001933)
/** Superficies elevadas */
val MaxSurface = Color(0xFF0A2440)
val MaxSurfaceVariant = Color(0xFF123252)
/** Degradado Max */
val MaxBlue = Color(0xFF2E5BFF)
val MaxViolet = Color(0xFF8B2BE2)
val MaxMagenta = Color(0xFFC335C6)
/** Acentos */
val MaxCyan = Color(0xFF35C5F5)
val MaxText = Color(0xFFFFFFFF)
val MaxTextMuted = Color(0xFF9DB2C9)
/** Marcas de productos */
val YouTubeRed = Color(0xFFFF0033)
val YtMusicRed = Color(0xFFFF2D55)
/** Verde de éxito para el bloqueador */
val BlockGreen = Color(0xFF2ED573)

/** Degradado insignia de la app (estilo "Max"). */
fun maxGradient() = Brush.linearGradient(listOf(MaxBlue, MaxViolet, MaxMagenta))

/** Degradado de YouTube. */
fun youtubeGradient() = Brush.linearGradient(listOf(Color(0xFFFF0033), Color(0xFFB3001C)))

/** Degradado de YouTube Music. */
fun ytMusicGradient() = Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFF8E0E36)))

/** Degradado suave para héroes y cabeceras. */
fun heroGradient() = Brush.verticalGradient(
    listOf(Color(0xFF0B3B8C), Color(0xFF1E1E63), Color(0xFF5A1E8C))
)

private val DarkScheme = darkColorScheme(
    primary = MaxBlue,
    onPrimary = Color.White,
    secondary = MaxMagenta,
    onSecondary = Color.White,
    tertiary = MaxCyan,
    onTertiary = Color(0xFF001A33),
    background = MaxBackground,
    onBackground = MaxText,
    surface = MaxSurface,
    onSurface = MaxText,
    surfaceVariant = MaxSurfaceVariant,
    onSurfaceVariant = MaxTextMuted,
    primaryContainer = Color(0xFF1A3E8C),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondaryContainer = Color(0xFF4B1E5E),
    onSecondaryContainer = Color(0xFFFFD9F6),
    outline = Color(0xFF2A4A6B)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1A3EDB),
    onPrimary = Color.White,
    secondary = Color(0xFF8B2BE2),
    onSecondary = Color.White,
    tertiary = Color(0xFF0077A8),
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF0A1B2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1B2E),
    surfaceVariant = Color(0xFFDCE6F2),
    onSurfaceVariant = Color(0xFF44607C),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0A1B5E)
)

@Composable
fun DenilsonTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}

