package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
  primary = MlBlue,
  onPrimary = Color.White,
  primaryContainer = MlYellow,
  onPrimaryContainer = Color(0xFF1E2348),
  secondary = MlBlueLight,
  onSecondary = Color.White,
  tertiary = MlYellowDark,
  background = Color(0xFFF1F5F9),
  surface = Color.White,
  onBackground = Color(0xFF0F172A),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE2E8F0),
  onSurfaceVariant = Color(0xFF475569)
)

private val DarkColorScheme = darkColorScheme(
  primary = MlYellow,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF2D3277),
  onPrimaryContainer = Color.White,
  secondary = Color(0xFF60A5FA),
  onSecondary = Color.Black,
  background = Color(0xFF0F172A),
  surface = Color(0xFF1E293B),
  onBackground = Color.White,
  onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent branding
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
