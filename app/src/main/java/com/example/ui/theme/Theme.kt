package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DarkEmeraldPrimary,
  onPrimary = Color(0xFF00382E),
  primaryContainer = DarkEmeraldPrimaryContainer,
  onPrimaryContainer = Color(0xFFCCFBF1),
  secondary = DarkEmeraldSecondary,
  onSecondary = Color(0xFF003730),
  secondaryContainer = Color(0xFF134E48),
  onSecondaryContainer = Color(0xFF99F6E4),
  tertiary = EmeraldTertiary,
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = Color.White,
  primaryContainer = EmeraldPrimaryContainer,
  onPrimaryContainer = EmeraldOnPrimaryContainer,
  secondary = EmeraldSecondary,
  onSecondary = Color.White,
  secondaryContainer = EmeraldSecondaryContainer,
  onSecondaryContainer = Color(0xFF115E59),
  tertiary = EmeraldTertiary,
  background = BackgroundLight,
  onBackground = TextPrimaryLight,
  surface = SurfaceLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent emerald branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

